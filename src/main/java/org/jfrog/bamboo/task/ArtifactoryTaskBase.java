package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.plugin.PluginAccessor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.usageReport.UsageReporter;

import java.io.File;

/**
 * Created by Bar Belity on 08/12/2019.
 */
public abstract class ArtifactoryTaskBase {

    protected static final Logger log = Logger.getLogger(ArtifactoryTaskBase.class);
    protected PluginAccessor pluginAccessor;
    protected CommonTaskContext taskContext;
    // True if the task is attending to be run in a Docker container
    protected boolean containerized;
    // File separator of the target agent: '/' in Unix/Linux/container or '\' in Windows
    protected String fileSeparator;
    protected BuildLogger logger;
    protected Log buildInfoLog;

    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        this.taskContext = context;
        this.logger = taskContext.getBuildLogger();
        this.buildInfoLog = new BuildInfoLog(log, logger);
        this.containerized = taskContext.getCommonContext().getDockerPipelineConfiguration().isEnabled();
        this.fileSeparator = containerized ? "/" : File.separator;
    }

    protected abstract ServerConfig getUsageServerConfig();

    protected abstract String getTaskUsageName();

    protected void reportUsage(ServerConfig serverConfig, String taskName, Log log) {
        String[] featureIdArray = new String[]{taskName};
        UsageReporter usageReporter = new UsageReporter("bamboo-artifactory-plugin/" + Utils.getPluginVersion(pluginAccessor), featureIdArray);

        try {
            usageReporter.reportUsage(serverConfig.getUrl(), serverConfig.getUsername(), serverConfig.getPassword(), "", null, log);
            log.info("Usage info sent successfully.");
        } catch (Exception ex) {
            log.info("Failed sending usage report to Artifactory: " + ex);
        }
    }

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }
}
