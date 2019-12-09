package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.XrayScanContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.client.artifactoryXrayResponse.Summary;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryXrayClient;

import java.io.IOException;

/**
 * Created by Bar Belity on 24/05/2018.
 */
public class ArtifactoryXrayScanTask extends ArtifactoryTaskType {
    private static final Logger log = Logger.getLogger(ArtifactoryXrayScanTask.class);
    private ServerConfig xrayServerConfig;
    private XrayScanContext xrayContext;
    private BuildLogger logger;

    @Override
    protected void initTask(@NotNull TaskContext context) {
        logger = context.getBuildLogger();
        xrayContext = new XrayScanContext(context.getConfigurationMap());
        setXrayServerConfigurations(xrayContext);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) throws TaskException {
        ArtifactoryXrayClient client = createArtifactoryXrayClient(logger);

        try {
            ArtifactoryXrayResponse buildScanResult = doXrayScan(taskContext, client);

            String scanMessage;
            try {
                scanMessage = handleXrayScanResult(buildScanResult, logger, xrayContext);
            } catch (Exception e) {
                logger.addErrorLogEntry(e.getMessage());
                log.error(e.getMessage());
                return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
            }

            logger.addBuildLogEntry(scanMessage);
            log.info(scanMessage);
            return TaskResultBuilder.newBuilder(taskContext).success().build();

        } catch (Exception e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        } finally {
            client.close();
        }
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return xrayServerConfig;
    }

    @Override
    protected String getTaskUsageName() {
        return "xray_scan";
    }

    @Override
    protected Log getLog() {
        return new BuildInfoLog(log, logger);
    }

    private String handleXrayScanResult(ArtifactoryXrayResponse buildScanResult, BuildLogger logger, XrayScanContext xrayContext) throws IOException {
        Summary summary = getSummaryFromResponse(buildScanResult);
        // Log scan link
        if (StringUtils.isNotEmpty(summary.getMoreDetailsUrl())) {
            logger.addBuildLogEntry("Xray scan details are available at: " + summary.getMoreDetailsUrl());
        }

        String scanMessage = summary.getMessage();
        // Check if scan returned vulnerable
        boolean isVulnerable = summary.isFailBuild();
        if (isVulnerable) {
            // Check if should fail build
            if (xrayContext.isFailIfVulnerable()) {
                throw new IOException(scanMessage);
            }
        }

        return scanMessage;
    }

    private Summary getSummaryFromResponse(ArtifactoryXrayResponse buildScanResult) {
        Summary summary = buildScanResult.getSummary();
        if (summary == null) {
            throw new IllegalStateException("Failed while processing the JSON result: 'summary' field is missing.");
        }
        return summary;
    }

    private ArtifactoryXrayClient createArtifactoryXrayClient(BuildLogger logger) {
        // Extract parameters for Xray Client.
        return new ArtifactoryXrayClient(xrayServerConfig.getUrl(), xrayServerConfig.getUsername(), xrayServerConfig.getPassword(), new BuildInfoLog(log, logger));
    }

    private void setXrayServerConfigurations(XrayScanContext xrayContext) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        String serverId = xrayContext.getArtifactoryServerId();
        xrayServerConfig = serverConfigManager.getServerConfigById(Long.parseLong(serverId));
        if (xrayServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        String username = StringUtils.isBlank(xrayContext.getUsername()) ? xrayServerConfig.getUsername() : xrayContext.getUsername();
        xrayServerConfig.setUsername(username);
        String password = StringUtils.isBlank(xrayContext.getPassword()) ? xrayServerConfig.getPassword() : xrayContext.getPassword();
        xrayServerConfig.setPassword(password);
    }

    private ArtifactoryXrayResponse doXrayScan(TaskContext taskContext, ArtifactoryXrayClient client) throws IOException, InterruptedException {
        // Extract build parameters
        BuildContext buildContext = taskContext.getBuildContext();
        String buildNumber = String.valueOf(buildContext.getBuildNumber());
        String buildName = buildContext.getPlanName();

        // Launch Xray Scan
        return client.xrayScanBuild(buildName, buildNumber, "bamboo");
    }
}
