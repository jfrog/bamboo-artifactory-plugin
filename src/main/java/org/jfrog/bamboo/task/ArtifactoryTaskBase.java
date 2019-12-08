package org.jfrog.bamboo.task;

import com.atlassian.bamboo.task.*;
import com.atlassian.plugin.PluginAccessor;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.util.ServerConfigBase;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.usageReport.UsageReporter;

/**
 * Created by Bar Belity on 02/12/2019.
 */
public abstract class ArtifactoryTaskBase implements TaskType {
    protected PluginAccessor pluginAccessor;

    protected abstract ServerConfigBase getUsageServerConfig();

    protected abstract String getTaskUsageName();

    protected abstract TaskResult runTask(@NotNull TaskContext context) throws TaskException;

    protected abstract boolean initTask(@NotNull TaskContext context) throws TaskException;

    protected abstract Log getLog();

    public TaskResult execute(@NotNull TaskContext context) throws TaskException {
        // Initialize task.
        boolean initSuccess = initTask(context);
        if (!initSuccess) {
            return TaskResultBuilder.newBuilder(context).failed().build();
        }

        // Report task usage to Artifactory.
        ServerConfigBase server = getUsageServerConfig();
        if (server != null) {
            reportUsage(server, getTaskUsageName(), getLog());
        }

        // Run task execution.
        return runTask(context);
    }

    private void reportUsage(ServerConfigBase serverConfig, String taskName, Log log) {
        String[] featureIdArray = new String[] {taskName};
        UsageReporter usageReporter = new UsageReporter("bamboo-artifactory-plugin/" + Utils.getPluginVersion(pluginAccessor), featureIdArray);

        try {
            usageReporter.reportUsage(serverConfig.getUrl(), serverConfig.getUsername(), serverConfig.getPassword(), "", null, log);
            log.info("Usage info sent successfully.");
        } catch (Exception ex) {
            log.info("Failed sending usage report to Artifactory: " + ex);
        }
    }

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor){
        this.pluginAccessor = pluginAccessor;
    }
}
