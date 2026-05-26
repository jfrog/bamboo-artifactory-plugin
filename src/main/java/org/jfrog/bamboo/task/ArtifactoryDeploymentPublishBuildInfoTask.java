package org.jfrog.bamboo.task;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.PublishBuildInfoContext;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.util.Date;
import java.util.Map;

/**
 * Deployment-project variant of {@link ArtifactoryPublishBuildInfoTask}.
 * <p>
 * A Bamboo Deployment Project does not provide a {@link com.atlassian.bamboo.v2.build.BuildContext}, so
 * this task reads the build name / number directly from its configuration and publishes a minimal
 * BuildInfo (without the in-plan build-info aggregation that the build-time task performs).
 *
 * <p>This task addresses customer requests for the JFrog plugin to be selectable from the Deployment
 * Projects task picker, where previously only "Artifactory Download" / "Artifactory Deployment" were
 * available.
 */
public class ArtifactoryDeploymentPublishBuildInfoTask extends ArtifactoryDeploymentTaskType {

    public static final String TASK_NAME = "artifactoryDeploymentPublishBuildInfoTask";

    private CustomVariableContext customVariableContext;
    private ServerConfig publishServerConfig;
    private PublishBuildInfoContext publishBuildInfoContext;

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        publishBuildInfoContext = new PublishBuildInfoContext(context.getConfigurationMap());
        publishServerConfig = resolvePublishServerConfig(context);
    }

    @NotNull
    @Override
    protected TaskResult runTask(@NotNull DeploymentTaskContext deploymentTaskContext) {
        String buildName = publishBuildInfoContext.getBuildName();
        String buildNumber = publishBuildInfoContext.getBuildNumber();
        if (StringUtils.isBlank(buildName) || StringUtils.isBlank(buildNumber)) {
            String message = "Build name and build number must be configured for the deployment Publish Build Info task.";
            buildInfoLog.error(message);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }

        ArtifactoryManagerBuilder clientBuilder = TaskUtils.getArtifactoryManagerBuilderBuilder(publishServerConfig, buildInfoLog);
        try (ArtifactoryManager client = clientBuilder.build()) {
            BuildInfo build = new BuildInfoBuilder(buildName)
                    .number(buildNumber)
                    .startedDate(new Date())
                    .build();
            client.publishBuildInfo(build, "");
            buildInfoLog.info("Published build-info for '" + buildName + "/" + buildNumber + "' to " + client.getUrl());
        } catch (Exception e) {
            buildInfoLog.error("Exception occurred while publishing build-info: " + e.getMessage(), e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
        return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
    }

    private ServerConfig resolvePublishServerConfig(@NotNull CommonTaskContext context) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(publishBuildInfoContext.getArtifactoryServerId());
        if (selectedServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        Map<String, String> runtimeContext = context.getRuntimeTaskContext();
        return TaskUtils.getDeploymentServerConfig(
                publishBuildInfoContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                publishBuildInfoContext.getOverriddenPassword(runtimeContext, buildInfoLog, true),
                serverConfigManager, selectedServerConfig, new BuildParamsOverrideManager(customVariableContext));
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return publishServerConfig;
    }

    @Override
    protected String getTaskUsageName() {
        return "deployment_publish_build_info";
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
