package org.jfrog.bamboo.util;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.util.version.VcsHelper;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jfrog.bamboo.configuration.BuildParamsOverrideManager.*;
import static org.jfrog.bamboo.util.ConstantValues.AGGREGATED_BUILD_INFO;

/**
 * Utility class that serves as a helper for common operations of a task.
 *
 * @author Tomer Cohen
 */
public class TaskUtils {
    // Bamboo's buildData variable size is limited by the size of an environment variable in the operating system.
    // This variable limits the size of a buildData variable.
    private static final int MAX_BUILD_DATA_SIZE = 2047;

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

    public static ServerConfig getResolutionServerConfig(String baseUsername, String basePassword, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BuildParamsOverrideManager buildParamsOverrideManager) {
        return getRequestedServerConfig(baseUsername, basePassword, serverConfigManager, serverConfig, buildParamsOverrideManager,
                OVERRIDE_ARTIFACTORY_RESOLVER_URL, OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME, OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD);
    }

    public static ServerConfig getDeploymentServerConfig(String baseUsername, String basePassword, ServerConfigManager serverConfigManager, ServerConfig serverConfig, BuildParamsOverrideManager buildParamsOverrideManager) {
        return getRequestedServerConfig(baseUsername, basePassword, serverConfigManager, serverConfig, buildParamsOverrideManager,
                OVERRIDE_ARTIFACTORY_DEPLOYER_URL, OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME, OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD);
    }

    private static ServerConfig getRequestedServerConfig(String baseUsername, String basePassword,
                                                         ServerConfigManager serverConfigManager, ServerConfig serverConfig,
                                                         BuildParamsOverrideManager buildParamsOverrideManager,
                                                         String overrideUrlKey, String overrideUsernameKey, String overridePasswordKey) {
        if (serverConfig == null) {
            return null;
        }
        String username = overrideParam(serverConfigManager.substituteVariables(baseUsername), overrideUsernameKey, buildParamsOverrideManager);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        String password = overrideParam(serverConfigManager.substituteVariables(basePassword), overridePasswordKey, buildParamsOverrideManager);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }

        String serverUrl = overrideParam(serverConfigManager.substituteVariables(serverConfig.getUrl()), overrideUrlKey, buildParamsOverrideManager);

