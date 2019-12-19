package org.jfrog.bamboo.util;

import com.atlassian.bamboo.task.TaskContext;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.security.EncryptionHelper;
import org.jfrog.bamboo.util.generic.GenericData;
import org.jfrog.bamboo.util.version.VcsHelper;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.jfrog.bamboo.util.ConstantValues.AGGREGATED_BUILD_INFO;

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

    public static String decryptIfNeeded(String s) {
        try {
            s = EncryptionHelper.decrypt(s);
        } catch (RuntimeException e) {
            // Ignore. The field may not be encrypted.
        }
        return s;
    }

    public static ServerConfig getResolutionServerConfig(String baseUsername, String basePassword, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BuildParamsOverrideManager buildParamsOverrideManager) {
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

        return new ServerConfig(serverConfig.getId(), serverUrl, username, password, serverConfig.getTimeout());
    }

    public static ServerConfig getDeploymentServerConfig(String baseUsername, String basePassword, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BuildParamsOverrideManager buildParamsOverrideManager) {
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

        return new ServerConfig(serverConfig.getId(), serverUrl, username, password, serverConfig.getTimeout());
    }

    public static ArtifactoryDependenciesClientBuilder getArtifactoryDependenciesClientBuilder(ServerConfig serverConfig, BuildInfoLog log) {
        ArtifactoryDependenciesClientBuilder dependenciesClientBuilder = new ArtifactoryDependenciesClientBuilder();
        dependenciesClientBuilder.setArtifactoryUrl(serverConfig.getUrl()).setUsername(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword()).setLog(log).setConnectionTimeout(serverConfig.getTimeout());
        ProxyUtils.setProxyConfig(serverConfig.getUrl(), dependenciesClientBuilder);
        return dependenciesClientBuilder;
    }

    public static ArtifactoryDependenciesClient getArtifactoryDependenciesClient(ServerConfig serverConfig, BuildInfoLog log) {
        ArtifactoryDependenciesClient dependenciesClient = new ArtifactoryDependenciesClient(serverConfig.getUrl(),
                serverConfig.getUsername(), serverConfig.getPassword(), log);
        dependenciesClient.setConnectionTimeout(serverConfig.getTimeout());
        ProxyUtils.setProxyConfig(serverConfig.getUrl(), dependenciesClient);
        return dependenciesClient;
    }

    public static ArtifactoryBuildInfoClientBuilder getArtifactoryBuildInfoClientBuilder(ServerConfig serverConfig, BuildInfoLog log) {
        ArtifactoryBuildInfoClientBuilder clientBuilder = new ArtifactoryBuildInfoClientBuilder();
        clientBuilder.setArtifactoryUrl(serverConfig.getUrl()).setUsername(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword()).setLog(log).setConnectionTimeout(serverConfig.getTimeout());
        ProxyUtils.setProxyConfig(serverConfig.getUrl(), clientBuilder);
        return clientBuilder;
    }

    public static ArtifactoryBuildInfoClient getArtifactoryBuildInfoClient(ServerConfig serverConfig, BuildInfoLog log) {
        ArtifactoryBuildInfoClient buildInfoClient = new ArtifactoryBuildInfoClient(serverConfig.getUrl(),
                serverConfig.getUsername(), serverConfig.getPassword(), log);
        buildInfoClient.setConnectionTimeout(serverConfig.getTimeout());
        ProxyUtils.setProxyConfig(serverConfig.getUrl(), buildInfoClient);
        return buildInfoClient;
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

    /**
     * Add build info stored in a file to the build-info stored in plan's context.
     */
    public static void addBuildInfoFromFileToContext(TaskContext taskContext, String buildInfoFilePath) throws IOException {
        if (StringUtils.isBlank(buildInfoFilePath)) {
            throw new IllegalArgumentException("Provided empty build-info file path.");
        }

        // Read build-info from file.
        Path generatedBuildInfoPath = Paths.get(buildInfoFilePath);
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(generatedBuildInfoPath, StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        generatedBuildInfoPath.toFile().delete();

        // Create build object.
        String buildInfoJson = contentBuilder.toString();
        if (org.apache.commons.lang3.StringUtils.isBlank(buildInfoJson)) {
            return;
        }
        Build build = BuildInfoExtractorUtils.jsonStringToBuildInfo(buildInfoJson);

        appendBuildToBuildInfoInContext(taskContext, build);
    }

    /**
     * Append a 'Build' object to the build-info stored in plan's context.
     */
    public static void appendBuildToBuildInfoInContext(TaskContext taskContext, Build build) throws IOException {
        GenericData gd = new GenericData();
        String buildInfo = getAggregatedBuildInfo(taskContext);
        if (!StringUtils.isBlank(buildInfo)) {
            gd = BuildInfoExtractorUtils.jsonStringToGeneric(buildInfo, GenericData.class);
        }
        gd.addBuild(build);
        buildInfo = BuildInfoExtractorUtils.buildInfoToJsonString(gd);
        addAggregatedBuildInfoToContext(taskContext, buildInfo);
    }

    private static void addAggregatedBuildInfoToContext(TaskContext taskContext, String buildInfo) {
        taskContext.getBuildContext().getParentBuildContext().getBuildResult().
                getCustomBuildData().put(AGGREGATED_BUILD_INFO, buildInfo);
    }

    private static String getAggregatedBuildInfo(TaskContext taskContext) {
        return taskContext.getBuildContext().getParentBuildContext().getBuildResult().
                getCustomBuildData().get(AGGREGATED_BUILD_INFO);
    }

    public static String getAndDeleteAggregatedBuildInfo(TaskContext taskContext) {
        return taskContext.getBuildContext().getParentBuildContext().getBuildResult().
                getCustomBuildData().remove(AGGREGATED_BUILD_INFO);
    }
}
