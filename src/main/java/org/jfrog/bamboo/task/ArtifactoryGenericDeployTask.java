package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.generic.GenericBuildInfoHelper;
import org.jfrog.bamboo.util.generic.GenericData;
import org.jfrog.bamboo.util.version.VcsHelper;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;
import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM;

/**
 * @author Tomer Cohen
 */
public class ArtifactoryGenericDeployTask implements TaskType {
    public static final String TASK_NAME = "artifactoryGenericTask";
    private static final Logger log = Logger.getLogger(ArtifactoryGenericDeployTask.class);
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private PluginAccessor pluginAccessor;
    private BuildLogger logger;
    private GenericBuildInfoHelper buildInfoHelper;
    private CustomVariableContext customVariableContext;
    private BuildParamsOverrideManager buildParamsOverrideManager;

    public ArtifactoryGenericDeployTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @Override
    @NotNull
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        logger = taskContext.getBuildLogger();
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + getArtifactoryVersion());
        if (!taskContext.isFinalising()) {
            log.error(logger.addErrorLogEntry("Artifactory Generic Deploy Task must run as a final Task!"));
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }
        BuildContext context = taskContext.getBuildContext();
        CurrentBuildResult result = context.getBuildResult();
        // if build wasn't a success don't do anything
        if (result.getBuildState().equals(BuildState.FAILED)) {
            log.error(logger.addErrorLogEntry("Build failed, not deploying to Artifactory."));
            return TaskResultBuilder.newBuilder(taskContext).success().build();
        }
        GenericContext genericContext = new GenericContext(taskContext.getConfigurationMap());
        Map<String, String> env = Maps.newHashMap();
        env.putAll(environmentVariableAccessor.getEnvironment(taskContext));
        env.putAll(environmentVariableAccessor.getEnvironment());
        String vcsRevision = VcsHelper.getRevisionKey(context);
        if (StringUtils.isBlank(vcsRevision)) {
            vcsRevision = "";
        }

        String vcsUrl = VcsHelper.getVcsUrl(context);
        if (StringUtils.isBlank(vcsUrl)) {
            vcsUrl = "";
        }

        buildInfoHelper = new GenericBuildInfoHelper(env, vcsRevision, vcsUrl);
        buildInfoHelper.init(buildParamsOverrideManager, context);
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(genericContext.getSelectedServerId());
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactpry server. Please check the Artifactory server in the task configuration.");
        }
        String username = getUsername(genericContext, serverConfigManager, serverConfig);
        Build build = getBuild(genericContext, taskContext, username);
        ArtifactoryBuildInfoClient client = getClient(genericContext, taskContext, username, serverConfigManager, serverConfig);
        try {
            File sourceCodeDirectory = getWorkingDirectory(context, taskContext);
            if (sourceCodeDirectory == null) {
                log.error(logger.addErrorLogEntry("No build directory found!"));
                return TaskResultBuilder.newBuilder(taskContext).success().build();
            }
            if (genericContext.isUseFileSpecs()) {
                String spec = getFileSpecSource(genericContext, sourceCodeDirectory);
                deployByFileSpec(sourceCodeDirectory, taskContext, build, client, spec);
            } else {
                deployByLegacyPattern(sourceCodeDirectory, taskContext, build, client, genericContext);
            }
            if (genericContext.isPublishBuildInfo()) {
                publishBuildInfo(taskContext, client, build);
            }
        } catch (Exception e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        } finally {
            client.close();
        }
        Map<String, String> customBuildData = result.getCustomBuildData();
        if (genericContext.isPublishBuildInfo() && !customBuildData.containsKey(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) {
            customBuildData.put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private String getFileSpecSource(GenericContext genericContext, File sourceCodeDirectory) throws IOException {
        return genericContext.isFileSpecInJobConfiguration() ? genericContext.getJobConfigurationSpec() : TaskUtils.getSpecFromFile(sourceCodeDirectory, genericContext.getFilePathSpec());
    }

    private File getWorkingDirectory(BuildContext context, TaskContext taskContext) throws RepositoryException {
        File checkoutDir = VcsHelper.getCheckoutDirectory(context);
        if (checkoutDir != null) {
            return checkoutDir;
        } else {
            return taskContext.getWorkingDirectory();
        }
    }

    private Multimap<String, File> buildTargetPathToFiles(File directory, GenericContext context)
            throws IOException {
        Multimap<String, File> result = HashMultimap.create();
        String deployPattern = context.getDeployPattern();
        deployPattern = StringUtils.replace(deployPattern, "\r\n", "\n");
        deployPattern = StringUtils.replace(deployPattern, ",", "\n");

        // Collect all published items pair in the form of 'pattern -> targetPath'
        Multimap<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(deployPattern);
        if (pairs.isEmpty()) {
            return result;
        }

        // Collect all found items and put them into the result in the form of 'targetPath -> File'
        for (Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> filesMap = PublishedItemsHelper.buildPublishingData(directory, entry.getKey(),
                    entry.getValue());
            if (filesMap != null) {
                log.info(logger.addBuildLogEntry(
                        "For pattern: " + entry.getKey() + " " + filesMap.size() + " artifacts were found"));
                result.putAll(filesMap);
            } else {
                log.warn(logger.addBuildLogEntry("For pattern: " + entry.getKey() + " no artifacts were found"));
            }
        }

        return result;
    }

    private void deployByLegacyPattern(File sourceCodeDirectory, TaskContext taskContext, Build build, ArtifactoryBuildInfoClient client, GenericContext context) throws IOException, NoSuchAlgorithmException {
        Multimap<String, File> filesMap = buildTargetPathToFiles(sourceCodeDirectory, context);
        Set<DeployDetails> details = buildInfoHelper.createDeployDetailsAndAddToBuildInfo(build, filesMap, taskContext.getBuildContext(), context);
        for (DeployDetails detail : details) {
            client.deployArtifact(detail);
        }
    }

    private void deployByFileSpec(File sourceCodeDirectory, TaskContext taskContext, Build build, ArtifactoryBuildInfoClient client, String spec) throws IOException, NoSuchAlgorithmException {
        SpecsHelper specsHelper = new SpecsHelper(new BuildInfoLog(ArtifactoryGenericDeployTask.log, taskContext.getBuildLogger()));
        Map<String, String> buildProperties = buildInfoHelper.getDynamicPropertyMap(build);
        buildInfoHelper.addCommonProperties(buildProperties);
        List<Artifact> artifacts = specsHelper.uploadArtifactsBySpec(spec, sourceCodeDirectory, buildProperties, client);

        ModuleBuilder moduleBuilder =
                new ModuleBuilder().id(taskContext.getBuildContext().getPlanName() + ":" + taskContext.getBuildContext().getBuildNumber())
                        .artifacts(artifacts);
        build.setModules(Lists.newArrayList(moduleBuilder.build()));
    }

    public void publishBuildInfo(TaskContext taskContext, ArtifactoryBuildInfoClient client, Build build) throws IOException {
        BuildContext buildContext = taskContext.getBuildContext();
        /**
         * Look for dependencies from the Generic resolve task, if exists!
         * */
        getDependenciesFromContext(taskContext, build);
        client.sendBuildInfo(build);
        buildContext.getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM, client.getArtifactoryUrl());
    }

    private String getUsername(GenericContext context, ServerConfigManager serverConfigManager, ServerConfig serverConfig) {
        String username = buildInfoHelper.overrideParam(serverConfigManager.substituteVariables(context.getUsername()),
                BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        return username;
    }

    private String getPassword(GenericContext context, ServerConfigManager serverConfigManager, ServerConfig serverConfig) {
        String password = buildInfoHelper.overrideParam(serverConfigManager.substituteVariables(context.getPassword()),
                BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        return password;
    }

    private ArtifactoryBuildInfoClient getClient(GenericContext context, TaskContext taskContext, String username, ServerConfigManager serverConfigManager, ServerConfig serverConfig) {
        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());
        org.jfrog.build.api.util.Log bambooBuildInfoLog = new BuildInfoLog(ArtifactoryGenericDeployTask.log, taskContext.getBuildLogger());
        return new ArtifactoryBuildInfoClient(serverUrl, username, getPassword(context, serverConfigManager, serverConfig), bambooBuildInfoLog);
    }

    private Build getBuild(GenericContext context, TaskContext taskContext, String username) {
        BuildContext buildContext = taskContext.getBuildContext();
        return buildInfoHelper.extractBuildInfo(buildContext, taskContext.getBuildLogger(), context, username);
    }

    private void getDependenciesFromContext(TaskContext taskContext, Build build) throws IOException {
        String genericJson = taskContext.getBuildContext().getParentBuildContext().getBuildResult().
                getCustomBuildData().get("genericJson");

        if (StringUtils.isNotBlank(genericJson)) {
            GenericData genericData = BuildInfoExtractorUtils.jsonStringToGeneric(genericJson, GenericData.class);
            //Assumption: There is only one module for Generic
            build.getModules().get(0).setDependencies(genericData.getDependencies());
            build.setBuildDependencies(genericData.getBuildDependencies());
        }
    }

    public String getArtifactoryVersion() {
        Plugin plugin = pluginAccessor.getPlugin(ConstantValues.ARTIFACTORY_PLUGIN_KEY);
        if (plugin != null) {
            return plugin.getPluginInformation().getVersion();
        }
        return StringUtils.EMPTY;
    }
}