        return new ServerConfig(serverConfig.getId(), serverUrl, username, password, serverConfig.getTimeout());
    }

    public static ArtifactoryManagerBuilder getArtifactoryManagerBuilderBuilder(ServerConfig serverConfig, Log log) {
        ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder();
        artifactoryManagerBuilder.setServerUrl(serverConfig.getUrl()).setUsername(serverConfig.getUsername()).setPassword(serverConfig.getPassword())
                .setLog(log).setConnectionTimeout(serverConfig.getTimeout());
        ProxyUtils.setProxyConfig(serverConfig.getUrl(), artifactoryManagerBuilder);
        return artifactoryManagerBuilder;
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
    public static BuildInfo getBuildObjectFromBuildInfoFile(String buildInfoFilePath) throws IOException {
        if (StringUtils.isBlank(buildInfoFilePath)) {
            throw new IllegalArgumentException("Provided empty build-info file path.");
        }

        // Read build-info from file.
        Path generatedBuildInfoPath = Paths.get(buildInfoFilePath);
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(generatedBuildInfoPath, StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        generatedBuildInfoPath.toFile().delete();

        // Create build object.
        String buildInfoJson = contentBuilder.toString();
        if (StringUtils.isBlank(buildInfoJson)) {
            return null;
        }
        return BuildInfoExtractorUtils.jsonStringToBuildInfo(buildInfoJson);
    }

    /**
     * Split buildData variable to chunks, to bypass the limitation of an environment variable size.
     *
     * @param buildData - the buildData variable
     * @return chunks of the buildData variable
     */
    private static List<String> splitBuildDataToChunks(String buildData) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < buildData.length(); i += MAX_BUILD_DATA_SIZE) {
            chunks.add(buildData.substring(i, Math.min(buildData.length(), i + MAX_BUILD_DATA_SIZE)));
        }
        return chunks;
    }

    /**
     * Add a build info to the task context.
     *
     * @param taskContext - The task context
     * @param buildInfo   - String representation of the build info
     */
    public static void addBuildInfoToContext(TaskContext taskContext, String buildInfo) {
        Map<String, String> customBuildData = taskContext.getBuildContext().getParentBuildContext().getBuildResult().getCustomBuildData();
        List<String> chunks = splitBuildDataToChunks(buildInfo);
        for (int i = 0; i < chunks.size(); i++) {
            customBuildData.put(AGGREGATED_BUILD_INFO + "." + i, chunks.get(i));
        }
    }

    /**
     * Get and delete build info from the task context.
     *
     * @param taskContext - The task context
     * @return a string representation of the Build Info
     */
    public static String getAndDeleteAggregatedBuildInfo(TaskContext taskContext) {
        Map<String, String> customBuildData = taskContext.getBuildContext().getParentBuildContext().getBuildResult().getCustomBuildData();
        List<Map.Entry<String, String>> chunks = customBuildData.entrySet().stream()
                .filter(stringStringEntry -> stringStringEntry.getKey().startsWith(AGGREGATED_BUILD_INFO + "."))
                .sorted(Comparator.comparingInt(entry -> Integer.parseInt(StringUtils.substringAfterLast(entry.getKey(), "."))))
                .collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> chunk : chunks) {
            stringBuilder.append(chunk.getValue());
            customBuildData.remove(chunk.getKey());
        }
        return stringBuilder.toString();
    }

    /**
     * Add an executable path to a provided env map.
     *
     * @param env               - Environment variables map to add
     * @param buildContext      - The task's corresponding BuildContext
     * @param capabilityContext - The task's corresponding CapabilityContext
     * @param builderKey        - Builder prefix key, such as "system.builder.npm."
     * @param executableName    - Executable name in file system (os specific)
     * @param taskName          - Task's name
     * @param containerized     - True if the task is attending to be run in a Docker container
     * @return environment variables map with the added executable path
     * @throws TaskException - If capability is not defined or doesn't exist
     */
    public static Map<String, String> addExecutablePathToEnv(Map<String, String> env, PackageManagersContext buildContext, CapabilityContext capabilityContext, String builderKey, String executableName, String taskName, boolean containerized) throws TaskException {
        String executablePath = TaskUtils.getExecutablePath(buildContext, capabilityContext, builderKey, executableName, taskName, containerized);
        String path = env.get("PATH");
        if (SystemUtils.IS_OS_WINDOWS && !containerized) {
            path = executablePath + ";" + path;
        } else {
            path = executablePath + ":" + path;
        }
        env.put("PATH", path);
        return env;
    }

    /**
     * Returns the path to the executable needed for the task.
     * Path is built from the executable configuration and the executable name.
     *
     * @param buildContext      - The task's corresponding BuildContext
     * @param capabilityContext - The task's corresponding CapabilityContext
     * @param builderKey        - Builder prefix key, such as "system.builder.npm."
     * @param executableName    - Executable name in file system (os specific)
     * @param taskName          - Task's name
     * @param containerized     - True if the task is attending to be run in a Docker container
     * @return Path to requested executable.
     * @throws TaskException - If capability is not defined or doesn't exist
     */
    public static String getExecutablePath(PackageManagersContext buildContext, CapabilityContext capabilityContext,
                                           String builderKey, String executableName, String taskName, boolean containerized) throws TaskException {
        ReadOnlyCapabilitySet capabilitySet = capabilityContext.getCapabilitySet();
        if (capabilitySet == null) {
            return null;
        }
        Capability capability = capabilitySet.getCapability(builderKey + buildContext.getExecutable());
        if (capability == null || StringUtils.isEmpty(capability.getValue())) {
            throw new TaskException(taskName + " capability: " + buildContext.getExecutable() +
                    " is not defined, please check job configuration");
        }
        if (containerized) {
            return capability.getValue();
        }

        String path = Paths.get(capability.getValue(), "bin", executableName).toString();

        if (!new File(path).exists()) {
            path = Paths.get(capability.getValue()).toString();
            if (!new File(path).exists()) {
                throw new TaskException("Executable '" + executableName + "'  does not exist at path '" + path + "'");
            }
        }
        return path;
    }

    /**
     * Get a map of both environments variables configured in the task configuration and the ones already set.
     *
     * @param buildContext                - The task's corresponding BuildContext
     * @param environmentVariableAccessor - Accessor used to get available env
     * @return Map of all environment variables
     */
    public static Map<String, String> getEnvironmentVariables(PackageManagersContext buildContext, EnvironmentVariableAccessor environmentVariableAccessor) {
        Map<String, String> env = Maps.newHashMap();
        env.putAll(environmentVariableAccessor.getEnvironment());
        if (StringUtils.isNotBlank(buildContext.getEnvironmentVariables())) {
            env.putAll(environmentVariableAccessor
                    .splitEnvironmentAssignments(buildContext.getEnvironmentVariables(), false));
        }
        return env;
    }

    /**
     * Get a map of the commonly used artifact properties to be set when deploying an artifact.
     * Build name, build number, vcs url, vcs revision, timestamp.
     *
     * @return Map containing all properties.
     */
    public static ArrayListMultimap<String, String> getCommonArtifactPropertiesMap(BuildInfoHelper buildInfoHelper) {
        Map<String, String> propertiesMap = new HashMap<>();
        buildInfoHelper.addCommonProperties(propertiesMap);
        return ArrayListMultimap.create(Multimaps.forMap(propertiesMap));
    }

    /**
     * Return the global Bamboo temp folder. This folder is also mounted in containers.
     *
     * @param customVariableContext - Task custom variables
     * @return global the Global Bamboo temp folder
     */
    public static File getBambooTmp(CustomVariableContext customVariableContext) {
        return new File(customVariableContext.getVariableContexts().get("tmp.directory").getValue());
    }

    /**
     * Return the plan key of the running task.
     *
     * @param customVariableContext - Task's variables context
     * @return the plan key of the running task
     */
    public static String getPlanKey(CustomVariableContext customVariableContext) {
        return customVariableContext.getVariableContexts().get("planKey").getValue();
    }
}
