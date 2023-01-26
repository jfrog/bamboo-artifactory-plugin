package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.IOException;
import java.util.Map;

/**
 * Bamboo Download from Artifactory in deployment task - Download files from Artifactory in order to use them in deployment task.
 *
 * @author Yahav Itzhak
 */
public class ArtifactoryDeploymentDownloadTask extends ArtifactoryDeploymentTaskType {
    private CustomVariableContext customVariableContext;
    private ServerConfig downloadServerConfig;
    private GenericContext genericContext;
    private String fileSpec;

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        genericContext = new GenericContext(context.getConfigurationMap());
        BuildParamsOverrideManager buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        downloadServerConfig = getArtifactoryServerConfig(buildParamsOverrideManager);
    }

    @NotNull
    public TaskResult runTask(@NotNull DeploymentTaskContext deploymentTaskContext) {
        try (ArtifactoryManager client = TaskUtils.getArtifactoryManagerBuilderBuilder(downloadServerConfig, new BuildInfoLog(log, logger)).build()) {
            initFileSpec(deploymentTaskContext, genericContext, logger);
            SpecsHelper specsHelper = new SpecsHelper(new BuildInfoLog(log, logger));
            specsHelper.downloadArtifactsBySpec(fileSpec, client, deploymentTaskContext.getWorkingDirectory().getCanonicalPath());
        } catch (IOException e) {
            buildInfoLog.error("Exception occurred while executing task", e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
        return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
    }

    private ServerConfig getArtifactoryServerConfig(BuildParamsOverrideManager buildParamsOverrideManager) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(genericContext.getSelectedServerId());
        if (selectedServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        Map<String, String> runtimeContext = taskContext.getRuntimeTaskContext();
        // Get overridden server configurations for download.
        return TaskUtils.getResolutionServerConfig(
                genericContext.getOverriddenUsername(runtimeContext, buildInfoLog, false),
                genericContext.getOverriddenPassword(runtimeContext, buildInfoLog, false),
                serverConfigManager, selectedServerConfig, buildParamsOverrideManager);
    }

    private void initFileSpec(CommonTaskContext context, GenericContext taskContext, BuildLogger logger) throws IOException {
        fileSpec = FileSpecUtils.getFileSpec(taskContext.isFileSpecInJobConfiguration(),
                taskContext.getJobConfigurationSpec(), taskContext.getFilePathSpec(), context.getWorkingDirectory(),
                customVariableContext, logger);
        logger.addBuildLogEntry("Spec: " + fileSpec);
        FileSpecUtils.validateFileSpec(fileSpec);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return downloadServerConfig;
    }

    @Override
    protected String getTaskUsageName() {
        return "deployment_download";
    }
}
