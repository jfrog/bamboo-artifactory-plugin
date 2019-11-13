package org.jfrog.bamboo.util;

import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.runtime.RuntimeTaskDefinition;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.security.EncryptionHelper;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Utility class that serves as a helper for common operations of a task.
 *
 * @author Tomer Cohen
 */
public class TaskUtils {
    private static final char PROPERTIES_DELIMITER = ';';
    private static final char KEY_VALUE_SEPARATOR = '=';
    /* This is the name of the "Download Artifacts" task in bamboo, we are looking it up as downloading artifacts is a pre condition to our task */
    private static final String DOWNLOAD_ARTIFACTS_TASK_KEY = "com.atlassian.bamboo.plugins.bamboo-artifact-downloader-plugin:artifactdownloadertask";

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
     * Create Multimap that represent build/deployment matrix param to attach uploaded artifacts
     *
     * @param propertiesInput String that separated by semicolon to parse into map
     * @return Multimap that represents the deployment properties, empty map if no properties attaches
     */
    public static Multimap<String, String> extractMatrixParamFromString(String propertiesInput) {
        Multimap<String, String> matrixParams = ArrayListMultimap.create();
        String[] matrixParamString = StringUtils.split(propertiesInput, PROPERTIES_DELIMITER);
        for (String s : matrixParamString) {
            String[] keyValueArr = StringUtils.split(s, KEY_VALUE_SEPARATOR);
            boolean validProperty = keyValueArr.length == 2;
            if (validProperty) {
                // No whitespace allowed in key
                String formatKey = keyValueArr[0].replace(" ", StringUtils.EMPTY);
                matrixParams.put(formatKey, keyValueArr[1].trim());
            }
        }
        return matrixParams;
    }

    /**
     * Get the Id of selected Artifactory
     *
     * @param definition task that uses Artifactory
     * @return artifactory id
     */
    public static String getSelectedServerId(TaskDefinition definition) {
        if (definition == null) {
            return StringUtils.EMPTY;
        }
        Map<String, String> configuration = definition.getConfiguration();
        Map<String, String> filtered = Maps.filterKeys(configuration, new Predicate<String>() {
            public boolean apply(String input) {
                return StringUtils.endsWith(input, AbstractBuildContext.SERVER_ID_PARAM);
            }
        });
        return filtered.values().iterator().next();
    }

    /**
     * Searching for the "Artifacts Download" task in a list of tasks
     *
     * @param runtimeTaskDefinitionList all the job tasks
     * @return Artifacts Download task, null if no such task exists
     */
    public static RuntimeTaskDefinition findDownloadArtifactsTask(@NotNull List<RuntimeTaskDefinition> runtimeTaskDefinitionList) {
        for (RuntimeTaskDefinition rtd : runtimeTaskDefinitionList) {
            if (rtd.getPluginKey().equals(DOWNLOAD_ARTIFACTS_TASK_KEY)) {
                return rtd;
            }
        }
        throw new IllegalStateException("\"Artifacts Download\" task must run before the \"Artifactory Deployment\" task.");
    }

    /**
     * Search for any Artifactory build task (Maven, Gradle, Ivy, Generic)
     *
     * @return configuration map for this task
     */
    public static Map<String, String> findConfigurationForBuildTask(ViewBuildResults viewBuildResults) {
        List<TaskDefinition> taskDefinitions = viewBuildResults.getImmutableBuild().getBuildDefinition().getTaskDefinitions();
        for (TaskDefinition taskDefinition : taskDefinitions) {
            if (StringUtils.startsWith(taskDefinition.getPluginKey(), ConstantValues.ARTIFACTORY_PLUGIN_KEY)) {
                return taskDefinition.getConfiguration();
            }
        }
        throw new IllegalStateException("This job has no Artifactory task.");
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

    public static String getSpecFromFile(File sourceCodeDirectory, String specFilePath) throws IOException {
        FileInputStream fis = null;
        try {
            Path path = Paths.get(specFilePath);
            File specFile = path.isAbsolute() ? path.toFile() : Paths.get(sourceCodeDirectory.getAbsolutePath(), specFilePath).toFile();
            fis = new FileInputStream(specFile);
            byte[] data = new byte[(int) specFile.length()];
            fis.read(data);
            return new String(data, "UTF-8");
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    public static ArtifactoryDependenciesClient getArtifactoryDependenciesClient(GenericContext genericContext, BuildParamsOverrideManager buildParamsOverrideManager, Logger log) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(genericContext.getSelectedServerId());
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        String username = overrideParam(serverConfigManager.substituteVariables(genericContext.getUsername()), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME, buildParamsOverrideManager);
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        String password = overrideParam(serverConfigManager.substituteVariables(genericContext.getPassword()), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD, buildParamsOverrideManager);
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());
        return new ArtifactoryDependenciesClient(serverUrl, username, password, new BuildInfoLog(log));
    }

    private static String overrideParam(String originalValue, String overrideKey, BuildParamsOverrideManager buildParamsOverrideManager) {
        String overriddenValue = buildParamsOverrideManager.getOverrideValue(overrideKey);
        return overriddenValue.isEmpty() ? originalValue : overriddenValue;
    }
}