package org.jfrog.bamboo.context;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.bintray.PushToBintrayContext;

import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class GenericContext {
    public static final String PREFIX = "builder.artifactoryGenericBuilder.";
    public static final String SERVER_ID_PARAM = AbstractBuildContext.SERVER_ID_PARAM;
    public static final String REPO_KEY = "builder.artifactoryGenericBuilder.deployableRepo";
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
    public static final String ENABLE_BINTRAY_CONFIGURATION = "bintrayConfiguration";
    public static final String SIGN_METHOD_MAP_KEY = "signMethods";

    public static final Map<String, String> SIGN_METHOD_MAP = ImmutableMap.of(
            "false", "Don't Sign", "true", "Sign");

    private final Map<String, String> env;

    public GenericContext(Map<String, String> env) {
        this.env = env;
    }

    public static Set<String> getFieldsToCopy() {
        Set<String> fieldsToCopy = Sets.newHashSet(PREFIX + SERVER_ID_PARAM, REPO_KEY, REPO_RESOLVE_KEY, USERNAME, PASSWORD, DEPLOY_PATTERN, ARTIFACT_SPECS,
                RESOLVE_PATTERN, PUBLISH_BUILD_INFO, INCLUDE_ENV_VARS, ENV_VARS_INCLUDE_PATTERNS, ENV_VARS_EXCLUDE_PATTERNS, ENABLE_BINTRAY_CONFIGURATION);
        fieldsToCopy.addAll(PushToBintrayContext.bintrayFields);
        return fieldsToCopy;
    }

    public long getSelectedServerId() {
        String serverId = env.get(PREFIX + SERVER_ID_PARAM);
        if (StringUtils.isBlank(serverId)) {
            // In version 1.8.1 the key containing the Artifactory Server ID was changed
            // in the Generic Resolve and Deploy configurations.
            // The following line tries to get the server using the old key.
            serverId = env.get("artifactory.generic.artifactoryServerId");
        }
        if (StringUtils.isBlank(serverId)) {
            return -1;
        }
        return Long.parseLong(serverId);
    }

    public String getRepoKey() {
        String key = env.get(REPO_KEY);
        if (StringUtils.isBlank(key)) {
            // Compatibility with 1.8.0
            return env.get("artifactory.generic.deployableRepo");
        }
        return key;
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
