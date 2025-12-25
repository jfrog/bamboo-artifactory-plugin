package org.jfrog.bamboo.task;

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.build.api.multiMap.ListMultimap;
import org.jfrog.build.api.multiMap.Multimap;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.ci.Artifact;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;
import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM;

/**
 * @author Tomer Cohen
 */
public class ArtifactoryGenericDeployTask extends ArtifactoryTaskType {
    public static final String TASK_NAME = "artifactoryGenericTask";
    @Inject
    @ComponentImport
    private EnvironmentVariableAccessor environmentVariableAccessor;
    private BuildParamsOverrideManager buildParamsOverrideManager;
    @Inject
    @ComponentImport
    private CustomVariableContext customVariableContext;
    private BuildInfoHelper buildInfoHelper;
    private GenericContext genericContext;
    private BuildContext buildContext;
    private String fileSpec;
    @Inject
    private ServerConfigManager serverConfigManager;

    public void setServerConfigManager(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
    }

    public void setEnvironmentVariableAccessor(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
    }

    @Override
    public void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        buildContext = ((TaskContext) taskContext).getBuildContext();
        genericContext = new GenericContext(taskContext.getConfigurationMap());
        Map<String, String> runtimeContext = context.getRuntimeTaskContext();
        buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(genericContext.getBuildName(buildContext),
                genericContext.getBuildNumber(buildContext), taskContext, buildContext, environmentVariableAccessor,
                genericContext.getSelectedServerId(),
                genericContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                genericContext.getOverriddenPassword(runtimeContext, buildInfoLog, true),
                buildParamsOverrideManager,
                serverConfigManager);
    }

    @Override
    @NotNull
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + Utils.getPluginVersion(pluginAccessor));

        // Check if we should stop task.
        CurrentBuildResult result = buildContext.getBuildResult();
        // If build wasn't a success don't do anything.
        if (result.getBuildState().equals(BuildState.FAILED)) {
            log.error(logger.addErrorLogEntry("Build failed, not deploying to Artifactory."));
            return TaskResultBuilder.newBuilder(taskContext).success().build();
        }

        BuildInfo build = buildInfoHelper.getBuild(taskContext, genericContext);
        ArtifactoryManagerBuilder artifactoryManagerBuilder = buildInfoHelper.getClientBuilder(logger, log);
        try {
            File workingDirectory = getWorkingDirectory();
            if (workingDirectory == null) {
                log.error(logger.addErrorLogEntry("No build directory found!"));
                return TaskResultBuilder.newBuilder(taskContext).success().build();
            }
            if (genericContext.isUseFileSpecs()) {
                initFileSpec();
                build = deployByFileSpec(workingDirectory, build, artifactoryManagerBuilder, fileSpec);
            } else {
                build = deployByLegacyPattern(workingDirectory, build, artifactoryManagerBuilder, genericContext);
            }
            List<? extends TaskDefinition> taskDefinitions = taskContext.getBuildContext().getRuntimeTaskDefinitions();
            if (genericContext.isCaptureBuildInfo() || (genericContext.isPublishBuildInfo() && TaskDefinitionHelper.isBuildPublishTaskExists(taskDefinitions))) {
                taskBuildInfo = build;
            } else {
                if (genericContext.isPublishBuildInfo()) {
                    publishBuildInfo(taskContext, artifactoryManagerBuilder, build);
                }
            }
        } catch (Exception e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        Map<String, String> customBuildData = result.getCustomBuildData();
        if (genericContext.isPublishBuildInfo() && !customBuildData.containsKey(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) {
            customBuildData.put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    @Override
    protected String getTaskUsageName() {
        return "generic_deploy";
    }

    private void initFileSpec() throws IOException {
        fileSpec = FileSpecUtils.getFileSpec(genericContext.isFileSpecInJobConfiguration(),
                genericContext.getJobConfigurationSpec(), genericContext.getFilePathSpec(), getWorkingDirectory(),
                customVariableContext, logger);
        logger.addBuildLogEntry("Spec: " + fileSpec);
        FileSpecUtils.validateFileSpec(fileSpec);
    }

    /**
     * Gets the VCS checkout directory of the build.
     * If the build has more than 1 checkout-location, return the default working directory.
     * We keep using the VCS checkout directory for backward compatibility.
     *
     * @return the working directory for the task.
     */
    private File getWorkingDirectory() {
        Map<Long, String> checkoutLocations = buildContext.getCheckoutLocation();
        if (checkoutLocations.size() == 1) {
            Iterator<String> checkoutLocationsIterator = checkoutLocations.values().iterator();
            String checkoutLocation = checkoutLocationsIterator.next();
            if (StringUtils.isNotBlank(checkoutLocation)) {
                return new File(checkoutLocation);
            }
        }
        return taskContext.getWorkingDirectory();
    }

    private Multimap<String, File> buildTargetPathToFiles(File directory, GenericContext context)
            throws IOException {
        Multimap<String, File> result = new ListMultimap<>();
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

    private BuildInfo deployByLegacyPattern(File sourceCodeDirectory, BuildInfo build, ArtifactoryManagerBuilder artifactoryManagerBuilder, GenericContext context) throws IOException, NoSuchAlgorithmException {
        Multimap<String, File> filesMap = buildTargetPathToFiles(sourceCodeDirectory, context);
        Set<DeployDetails> details = Sets.newHashSet();
        Map<String, String> dynamicPropertyMap = buildInfoHelper.getDynamicPropertyMap(build);
        String repoKey = buildInfoHelper.overrideParam(context.getRepoKey(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOY_REPO);
        for (Map.Entry<String, File> entry : filesMap.entries()) {
            details.addAll(buildDeployDetailsFromFileSet(entry, repoKey, dynamicPropertyMap));
        }
        List<Artifact> artifacts = buildInfoHelper.convertDeployDetailsToArtifacts(details);

        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            for (DeployDetails detail : details) {
                artifactoryManager.upload(detail);
            }
        }

        return buildInfoHelper.addBuildInfoParams(build, artifacts, Lists.newArrayList(), Lists.newArrayList());
    }

    private Set<DeployDetails> buildDeployDetailsFromFileSet(Map.Entry<String, File> fileEntry, String targetRepository,
                                                             Map<String, String> propertyMap) throws IOException,
            NoSuchAlgorithmException {
        Set<DeployDetails> result = Sets.newHashSet();
        String targetPath = fileEntry.getKey();
        File artifactFile = fileEntry.getValue();
        String path = PublishedItemsHelper.calculateTargetPath(targetPath, artifactFile);
        path = StringUtils.replace(path, "//", "/");

        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "SHA1", "MD5");
        DeployDetails.Builder deployDetails = new DeployDetails.Builder().file(artifactFile).md5(checksums.get("MD5"))
                .sha1(checksums.get("SHA1")).targetRepository(targetRepository).artifactPath(path);
        deployDetails.addProperties(propertyMap);
        result.add(deployDetails.build());
        return result;
    }

    private BuildInfo deployByFileSpec(File sourceCodeDirectory, BuildInfo build, ArtifactoryManagerBuilder artifactoryManagerBuilder, String spec) throws IOException {
        SpecsHelper specsHelper = new SpecsHelper(buildInfoLog);
        Map<String, String> buildProperties = buildInfoHelper.getDynamicPropertyMap(build);
        buildInfoHelper.addCommonProperties(buildProperties);
        List<org.jfrog.build.extractor.ci.Artifact> artifacts;
        try {
            artifacts = specsHelper.uploadArtifactsBySpec(spec, sourceCodeDirectory, buildProperties, artifactoryManagerBuilder);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return buildInfoHelper.addBuildInfoParams(build, artifacts, Lists.newArrayList(), Lists.newArrayList());
    }

    public void publishBuildInfo(TaskContext taskContext, ArtifactoryManagerBuilder artifactoryManagerBuilder, BuildInfo build) throws IOException {
        BuildContext buildContext = taskContext.getBuildContext();
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            artifactoryManager.publishBuildInfo(build, "");
            buildContext.getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM, artifactoryManager.getUrl());
        }
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
