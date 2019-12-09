package org.jfrog.bamboo.task;

import com.atlassian.plugin.PluginAccessor;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.usageReport.UsageReporter;

/**
 * Created by Bar Belity on 08/12/2019.
 */
public abstract class ArtifactoryTaskBase {

    protected PluginAccessor pluginAccessor;

    protected abstract ServerConfig getUsageServerConfig();

    protected abstract String getTaskUsageName();

    protected abstract Log getLog();

    protected void reportUsage(ServerConfig serverConfig, String taskName, Log log) {
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
