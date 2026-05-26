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
import org.jfrog.bamboo.context.XrayScanContext;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.client.artifactoryXrayResponse.Summary;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.util.Map;

/**
 * Deployment-project variant of {@link ArtifactoryXrayScanTask}.
 * <p>
 * The Xray scan only requires a build name, build number and an Artifactory server, so it works
 * naturally in deployment projects. This implementation extends {@link ArtifactoryDeploymentTaskType}
 * (which implements Bamboo's {@code DeploymentTaskType}) so the task is selectable from the
 * Deployment Projects task picker.
 */
public class ArtifactoryDeploymentXrayScanTask extends ArtifactoryDeploymentTaskType {

    public static final String TASK_NAME = "artifactoryDeploymentXrayScanTask";

    private CustomVariableContext customVariableContext;
    private ServerConfig xrayServerConfig;
    private XrayScanContext xrayContext;

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        xrayContext = new XrayScanContext(context.getConfigurationMap());
        xrayServerConfig = resolveXrayServerConfig(context);
    }

    @NotNull
    @Override
    protected TaskResult runTask(@NotNull DeploymentTaskContext deploymentTaskContext) {
        String buildName = xrayContext.getBuildName();
        String buildNumber = xrayContext.getBuildNumber();
        if (StringUtils.isBlank(buildName) || StringUtils.isBlank(buildNumber)) {
            buildInfoLog.error("Build name and build number must be configured for the deployment Xray Scan task.");
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }

        try (ArtifactoryManager client = TaskUtils.getArtifactoryManagerBuilderBuilder(xrayServerConfig, buildInfoLog).build()) {
            ArtifactoryXrayResponse buildScanResult = client.scanBuild(buildName, buildNumber, "", "bamboo");
            Summary summary = buildScanResult.getSummary();
            if (summary == null) {
                throw new IllegalStateException("Failed while processing the JSON result: 'summary' field is missing.");
            }
            if (StringUtils.isNotEmpty(summary.getMoreDetailsUrl())) {
                logger.addBuildLogEntry("Xray scan details are available at: " + summary.getMoreDetailsUrl());
            }
            String scanMessage = summary.getMessage();
            if (summary.isFailBuild() && xrayContext.isFailIfVulnerable()) {
                buildInfoLog.error(scanMessage);
                return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
            }
            buildInfoLog.info(scanMessage);
        } catch (Exception e) {
            buildInfoLog.error("Exception occurred while executing task", e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
        return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
    }

    private ServerConfig resolveXrayServerConfig(@NotNull CommonTaskContext context) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(xrayContext.getArtifactoryServerId());
        if (selectedServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        Map<String, String> runtimeContext = context.getRuntimeTaskContext();
        return TaskUtils.getResolutionServerConfig(
                xrayContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                xrayContext.getOverriddenPassword(runtimeContext, buildInfoLog, true),
                serverConfigManager, selectedServerConfig, new BuildParamsOverrideManager(customVariableContext));
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return xrayServerConfig;
    }

    @Override
    protected String getTaskUsageName() {
        return "deployment_xray_scan";
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
