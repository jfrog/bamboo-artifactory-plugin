package org.jfrog.bamboo.util;

import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.security.EncryptionHelper;
import org.jfrog.bamboo.util.version.VcsHelper;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Utility class that serves as a helper for common operations of a task.
 *
 * @author Tomer Cohen
 */
public class TaskUtils {

    /**
     * Get an escaped version of the environment map that is to be passed onwards to the extractors. Bamboo escapes the
     * key of the property and replaces all '.' into '_' as well as adds the "bamboo" prefixHence a conversion back is
     * needed.
     *
     * @param env The original environment map.
     * @return The escaped environment map.
     */
    public static Map<String, String> getEscapedEnvMap(Map<String, String> env) {
        Map<String, String> result = Maps.newHashMap();
        if (env != null) {
            for (Map.Entry<String, String> entry : env.entrySet()) {
                String escaped = entry.getKey().replace('_', '.');
                escaped = StringUtils.removeStart(escaped, "bamboo.");
                result.put(escaped, entry.getValue());
            }
        }
        return result;
    }

    /**
     * Append the path of the build info properties file as a system property to the list of arguments that is given to
     * the build (as a -D param).
     */
    public static void appendBuildInfoPropertiesArgument(List<String> arguments, String buildInfoPropertiesFile) {
        if ((arguments != null) && StringUtils.isNotBlank(buildInfoPropertiesFile)) {
            arguments.add(Commandline.quoteArgument("-D" + BuildInfoConfigProperties.PROP_PROPS_FILE + "=" +
                    buildInfoPropertiesFile));
        }
    }

    /**
     * Find maven or gradle task for a specific plan
     *
     * @param plan - plan configuration in which we are trying to find the maven/gradle task
     * @return appropriate task definition or null in case no such task exist
     */
    public static TaskDefinition getMavenOrGradleTaskDefinition(ImmutablePlan plan) {
        if (plan == null) {
            return null;
        }
        List<TaskDefinition> definitions = plan.getBuildDefinition().getTaskDefinitions();
        if (definitions.isEmpty()) {
            return null;
        }
        return TaskDefinitionHelper.findMavenOrGradleDefinition(definitions);
    }

    public static ArtifactoryBuildInfoClient createClient(ServerConfigManager serverConfigManager, ServerConfig serverConfig,
                                                          AbstractBuildContext context, Logger log) {
        String serverUrl = substituteVariables(serverConfigManager, serverConfig.getUrl());
        String username = substituteVariables(serverConfigManager, context.getDeployerUsername());
        if (StringUtils.isBlank(username)) {
            username = substituteVariables(serverConfigManager, serverConfig.getUsername());
        }
        ArtifactoryBuildInfoClient client;
        BuildInfoLog bambooLog = new BuildInfoLog(log);
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

    public static String decryptIfNeeded(String s) {
        try {
            s = EncryptionHelper.decrypt(s);
        } catch (RuntimeException e) {
            // Ignore. The field may not be encrypted.
        }
        return s;
    }

    public static ServerConfigBase getResolutionServerConfig(String baseUsername, String basePassword, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BuildParamsOverrideManager buildParamsOverrideManager) {
        if (serverConfig == null) {
            return null;
        }
        String username = overrideParam(serverConfigManager.substituteVariables(baseUsername), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME, buildParamsOverrideManager);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        String password = overrideParam(serverConfigManager.substituteVariables(basePassword), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD, buildParamsOverrideManager);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());

        return new ServerConfigBase(serverUrl, username, password);
    }

    public static ServerConfigBase getDeploymentServerConfig(String baseUsername, String basePassword, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BuildParamsOverrideManager buildParamsOverrideManager) {
        if (serverConfig == null) {
            return null;
        }
        String username = overrideParam(serverConfigManager.substituteVariables(baseUsername), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME, buildParamsOverrideManager);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        String password = overrideParam(serverConfigManager.substituteVariables(basePassword), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD, buildParamsOverrideManager);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());

        return new ServerConfigBase(serverUrl, username, password);
    }

    public static ArtifactoryDependenciesClient getArtifactoryDependenciesClient(ServerConfigBase serverConfig, Logger log) {
        return new ArtifactoryDependenciesClient(serverConfig.getUrl(), serverConfig.getUsername(), serverConfig.getPassword(), new BuildInfoLog(log));
    }

    private static String overrideParam(String originalValue, String overrideKey, BuildParamsOverrideManager buildParamsOverrideManager) {
        String overriddenValue = buildParamsOverrideManager.getOverrideValue(overrideKey);
        return overriddenValue.isEmpty() ? originalValue : overriddenValue;
    }

    /**
     * Get the checkout directory if exists, or the plan's default working directory otherwise
     *
     * @param taskContext - the task's context
     * @return checkout directory if exists, plan's default working directory otherwise
     */
    public static File getVcsWorkingDirectory(TaskContext taskContext) {
        File checkoutDir = VcsHelper.getCheckoutDirectory(taskContext.getBuildContext());
        if (checkoutDir != null) {
            return checkoutDir;
        }
        return taskContext.getWorkingDirectory();
    }
}