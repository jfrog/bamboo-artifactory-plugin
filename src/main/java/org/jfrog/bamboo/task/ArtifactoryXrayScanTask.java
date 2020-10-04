package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.XrayScanContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.ProxyUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.client.artifactoryXrayResponse.Summary;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryXrayClient;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Bar Belity on 24/05/2018.
 */
public class ArtifactoryXrayScanTask extends ArtifactoryTaskType {
    private static final Logger log = Logger.getLogger(ArtifactoryXrayScanTask.class);
    private CustomVariableContext customVariableContext;
    private ServerConfig xrayServerConfig;
    private XrayScanContext xrayContext;
    private BuildLogger logger;
    private Log buildInfoLog;

    @Override
    protected void initTask(@NotNull TaskContext context) {
        logger = context.getBuildLogger();
        buildInfoLog = new BuildInfoLog(log, context.getBuildLogger());
        xrayContext = new XrayScanContext(context.getConfigurationMap());
        setXrayServerConfigurations(context);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {

        try (ArtifactoryXrayClient client = createArtifactoryXrayClient(logger)) {
            ArtifactoryXrayResponse buildScanResult = doXrayScan(taskContext, client);
            String scanMessage;
            try {
                scanMessage = handleXrayScanResult(buildScanResult, logger, xrayContext);
            } catch (Exception e) {
                buildInfoLog.error(e.getMessage(), e);
                return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
            }

            buildInfoLog.info(scanMessage);
            return TaskResultBuilder.newBuilder(taskContext).success().build();

        } catch (Exception e) {
            buildInfoLog.error("Exception occurred while executing task", e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
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
        ArtifactoryXrayClient client = new ArtifactoryXrayClient(xrayServerConfig.getUrl(), xrayServerConfig.getUsername(),
                xrayServerConfig.getPassword(), new BuildInfoLog(log, logger));
        // Add proxy Configurations.
        ProxyUtils.setProxyConfig(xrayServerConfig.getUrl(), client);
        return client;
    }

    private void setXrayServerConfigurations(TaskContext taskContext) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        long serverId = xrayContext.getArtifactoryServerId();
        xrayServerConfig = serverConfigManager.getServerConfigById(serverId);
        if (xrayServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        Map<String, String> runtimeContext = taskContext.getRuntimeTaskContext();
        xrayServerConfig = TaskUtils.getResolutionServerConfig(
                xrayContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                xrayContext.getOverriddenPassword(runtimeContext, buildInfoLog, true),
                serverConfigManager, xrayServerConfig, new BuildParamsOverrideManager(customVariableContext));
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
