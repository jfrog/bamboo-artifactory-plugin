package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.generic.GenericArtifactsResolver;
import org.jfrog.bamboo.util.generic.GenericData;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

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

        List<TaskDefinition> taskDefinitions = taskContext.getBuildContext().getTaskDefinitions();
        /**
         *In case generic deploy exists in the user job, and the publish build info flag is on, we need to
         * capture all the resolution data for the build info, and prepare it to the deploy task.
         */
        String buildinfoFlag = "false";
        TaskDefinition genericDeployDefinition = TaskDefinitionHelper.findGenericDeployDefinition(taskDefinitions);

        if (genericDeployDefinition != null)
            buildinfoFlag = genericDeployDefinition.getConfiguration().get("artifactory.generic.publishBuildInfo");

        GenericContext genericContext = new GenericContext(taskContext.getConfigurationMap());

        ArtifactoryDependenciesClient client = getArtifactoryDependenciesClient(genericContext);

        try {
            GenericArtifactsResolver resolver = new GenericArtifactsResolver(taskContext, client,
                    genericContext.getResolvePattern(), new BambooBuildInfoLog(log, logger));

            List<BuildDependency> buildDependencies = resolver.retrieveBuildDependencies();
            List<Dependency> dependencies = resolver.retrievePublishedDependencies();
            logger.addBuildLogEntry("Resolved " + dependencies.size() + " artifacts.");

            if (buildinfoFlag.equals("true")) {
                /**
                 * Add dependencies for the Generic deploy task, if exists!
                 * */
                addDependenciesToContext(taskContext, buildDependencies, dependencies);
            }

        } catch (IOException e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        } catch (InterruptedException ie) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, ie);
            log.error(message, ie);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
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
        String username = overrideParam(serverConfigManager.substituteVariables(genericContext.getUsername()), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        String password = overrideParam(serverConfigManager.substituteVariables(genericContext.getPassword()), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());
        return new ArtifactoryDependenciesClient(serverUrl, username, password, new BambooBuildInfoLog(log));
    }

    public String overrideParam(String originalValue, String overrideKey) {
        String overriddenValue = buildParamsOverrideManager.getOverrideValue(overrideKey);
        return overriddenValue.isEmpty() ? originalValue : overriddenValue;
    }
}
