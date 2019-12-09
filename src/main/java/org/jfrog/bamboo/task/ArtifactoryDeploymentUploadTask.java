package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.ArtifactoryDeploymentUploadConfiguration;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.deployment.LegacyDeploymentUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Bamboo deployment Artifactory task - Takes pre defined artifacts from a build plan and deploys them to Artifactory
 *
 * @author Aviad Shikloshi
 */
public class ArtifactoryDeploymentUploadTask extends ArtifactoryDeploymentTaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryDeploymentUploadTask.class);
    private BuildLogger buildLogger;
    private String fileSpec;
    private CustomVariableContext customVariableContext;
    private ServerConfig uploadServerConfig;

    @Override
    protected void initTask(@NotNull DeploymentTaskContext context) {
        buildLogger = context.getBuildLogger();
        uploadServerConfig = getUploadServerConfig(context);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull DeploymentTaskContext deploymentTaskContext) throws TaskException {
        Log bambooBuildInfoLog = new BuildInfoLog(log, buildLogger);

        ArtifactoryBuildInfoClientBuilder clientBuilder = new ArtifactoryBuildInfoClientBuilder();
        clientBuilder.setArtifactoryUrl(uploadServerConfig.getUrl()).setUsername(uploadServerConfig.getUsername())
                .setPassword(uploadServerConfig.getPassword()).setLog(bambooBuildInfoLog);
        String artifactsRootDirectory = deploymentTaskContext.getRootDirectory().getAbsolutePath();
        try {
            initFileSpec(deploymentTaskContext);
            SpecsHelper specsHelper = new SpecsHelper(bambooBuildInfoLog);
            specsHelper.uploadArtifactsBySpec(fileSpec, new File(artifactsRootDirectory), new HashMap<>(), clientBuilder);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
        } catch (Exception e) {
            String message = "Exception occurred while executing deployment task";
            log.error(message, e);
            buildLogger.addErrorLogEntry(message, e);
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
     * Return configuration of the Artifactory server to perform the upload to.
     * The configuration is filled with overriding information provided to the task definition and environment variables.
     */
    private ServerConfig getUploadServerConfig(@NotNull DeploymentTaskContext deploymentTaskContext) {
        ServerConfig selectedServerConfig = getSelectedServerConfig(deploymentTaskContext);
        // Get the deployer credentials configured in the task configuration
        String username = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.USERNAME);
        String password = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.PASSWORD);
        // If deployer credentials were not configured in the task configuration, use the credentials configured globally.
        if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
            username = selectedServerConfig.getUsername();
            password = selectedServerConfig.getPassword();
        }
        return new ServerConfig(selectedServerConfig.getId(), selectedServerConfig.getUrl(), username, password, selectedServerConfig.getTimeout());
    }

    /**
     * Get configurations of the selected server in the task definition.
     */
    private ServerConfig getSelectedServerConfig(@NotNull DeploymentTaskContext deploymentTaskContext) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        String serverId = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM);
        if (StringUtils.isBlank(serverId)) {
            // Compatibility with version 1.8.0
            serverId = deploymentTaskContext.getConfigurationMap().get("artifactoryServerId");
        }
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(Long.parseLong(serverId));
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