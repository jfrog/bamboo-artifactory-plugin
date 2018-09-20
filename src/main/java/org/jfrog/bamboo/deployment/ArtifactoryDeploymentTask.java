package org.jfrog.bamboo.deployment;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.AbstractArtifactoryConfiguration;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
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
public class ArtifactoryDeploymentTask implements DeploymentTaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryDeploymentTask.class);

    @NotNull
    @Override
    public TaskResult execute(@NotNull DeploymentTaskContext deploymentTaskContext) throws TaskException {
        BuildLogger buildLogger = deploymentTaskContext.getBuildLogger();
        Log bambooBuildInfoLog = new BuildInfoLog(log, buildLogger);
        ServerConfig serverConfig = getServerConfig(deploymentTaskContext);
        if (serverConfig == null) {
            buildLogger.addErrorLogEntry("Could not find Artifactpry server. Please check the Artifactory server in the task configuration.");
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
        // Get the deployer credentials configured in the task configuration
        String username = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentConfiguration.USERNAME);
        String password = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentConfiguration.PASSWORD);
        // If deployer credentials were not configured in the task configuration, use the credentials configured
        // globally
        if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
            username = serverConfig.getUsername();
            password = serverConfig.getPassword();
        }
        return doUpload(deploymentTaskContext, buildLogger, bambooBuildInfoLog, serverConfig, username, password);
    }

    private TaskResult doUpload(@NotNull DeploymentTaskContext deploymentTaskContext, BuildLogger buildLogger,
                                Log bambooBuildInfoLog, ServerConfig serverConfig, String username, String password) {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(serverConfig.getUrl(), username, password, bambooBuildInfoLog);
        String artifactsRootDirectory = deploymentTaskContext.getRootDirectory().getAbsolutePath();
        try {
            String spec = getFileSpec(deploymentTaskContext, buildLogger);
            if (StringUtils.isBlank(spec)) {
                String err = "Spec file can't be empty";
                buildLogger.addErrorLogEntry(err);
                log.error(err);
                return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
            }
            buildLogger.addBuildLogEntry(spec);
            SpecsHelper specsHelper = new SpecsHelper(bambooBuildInfoLog);
            specsHelper.uploadArtifactsBySpec(spec, new File(artifactsRootDirectory), new HashMap<>(), client);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
        } catch (Exception e) {
            buildLogger.addErrorLogEntry("Error while deploying artifacts to Artifactory: " + e.getMessage());
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        } finally {
            client.close();
        }
    }

    private String getFileSpec(@NotNull DeploymentTaskContext deploymentTaskContext, BuildLogger buildLogger) throws IOException {
        String specSourceChoice = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentConfiguration.SPEC_SOURCE_CHOICE);
        if (StringUtils.isBlank(specSourceChoice)) {
            buildLogger.addBuildLogEntry("Converting legacy configuration to upload spec");
            return LegacyDeploymentUtils.buildDeploymentSpec(deploymentTaskContext);
        }
        if (AbstractArtifactoryConfiguration.CFG_SPEC_SOURCE_JOB_CONFIGURATION.equals(specSourceChoice)) {
            buildLogger.addBuildLogEntry("Using task configuration spec");
            return deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentConfiguration.SPEC_SOURCE_JOB_CONFIGURATION);
        }
        String specFileLocation = deploymentTaskContext.getConfigurationMap()
                .get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentConfiguration.SPEC_SOURCE_FILE);
        buildLogger.addBuildLogEntry("Using spec from file located at: " + specFileLocation);
        return TaskUtils.getSpecFromFile(deploymentTaskContext.getRootDirectory(), specFileLocation);
    }

    private ServerConfig getServerConfig(@NotNull DeploymentTaskContext deploymentTaskContext) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        String serverId = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM);
        if (StringUtils.isBlank(serverId)) {
            // Compatibility with version 1.8.0
            serverId = deploymentTaskContext.getConfigurationMap().get("artifactoryServerId");
        }
        return serverConfigManager.getServerConfigById(Long.parseLong(serverId));
    }
}