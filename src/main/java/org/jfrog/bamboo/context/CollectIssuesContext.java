package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.jfrog.bamboo.configuration.ArtifactoryCollectIssuesConfiguration;

import java.util.Map;
import java.util.Set;

public class CollectIssuesContext {
    public static final String PREFIX = "artifactory.task.collectIssues.";
    public static final String SERVER_ID_PARAM = PREFIX + AbstractBuildContext.SERVER_ID_PARAM;
    public static final String CONFIG_SOURCE_CHOICE = PREFIX + "configSourceChoice";
    private static final String USERNAME = PREFIX + "username";
    private static final String PASSWORD = PREFIX + "password";
    private static final String CONFIG_SOURCE_FILE = PREFIX + "configSource.file";
    private static final String CONFIG_SOURCE_TASK_CONFIGURATION = PREFIX + "configSource.taskConfiguration";

    private final Map<String, String> env;

    public CollectIssuesContext(Map<String, String> env) {
        this.env = env;
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD,
                CONFIG_SOURCE_CHOICE, CONFIG_SOURCE_FILE, CONFIG_SOURCE_TASK_CONFIGURATION);
    }

    public long getArtifactoryServerId() {
        return Long.parseLong(env.get(SERVER_ID_PARAM));
    }

    public String getUsername() {
        return env.get(USERNAME);
    }

    public String getPassword() {
        return env.get(PASSWORD);
    }

    public boolean isConfigSourceTaskConfiguration() {
        return (ArtifactoryCollectIssuesConfiguration.CFG_CONFIG_SOURCE_TASK_CONFIGURATION.equals(env.get(CONFIG_SOURCE_CHOICE)));
    }

    public String getConfigFilePath() {
        return env.get(CONFIG_SOURCE_FILE);
    }

    public String getTaskConfigurationConfig() {
        return env.get(CONFIG_SOURCE_TASK_CONFIGURATION);
    }
}
