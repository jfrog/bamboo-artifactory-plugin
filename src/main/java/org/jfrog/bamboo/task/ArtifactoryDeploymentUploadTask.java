package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.ArtifactoryDeploymentUploadConfiguration;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.deployment.LegacyDeploymentUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Bamboo deployment Artifactory task - Takes pre defined artifacts from a build plan and deploys them to Artifactory
 *
 * @author Aviad Shikloshi
 */
public class ArtifactoryDeploymentUploadTask extends AbstractSpecTask implements DeploymentTaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryDeploymentUploadTask.class);
    private BuildLogger buildLogger;

    @NotNull
    @Override
    public TaskResult execute(@NotNull DeploymentTaskContext deploymentTaskContext) throws TaskException {
        buildLogger = deploymentTaskContext.getBuildLogger();
        ServerConfig serverConfig = getServerConfig(deploymentTaskContext);
        if (serverConfig == null) {
            buildLogger.addErrorLogEntry("Could not find Artifactpry server. Please check the Artifactory server in the task configuration.");
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
        // Get the deployer credentials configured in the task configuration
        String username = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.USERNAME);
        String password = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.PASSWORD);
        // If deployer credentials were not configured in the task configuration, use the credentials configured
        // globally
        if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
            username = serverConfig.getUsername();
            password = serverConfig.getPassword();
        }
        return upload(deploymentTaskContext, serverConfig, username, password);
    }

    @Override
    protected File getWorkingDirectory(@NotNull CommonTaskContext context) {
        return context.getRootDirectory();
    }

    @Override
    protected String getJobConfigurationSpec(@NotNull CommonTaskContext context) {
        return context.getConfigurationMap()
                .get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_JOB_CONFIGURATION);
    }

    @Override
    protected String getFilePathSpec(@NotNull CommonTaskContext context) {
        return context.getConfigurationMap()
                .get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_FILE);
    }

    @Override
    protected Boolean isFileSpecInJobConfiguration(@NotNull CommonTaskContext context) {
        return ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_JOB_CONFIGURATION.equals(
                context.getConfigurationMap()
                        .get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_CHOICE));
    }

    @Override
    protected TaskResult initFileSpec(@NotNull CommonTaskContext context) throws IOException {
        String specSourceChoice = context.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.SPEC_SOURCE_CHOICE);
        if (StringUtils.isNotBlank(specSourceChoice)) {
            return super.initFileSpec(context);
        }
        buildLogger.addBuildLogEntry("Converting legacy configuration to upload spec");
        fileSpec = LegacyDeploymentUtils.buildDeploymentSpec(context);
        buildLogger.addBuildLogEntry("Spec: " + fileSpec);
        return validateFileSpec(context);
    }

    private TaskResult upload(@NotNull DeploymentTaskContext deploymentTaskContext, ServerConfig serverConfig, String username, String password) {
        Log bambooBuildInfoLog = new BuildInfoLog(log, buildLogger);
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(serverConfig.getUrl(), username, password, bambooBuildInfoLog);
        String artifactsRootDirectory = deploymentTaskContext.getRootDirectory().getAbsolutePath();
        try {
            TaskResult taskResult = initFileSpec(deploymentTaskContext);
            if (taskResult != null) {
                return taskResult;
            }
            SpecsHelper specsHelper = new SpecsHelper(bambooBuildInfoLog);
            specsHelper.uploadArtifactsBySpec(fileSpec, new File(artifactsRootDirectory), new HashMap<>(), client);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
        } catch (Exception e) {
            buildLogger.addErrorLogEntry("Error while deploying artifacts to Artifactory: " + e.getMessage());
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        } finally {
            client.close();
        }
    }

    private ServerConfig getServerConfig(@NotNull DeploymentTaskContext deploymentTaskContext) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        String serverId = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM);
        if (StringUtils.isBlank(serverId)) {
            // Compatibility with version 1.8.0
            serverId = deploymentTaskContext.getConfigurationMap().get("artifactoryServerId");
        }
        return serverConfigManager.getServerConfigById(Long.parseLong(serverId));
    }
}