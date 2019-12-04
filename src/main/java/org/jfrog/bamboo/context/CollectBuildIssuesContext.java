package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.jfrog.bamboo.configuration.ArtifactoryCollectBuildIssuesConfiguration;

import java.util.Map;
import java.util.Set;

public class CollectBuildIssuesContext {
    public static final String SERVER_ID_PARAM = "artifactory.task.collectBuildIssues." + AbstractBuildContext.SERVER_ID_PARAM;
    public static final String CONFIG_SOURCE_CHOICE = "artifactory.task.collectBuildIssues.config.source";
    private static final String USERNAME = "artifactory.task.collectBuildIssues.username";
    private static final String PASSWORD = "artifactory.task.collectBuildIssues.password";
    private static final String CONFIG_SOURCE_FILE = "artifactory.task.collectBuildIssues.config.source.file";
    private static final String CONFIG_SOURCE_TASK_CONFIGURATION = "artifactory.task.collectBuildIssues.config.source.taskConfiguration";

    private final Map<String, String> env;

    public CollectBuildIssuesContext(Map<String, String> env) {
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
        return (ArtifactoryCollectBuildIssuesConfiguration.CFG_CONFIG_SOURCE_TASK_CONFIGURATION.equals(env.get(CONFIG_SOURCE_CHOICE)));
    }

    public String getConfigFilePath() {
        return env.get(CONFIG_SOURCE_FILE);
    }

    public String getTaskConfigurationConfig() {
        return env.get(CONFIG_SOURCE_TASK_CONFIGURATION);
    }
}
