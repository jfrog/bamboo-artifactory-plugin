package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author Aviad Shikloshi
 */
public class MavenSyncUtils {

    private static final Logger log = Logger.getLogger(MavenSyncUtils.class);
    private static final String NEXUS_PUSH_PLUGIN_NAME = "bintrayOsoPush";

    public static boolean isPushToNexusEnabled(ServerConfigManager serverConfigManager, TaskDefinition definition,
                                               String serverId) {
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
        if (definition == null) {
            return false;
        }
        if (StringUtils.isBlank(serverId)) {
            log.error("No special promotion modes enabled: no selected Artifactory server Id.");
            return false;
        }
        ServerConfig serverConfig = component.getServerConfigById(Long.parseLong(serverId));
        if (serverConfig == null) {
            log.error("No special promotion modes enabled: error while retrieving querying for enabled user plugins: " +
                    "could not find Artifactory server configuration by the ID " + serverId);
            return false;
        }
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(definition.getConfiguration());
        ArtifactoryBuildInfoClient client = createClient(serverConfigManager, serverConfig, context);
        try {
            Map<String, List<Map>> userPluginInfo = client.getUserPluginInfo();
            if (!userPluginInfo.containsKey("promotions")) {
                log.debug("No special promotion modes enabled: no 'execute' user plugins could be found.");
                return false;
            }
            List<Map> executionPlugins = userPluginInfo.get("promotions");
            Iterables.find(executionPlugins, new Predicate<Map>() {
                @Override
                public boolean apply(Map pluginInfo) {
                    if ((pluginInfo != null) && pluginInfo.containsKey("name")) {
                        String pluginName = pluginInfo.get("name").toString();
                        return NEXUS_PUSH_PLUGIN_NAME.equals(pluginName);
                    }
                    return false;
                }
            });
            return true;
        } catch (IOException ioe) {
            log.error("No special promotion modes enabled: error while retrieving querying for enabled user plugins: " +
                    ioe.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("No special promotion modes enabled: error while retrieving querying for enabled user " +
                        "plugins.", ioe);
            }
        } catch (NoSuchElementException nsee) {
            log.debug("No special promotion modes enabled: no relevant execute user plugins could be found.");
        }
        return false;
    }

    public static ArtifactoryBuildInfoClient createClient(ServerConfigManager serverConfigManager, ServerConfig serverConfig,
                                                          AbstractBuildContext context) {
        String serverUrl = substituteVariables(serverConfigManager, serverConfig.getUrl());
        String username = substituteVariables(serverConfigManager, context.getDeployerUsername());
        if (StringUtils.isBlank(username)) {
            username = substituteVariables(serverConfigManager, serverConfig.getUsername());
        }
        ArtifactoryBuildInfoClient client;
        BambooBuildInfoLog bambooLog = new BambooBuildInfoLog(log);
        if (StringUtils.isBlank(username)) {
            client = new ArtifactoryBuildInfoClient(serverUrl, bambooLog);
        } else {
            String password = substituteVariables(serverConfigManager, context.getDeployerPassword());
            if (StringUtils.isBlank(password)) {
                password = substituteVariables(serverConfigManager, serverConfig.getPassword());
            }
            client = new ArtifactoryBuildInfoClient(serverUrl, username, password, bambooLog);
        }
        client.setConnectionTimeout(serverConfig.getTimeout());
        return client;
    }

    /**
     * Substitute (replace) Bamboo variable names with their defined values
     */
    private static String substituteVariables(ServerConfigManager serverConfigManager, String s) {
        return s != null ? serverConfigManager.substituteVariables(s) : null;
    }

}
