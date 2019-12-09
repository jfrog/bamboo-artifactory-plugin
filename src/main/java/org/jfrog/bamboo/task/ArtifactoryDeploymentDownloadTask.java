package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.IOException;

/**
 * Bamboo Download from Artifactory in deployment task - Download files from Artifactory in order to use them in deployment task.
 *
 * @author Yahav Itzhak
 */
public class ArtifactoryDeploymentDownloadTask extends ArtifactoryDeploymentTaskType {
    private static final Logger log = Logger.getLogger(ArtifactoryDeploymentDownloadTask.class);
    private CustomVariableContext customVariableContext;
    private String fileSpec;
    private BuildLogger logger;
    private GenericContext genericContext;
    private ServerConfig downloadServerConfig;

    @Override
    protected void initTask(@NotNull DeploymentTaskContext context) {
        logger = context.getBuildLogger();
        genericContext = new GenericContext(context.getConfigurationMap());
        BuildParamsOverrideManager buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        downloadServerConfig = getArtifactoryServerConfig(buildParamsOverrideManager);
    }

    @NotNull
    public TaskResult runTask(@NotNull DeploymentTaskContext deploymentTaskContext) throws TaskException {
        ArtifactoryDependenciesClient client = TaskUtils.getArtifactoryDependenciesClient(downloadServerConfig, log);
        try {
            initFileSpec(deploymentTaskContext, genericContext, logger);
            SpecsHelper specsHelper = new SpecsHelper(new BuildInfoLog(log, logger));
            specsHelper.downloadArtifactsBySpec(fileSpec, client, deploymentTaskContext.getWorkingDirectory().getCanonicalPath());
        } catch (IOException e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        } finally {
            client.close();
        }
        return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
    }

    private ServerConfig getArtifactoryServerConfig(BuildParamsOverrideManager buildParamsOverrideManager) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(genericContext.getSelectedServerId());
        if (selectedServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        // Get overridden server configurations for download.
        return TaskUtils.getResolutionServerConfig(genericContext.getUsername(), genericContext.getPassword(),
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

    @Override
    protected Log getLog() {
        return new BuildInfoLog(log, logger);
    }
}
