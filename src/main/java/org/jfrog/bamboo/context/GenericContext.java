package org.jfrog.bamboo.context;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.configuration.AbstractArtifactoryConfiguration;

import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.context.AbstractBuildContext.CAPTURE_BUILD_INFO;
import static org.jfrog.bamboo.context.AbstractBuildContext.BUILD_INFO_AGGREGATION;

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
    public static final String USE_SPECS_CHOICE = "artifactory.generic.useSpecsChoice";
    public static final String SPEC_SOURCE_CHOICE = "artifactory.generic.specSourceChoice";
    public static final String SPEC_SOURCE_JOB_CONFIGURATION = "artifactory.generic.jobConfiguration";
    public static final String SPEC_SOURCE_FILE = "artifactory.generic.file";
    public static final String RESOLVE_PATTERN = "artifactory.generic.resolvePattern";
    public static final String PUBLISH_BUILD_INFO = "artifactory.generic.publishBuildInfo";
    public static final String INCLUDE_ENV_VARS = "artifactory.generic.includeEnvVars";
    public static final String ARTIFACT_SPECS = "artifactory.generic.artifactSpecs";
    public static final String ENV_VARS_INCLUDE_PATTERNS = "artifactory.generic.envVarsIncludePatterns";
    public static final String ENV_VARS_EXCLUDE_PATTERNS = "artifactory.generic.envVarsExcludePatterns";
    public static final String SIGN_METHOD_MAP_KEY = "signMethods";

    public static final Map<String, String> SIGN_METHOD_MAP = ImmutableMap.of(
            "false", "Don't Sign", "true", "Sign");

    private final Map<String, String> env;

    public GenericContext(Map<String, String> env) {
        this.env = env;
    }

    public static Set<String> getFieldsToCopy() {
        Set<String> fieldsToCopy = Sets.newHashSet(PREFIX + SERVER_ID_PARAM, REPO_KEY, REPO_RESOLVE_KEY, USERNAME, PASSWORD, DEPLOY_PATTERN, SPEC_SOURCE_JOB_CONFIGURATION, BUILD_INFO_AGGREGATION, CAPTURE_BUILD_INFO,
                SPEC_SOURCE_FILE, ARTIFACT_SPECS, RESOLVE_PATTERN, PUBLISH_BUILD_INFO, INCLUDE_ENV_VARS, ENV_VARS_INCLUDE_PATTERNS, ENV_VARS_EXCLUDE_PATTERNS,
                USE_SPECS_CHOICE, SPEC_SOURCE_CHOICE);
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

    public long getArtifactoryServerId() {
        return 0;
    }

    public String getDeployPattern() {
        return env.get(DEPLOY_PATTERN);
    }

    public boolean isUseFileSpecs() {
        return (AbstractArtifactoryConfiguration.CFG_FILE_SPECS.equals(env.get(USE_SPECS_CHOICE)));
    }

    public boolean isFileSpecInJobConfiguration() {
        return (AbstractArtifactoryConfiguration.CFG_SPEC_SOURCE_JOB_CONFIGURATION.equals(env.get(SPEC_SOURCE_CHOICE)));
    }

    public String getJobConfigurationSpec() {
        return env.get(SPEC_SOURCE_JOB_CONFIGURATION);
    }

    public String getFilePathSpec() {
        return env.get(SPEC_SOURCE_FILE);
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

    public boolean isCaptureBuildInfo() {
        return Boolean.parseBoolean(env.get(CAPTURE_BUILD_INFO));
    }

    public String getEnvVarsExcludePatterns() {
        return env.get(ENV_VARS_EXCLUDE_PATTERNS);
    }

    public String getArtifactSpecs() {
        return env.get(ARTIFACT_SPECS);
    }
}
