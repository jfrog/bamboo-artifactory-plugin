package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.XrayScanContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.client.artifactoryXrayResponse.Summary;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryXrayClient;

import java.io.IOException;

/**
 * Created by Bar Belity on 24/05/2018.
 */
public class ArtifactoryXrayScanTask implements TaskType
{
    private static final Logger log = Logger.getLogger(ArtifactoryXrayScanTask.class);
    private PluginAccessor pluginAccessor;

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor){
        this.pluginAccessor = pluginAccessor;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        BuildLogger logger = taskContext.getBuildLogger();

        XrayScanContext xrayContext = new XrayScanContext(taskContext.getConfigurationMap());

        ArtifactoryXrayClient client = createArtifactoryXrayClient(xrayContext, logger);

        try {
            TaskUtils.ReportTaskUsageToArtifactory(client, "rt_build_scan", pluginAccessor, logger);
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

    private ArtifactoryXrayClient createArtifactoryXrayClient(XrayScanContext xrayContext, BuildLogger logger) {
        ServerConfig serverConfig = getServerConfig(xrayContext);
        // Extract parameters for Xray Client
        String artifactoryUrl = serverConfig.getUrl();
        String username = StringUtils.isBlank(xrayContext.getUsername()) ? serverConfig.getUsername() : xrayContext.getUsername();
        String password = StringUtils.isBlank(xrayContext.getPassword()) ? serverConfig.getPassword() : xrayContext.getPassword();

        return new ArtifactoryXrayClient(artifactoryUrl, username, password, new BuildInfoLog(log, logger));
    }

    private ServerConfig getServerConfig(XrayScanContext xrayContext) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        String serverId = xrayContext.getArtifactoryServerId();
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(Long.parseLong(serverId));
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }

        return serverConfig;
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
