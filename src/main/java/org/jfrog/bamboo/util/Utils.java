package org.jfrog.bamboo.util;

import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.runtime.RuntimeTaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.builder.BaseBuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.release.provider.TokenDataProvider;

import java.util.Map;
import java.util.Properties;

/**
 * Created by Tamirh on 26/07/2016.
 */
public class Utils {

    public static <V> Map<String, V> filterMapKeysByPrefix(Map<String, V> map, String prefix) {
        Map<String, V> result = new HashedMap();
        if (map == null) {
            return result;
        }
        for (Map.Entry<String, V> entry : map.entrySet()) {
            if (StringUtils.startsWith(entry.getKey(), prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static <V> Map<String, V> filterPropertiesKeysByPrefix(Properties properties, String prefix) {
        Map<String, V> result = new HashedMap();
        if (properties == null) {
            return result;
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            V value = (V) entry.getValue();
            if (StringUtils.startsWith(key, prefix)) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * The security token is passed for remote agents to be able to copy the maven build-info.json
     * and gradle gradle.properties files so the release management will be able to read them.
     * @param buildContext Bamboo's build context
     * @param runtimeTaskDefinition Maven or Gradle runtime task definition
     * @return the security token
     */
    public static String getTaskSecurityToken(BuildContext buildContext, TaskDefinition runtimeTaskDefinition) {
        for (RuntimeTaskDefinition task : buildContext.getRuntimeTaskDefinitions()) {
            if (task.equals(runtimeTaskDefinition)) {
                return task.getRuntimeContext().get(TokenDataProvider.SECURITY_TOKEN);
            }
        }
        return null;
    }

    public static String getUsername(String username, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BaseBuildInfoHelper buildInfoHelper) {
        username = buildInfoHelper.overrideParam(serverConfigManager.substituteVariables(username),
                BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        return username;
    }

    public static String getPassword(String password, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BaseBuildInfoHelper buildInfoHelper) {
        password = buildInfoHelper.overrideParam(serverConfigManager.substituteVariables(password),
                BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        return password;
    }

    public static String getArtifactoryVersion(PluginAccessor pluginAccessor) {
        Plugin plugin = pluginAccessor.getPlugin(ConstantValues.ARTIFACTORY_PLUGIN_KEY);
        if (plugin != null) {
            return plugin.getPluginInformation().getVersion();
        }
        return StringUtils.EMPTY;
    }
}
