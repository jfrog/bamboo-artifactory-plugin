package org.jfrog.bamboo.task;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.ArtifactoryDeploymentUploadConfiguration;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.DeploymentUploadContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.deployment.LegacyDeploymentUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Bamboo deployment Artifactory task - Takes pre defined artifacts from a build plan and deploys them to Artifactory
 *
 * @author Aviad Shikloshi
 */
public class ArtifactoryDeploymentUploadTask extends ArtifactoryDeploymentTaskType {
    private CustomVariableContext customVariableContext;
    private ServerConfig uploadServerConfig;
    private String fileSpec;

    @Override
    protected void initTask(@NotNull DeploymentTaskContext context) {
        super.initTask(context);
        Map<String, String> runtimeContext = context.getRuntimeTaskContext();
        DeploymentUploadContext deploymentUploadContext = new DeploymentUploadContext(context.getConfigurationMap());
        ServerConfig selectedServerConfig = getSelectedServerConfig(context, deploymentUploadContext);
        uploadServerConfig = TaskUtils.getResolutionServerConfig(
                deploymentUploadContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                deploymentUploadContext.getOverriddenPassword(runtimeContext, buildInfoLog, true),
                ServerConfigManager.getInstance(), selectedServerConfig, new BuildParamsOverrideManager(customVariableContext));
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull DeploymentTaskContext deploymentTaskContext) {
        ArtifactoryBuildInfoClientBuilder clientBuilder = TaskUtils.getArtifactoryBuildInfoClientBuilder(uploadServerConfig, buildInfoLog);
        String artifactsRootDirectory = deploymentTaskContext.getRootDirectory().getAbsolutePath();
        try {
            initFileSpec(deploymentTaskContext);
            SpecsHelper specsHelper = new SpecsHelper(buildInfoLog);
            specsHelper.uploadArtifactsBySpec(fileSpec, new File(artifactsRootDirectory), new HashMap<>(), clientBuilder);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
        } catch (Exception e) {
            buildInfoLog.error("Exception occurred while executing deployment task", e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return uploadServerConfig;
    }

    @Override
    protected String getTaskUsageName() {
        return "deployment_upload";
    }

    @Override
    protected Log getLog() {
        return new BuildInfoLog(log, buildLogger);
    }

    private void initFileSpec(CommonTaskContext context) throws IOException {
        String specSourceChoice = context.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX
                + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_CHOICE);
        if (StringUtils.isNotBlank(specSourceChoice)) {
            fileSpec = FileSpecUtils.getFileSpec(isFileSpecInJobConfiguration(context),
                    getJobConfigurationSpec(context), getFilePathSpec(context), context.getWorkingDirectory(),
                    customVariableContext, buildLogger);
            buildLogger.addBuildLogEntry("Spec: " + fileSpec);
            FileSpecUtils.validateFileSpec(fileSpec);
            return;
        }
        buildLogger.addBuildLogEntry("Converting legacy configuration to upload spec");
        fileSpec = LegacyDeploymentUtils.buildDeploymentSpec(context);
        buildLogger.addBuildLogEntry("Spec: " + fileSpec);
        FileSpecUtils.validateFileSpec(fileSpec);
    }

    /**
     * Get configurations of the selected server in the task definition.
     */
    private ServerConfig getSelectedServerConfig(@NotNull DeploymentTaskContext deploymentTaskContext, DeploymentUploadContext context) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        long serverId = context.getArtifactoryServerId();
        if (serverId == -1) {
            // Compatibility with version 1.8.0
            serverId = Long.parseLong(deploymentTaskContext.getConfigurationMap().get("artifactoryServerId"));
        }
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        return serverConfig;
    }

    private String getJobConfigurationSpec(CommonTaskContext context) {
        return context.getConfigurationMap()
                .get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_JOB_CONFIGURATION);
    }

    private String getFilePathSpec(CommonTaskContext context) {
        return context.getConfigurationMap()
                .get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_FILE);
    }

    private Boolean isFileSpecInJobConfiguration(CommonTaskContext context) {
        return ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_JOB_CONFIGURATION.equals(
                context.getConfigurationMap()
                        .get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_CHOICE));
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}