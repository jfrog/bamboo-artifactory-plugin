package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.generic.GenericArtifactsResolver;
import org.jfrog.bamboo.util.generic.GenericData;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.IOException;
import java.util.List;

/**
 * @author Lior Hasson
 */
public class ArtifactoryGenericResolveTask implements TaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryGenericResolveTask.class);

    private BuildLogger logger;
    private BuildParamsOverrideManager buildParamsOverrideManager;


    public ArtifactoryGenericResolveTask(CustomVariableContext customVariableContext) {
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        logger = taskContext.getBuildLogger();

        List<? extends TaskDefinition> taskDefinitions = taskContext.getBuildContext().getRuntimeTaskDefinitions();
        /**
         *In case generic deploy exists in the user job, and the publish build info flag is on, we need to
         * capture all the resolution data for the build info, and prepare it to the deploy task.
         */
        boolean buildinfoFlag = false;
        TaskDefinition genericDeployDefinition = TaskDefinitionHelper.findGenericDeployDefinition(taskDefinitions);

        if (genericDeployDefinition != null)
            buildinfoFlag = Boolean.valueOf(genericDeployDefinition.getConfiguration().get("artifactory.generic.publishBuildInfo"));

        GenericContext genericContext = new GenericContext(taskContext.getConfigurationMap());

        ArtifactoryDependenciesClient client = getArtifactoryDependenciesClient(genericContext);

        try {
            org.jfrog.build.api.util.Log bambooBuildInfoLog = new BuildInfoLog(log, logger);
            List<BuildDependency> buildDependencies;
            List<Dependency> dependencies;

            if (genericContext.isUseFileSpecs()) {
                buildDependencies = Lists.newArrayList();
                String spec = genericContext.isFileSpecInJobConfiguration() ? genericContext.getJobConfigurationSpec() : TaskUtils.getSpecFromFile(taskContext.getWorkingDirectory(), genericContext.getFilePathSpec());
                if (StringUtils.isNotBlank(spec)) {
                    SpecsHelper specsHelper = new SpecsHelper(bambooBuildInfoLog);
                    dependencies = specsHelper.downloadArtifactsBySpec(spec, client, taskContext.getWorkingDirectory().getCanonicalPath());
                } else {
                    dependencies = Lists.newArrayList();
                }
            } else {
                GenericArtifactsResolver resolver = new GenericArtifactsResolver(taskContext, client,
                        genericContext.getResolvePattern(), bambooBuildInfoLog);
                buildDependencies = resolver.retrieveBuildDependencies();
                dependencies = resolver.retrievePublishedDependencies();
            }
            if (buildinfoFlag) {
                /**
                 * Add dependencies for the Generic deploy task, if exists!
                 * */
                addDependenciesToContext(taskContext, buildDependencies, dependencies);
            }

        } catch (IOException|InterruptedException e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        } finally {
            client.close();
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private void addDependenciesToContext(TaskContext taskContext, List<BuildDependency> buildDependencies, List<Dependency> dependencies) throws IOException {
        GenericData gd = new GenericData();
        gd.setBuildDependencies(buildDependencies);
        gd.setDependencies(dependencies);
        String json = BuildInfoExtractorUtils.buildInfoToJsonString(gd);

        taskContext.getBuildContext().getParentBuildContext().getBuildResult().
                getCustomBuildData().put("genericJson", json);
    }

    private ArtifactoryDependenciesClient getArtifactoryDependenciesClient(GenericContext genericContext) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(genericContext.getSelectedServerId());
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactpry server. Please check the Artifactory server in the task configuration.");
        }
        String username = overrideParam(serverConfigManager.substituteVariables(genericContext.getUsername()), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        String password = overrideParam(serverConfigManager.substituteVariables(genericContext.getPassword()), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());
        return new ArtifactoryDependenciesClient(serverUrl, username, password, new BuildInfoLog(log));
    }

    public String overrideParam(String originalValue, String overrideKey) {
        String overriddenValue = buildParamsOverrideManager.getOverrideValue(overrideKey);
        return overriddenValue.isEmpty() ? originalValue : overriddenValue;
    }
}
