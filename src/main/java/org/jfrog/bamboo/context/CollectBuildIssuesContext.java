package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.jfrog.bamboo.configuration.ArtifactoryCollectBuildIssuesConfiguration;

import java.util.Map;
import java.util.Set;

public class CollectBuildIssuesContext extends ArtifactoryBuildContext {
    private static final String PREFIX = "artifactory.task.collectBuildIssues.";
    public static final String SERVER_ID_PARAM = PREFIX + PackageManagersContext.SERVER_ID_PARAM;
    public static final String CONFIG_SOURCE_CHOICE = PREFIX + "config.source";
    private static final String USERNAME = PREFIX + USERNAME_PARAM;
    private static final String PASSWORD = PREFIX + PASSWORD_PARAM;
    private static final String CONFIG_SOURCE_FILE = PREFIX + "config.source.file";
    private static final String CONFIG_SOURCE_TASK_CONFIGURATION = PREFIX + "config.source.taskConfiguration";

    public CollectBuildIssuesContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD, CONFIG_SOURCE_CHOICE, CONFIG_SOURCE_FILE,
                CONFIG_SOURCE_TASK_CONFIGURATION, BUILD_NAME, BUILD_NUMBER,
                DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS);
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
