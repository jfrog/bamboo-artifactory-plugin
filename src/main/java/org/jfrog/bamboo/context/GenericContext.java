package org.jfrog.bamboo.context;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.bamboo.configuration.AbstractArtifactoryConfiguration;
import org.jfrog.build.api.util.Log;

import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.configuration.AbstractArtifactoryConfiguration.CVG_CRED_SHARED_CREDENTIALS;
import static org.jfrog.bamboo.configuration.AbstractArtifactoryConfiguration.CVG_CRED_USERNAME_PASSWORD;
import static org.jfrog.bamboo.context.AbstractBuildContext.*;
import static org.jfrog.bamboo.release.provider.SharedCredentialsDataProvider.*;

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

    protected final Map<String, String> env;

    public GenericContext(Map<String, String> env) {
        this.env = env;
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(PREFIX + SERVER_ID_PARAM, REPO_KEY, REPO_RESOLVE_KEY,
                RESOLVER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, USERNAME, PASSWORD, RESOLVER_SHARED_CREDENTIALS,
                DEPLOYER_SHARED_CREDENTIALS, DEPLOY_PATTERN, SPEC_SOURCE_JOB_CONFIGURATION, BUILD_INFO_AGGREGATION,
                CAPTURE_BUILD_INFO, SPEC_SOURCE_FILE, ARTIFACT_SPECS, RESOLVE_PATTERN, PUBLISH_BUILD_INFO,
                INCLUDE_ENV_VARS, ENV_VARS_INCLUDE_PATTERNS, ENV_VARS_EXCLUDE_PATTERNS, USE_SPECS_CHOICE, SPEC_SOURCE_CHOICE);
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

    public String getResolverOverrideCredentialsChoice() {
        return env.get(RESOLVER_OVERRIDE_CREDENTIALS_CHOICE);
    }

    public String getDeployerOverrideCredentialsChoice() {
        return env.get(DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE);
    }

    public String getUsername() {
        return env.get(USERNAME);
    }

    public String getPassword() {
        return env.get(PASSWORD);
    }

    public String getResolverSharedCredentials() {
        return env.get(RESOLVER_SHARED_CREDENTIALS);
    }

    public String getDeployerSharedCredentials() {
        return env.get(DEPLOYER_SHARED_CREDENTIALS);
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

    public String getOverriddenUsername(Map<String, String> runtimeTaskContext, Log log, boolean deployer) {
        switch (StringUtils.defaultString(deployer ? getDeployerOverrideCredentialsChoice() : getResolverOverrideCredentialsChoice())) {
            case CVG_CRED_USERNAME_PASSWORD:
                log.info("Using Artifactory username '" + getUsername() + "' configured in job");
                return getUsername();
            case CVG_CRED_SHARED_CREDENTIALS:
                String username = runtimeTaskContext.get(deployer ? DEPLOYER_SHARED_CREDENTIALS_USER : RESOLVER_SHARED_CREDENTIALS_USER);
                String credentialsId = deployer ? getDeployerSharedCredentials() : getResolverSharedCredentials();
                log.info("Using Artifactory username '" + username + "' configured in credentials ID '" + credentialsId + "'");
                return username;
            default:
                return "";
        }
    }

    public String getOverriddenPassword(Map<String, String> runtimeTaskContext, Log log, boolean deployer) {
        switch (StringUtils.defaultString(deployer ? getDeployerOverrideCredentialsChoice() : getResolverOverrideCredentialsChoice())) {
            case CVG_CRED_USERNAME_PASSWORD:
                log.info("Using Artifactory password configured in job");
                return getPassword();
            case CVG_CRED_SHARED_CREDENTIALS:
                String credentialsId = deployer ? getDeployerSharedCredentials() : getResolverSharedCredentials();
                log.info("Using Artifactory password configured in credentials ID '" + credentialsId + "'");
                return runtimeTaskContext.get(deployer ? DEPLOYER_SHARED_CREDENTIALS_PASSWORD : RESOLVER_SHARED_CREDENTIALS_PASSWORD);
            default:
                return "";
        }
    }
}
