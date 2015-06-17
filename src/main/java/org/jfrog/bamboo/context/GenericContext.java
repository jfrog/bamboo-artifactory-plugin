package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class GenericContext {
    public static final String SELECTED_SERVER_ID = "artifactory.generic.artifactoryServerId";
    public static final String REPO_KEY = "artifactory.generic.deployableRepo";
    public static final String REPO_RESOLVE_KEY = "artifactory.generic.resolveRepo";
    public static final String USERNAME = "artifactory.generic.username";
    public static final String PASSWORD = "artifactory.generic.password";
    public static final String DEPLOY_PATTERN = "artifactory.generic.deployPattern";
    public static final String RESOLVE_PATTERN = "artifactory.generic.resolvePattern";
    public static final String PUBLISH_BUILD_INFO = "artifactory.generic.publishBuildInfo";
    public static final String INCLUDE_ENV_VARS = "artifactory.generic.includeEnvVars";
    public static final String ARTIFACT_SPECS = "artifactory.generic.artifactSpecs";
    public static final String ENV_VARS_INCLUDE_PATTERNS = "artifactory.generic.envVarsIncludePatterns";
    public static final String ENV_VARS_EXCLUDE_PATTERNS = "artifactory.generic.envVarsExcludePatterns";

    private final Map<String, String> env;

    public GenericContext(Map<String, String> env) {
        this.env = env;
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SELECTED_SERVER_ID, REPO_KEY, REPO_RESOLVE_KEY, USERNAME, PASSWORD, DEPLOY_PATTERN, ARTIFACT_SPECS,
                RESOLVE_PATTERN, PUBLISH_BUILD_INFO, INCLUDE_ENV_VARS, ENV_VARS_INCLUDE_PATTERNS, ENV_VARS_EXCLUDE_PATTERNS);
    }

    public long getSelectedServerId() {
        String serverId = env.get(SELECTED_SERVER_ID);
        if (StringUtils.isBlank(serverId)) {
            return -1;
        }
        return Long.parseLong(serverId);
    }

    public String getRepoKey() {
        return env.get(REPO_KEY);
    }

    public String getUsername() {
        return env.get(USERNAME);
    }

    public String getPassword() {
        return env.get(PASSWORD);
    }

    public String getDeployPattern() {
        return env.get(DEPLOY_PATTERN);
    }

    public String getResolvePattern() {
        return env.get(RESOLVE_PATTERN);
    }

    public boolean isPublishBuildInfo() {
        return Boolean.parseBoolean(env.get(PUBLISH_BUILD_INFO));
    }

    public boolean isIncludeEnvVars() {
        return Boolean.parseBoolean(env.get(INCLUDE_ENV_VARS));
    }

    public String getEnvVarsIncludePatterns() {
        return env.get(ENV_VARS_INCLUDE_PATTERNS);
    }

    public String getEnvVarsExcludePatterns() {
        return env.get(ENV_VARS_EXCLUDE_PATTERNS);
    }

    public String getArtifactSpecs() {
        return env.get(ARTIFACT_SPECS);
    }
}
