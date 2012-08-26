package org.jfrog.bamboo.context;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Container object for common build environment properties that is based on the configuration's namespace. Each context
 * has its own unique prefix that the property is appended to.
 *
 * @author Tomer Cohen
 */
public abstract class AbstractBuildContext {

    public static final String SERVER_ID_PARAM = "artifactoryServerId";
    public static final String RESOLUTION_REPO_PARAM = "resolutionRepo";
    public static final String PUBLISHING_REPO_PARAM = "publishingRepo";
    public static final String DEPLOYER_USERNAME_PARAM = "deployerUsername";
    public static final String DEPLOYER_PASSWORD_PARAM = "deployerPassword";
    public static final String USE_ARTIFACTORY_GRADLE_PLUGIN = "useArtifactoryGradlePlugin";
    public static final String PUBLISH_BUILD_INFO_PARAM = "publishBuildInfo";
    public static final String INCLUDE_ENV_VARS_PARAM = "includeEnvVars";
    public static final String RUN_LICENSE_CHECKS = "runLicenseChecks";
    public static final String LICENSE_VIOLATION_RECIPIENTS = "licenseViolationRecipients";
    public static final String LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES = "limitChecksToScopes";
    public static final String INCLUDE_PUBLISHED_ARTIFACTS = "includePublishedArtifacts";
    public static final String DISABLE_AUTOMATIC_LICENSE_DISCOVERY = "disableAutoLicenseDiscovery";
    public static final String TEST_CHECKED = "testChecked";
    public static final String TEST_RESULT_DIRECTORY = "testResultsDirectory";
    public static final String TEST_DIRECTORY_OPTION = "testDirectoryOption";
    public static final String ENVIRONMENT_VARIABLES = "environmentVariables";

    public static final String PUBLISH_ARTIFACTS_PARAM = "publishArtifacts";
    public static final String PUBLISH_MAVEN_DESCRIPTORS_PARAM = "publishMavenDescriptors";
    public static final String PUBLISH_IVY_DESCRIPTORS_PARAM = "publishIvyDescriptors";
    public static final String USE_M2_COMPATIBLE_PATTERNS_PARAM = "useM2CompatiblePatterns";
    public static final String IVY_PATTERN_PARAM = "ivyPattern";
    public static final String ARTIFACT_PATTERN_PARAM = "artifactPattern";
    public static final String PUBLISH_INCLUDE_PATTERNS_PARAM = "publishIncludePatterns";
    public static final String PUBLISH_EXCLUDE_PATTERNS_PARAM = "publishExcludePatterns";
    public static final String ARTIFACT_SPECS_PARAM = "artifactSpecs";
    public static final String NO_RESOLUTION_REPO_KEY_CONFIGURED = "noResolutionRepoKeyConfigured";
    public static final String NO_PUBLISHING_REPO_KEY_CONFIGURED = "noPublishingRepoKeyConfigured";
    public static final String JDK = "buildJdk";
    public static final String EXECUTABLE = "executable";
    public static final String BASE_URL = "baseUrl";
    public static final String BUILD_TIMESTAMP = "buildTimeStamp";

    public final ReleaseManagementContext releaseManagementContext = new ReleaseManagementContext();

    // release management props.
    public static final String ENABLE_RELEASE_MANAGEMENT = "enableReleaseManagement";
    public static final String ACTIVATE_RELEASE_MANAGEMENT = "activateReleaseManagement";
    public static final String VCS_TAG_BASE = "vcsTagBase";
    public static final String GIT_RELEASE_BRANCH = "gitReleaseBranch";
    public static final String ALTERNATIVE_TASKS = "alternativeTasks";
    public static final String RELEASE_PROPS = "releaseProps";
    public static final String NEXT_INTEG_PROPS = "nextIntegProps";
    private final String prefix;
    protected final Map<String, String> env;

    public AbstractBuildContext(String prefix, Map<String, String> env) {
        this.prefix = prefix;
        this.env = env;
    }

    public static AbstractBuildContext createContextFromMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException("No empty map allowed");
        }
        String value = getBuilderValue(map);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (StringUtils.startsWith(value, GradleBuildContext.PREFIX)) {
            return new GradleBuildContext(sanitizeEntries(map));
        } else if (StringUtils.startsWith(value, Maven3BuildContext.PREFIX)) {
            return new Maven3BuildContext(sanitizeEntries(map));
        }
        return null;
    }

    private static String getBuilderValue(Map<String, String> confMap) {
        for (Map.Entry<String, String> entry : confMap.entrySet()) {
            if (StringUtils.startsWith(entry.getKey(), "builder.")) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Map<String, String> sanitizeEntries(Map<String, String> confMap) {
        Map<String, String> result = Maps.newHashMap();
        for (Map.Entry<String, String> entry : confMap.entrySet()) {
            if (StringUtils.isBlank(entry.getValue())) {
                result.put(entry.getKey(), "");
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public long getArtifactoryServerId() {
        String serverId = env.get(prefix + SERVER_ID_PARAM);
        if (StringUtils.isBlank(serverId)) {
            return -1;
        }
        return Long.parseLong(serverId);
    }

    public String getBaseUrl() {
        return env.get(BASE_URL);
    }

    public long getBuildTimestamp() {
        String isoTimeStamp = env.get(BUILD_TIMESTAMP);
        return ((Date) (new ISO8601DateConverter().fromString(isoTimeStamp))).getTime();
    }

    public String getEnvironmentVariables() {
        return env.get(prefix + ENVIRONMENT_VARIABLES);
    }

    public String getExecutable() {
        return env.get(prefix + EXECUTABLE);
    }

    public boolean isTestChecked() {
        return Boolean.parseBoolean(env.get(TEST_CHECKED));
    }

    public String getTestDirectory() {
        return env.get(prefix + TEST_RESULT_DIRECTORY);
    }

    public String getTestDirectoryOption() {
        return env.get(TEST_DIRECTORY_OPTION);
    }

    public String getPublishingRepo() {
        return env.get(prefix + PUBLISHING_REPO_PARAM);
    }

    public String getDeployerUsername() {
        return env.get(prefix + DEPLOYER_USERNAME_PARAM);
    }

    public String getDeployerPassword() {
        return env.get(prefix + DEPLOYER_PASSWORD_PARAM);
    }

    public String getJdkLabel() {
        return env.get(prefix + JDK);
    }

    public boolean useArtifactoryGradlePlugin() {
        return Boolean.parseBoolean(env.get(prefix + USE_ARTIFACTORY_GRADLE_PLUGIN));
    }

    public boolean isPublishBuildInfo() {
        return Boolean.parseBoolean(env.get(PUBLISH_BUILD_INFO_PARAM));
    }

    public boolean isIncludeEnvVars() {
        return Boolean.parseBoolean(env.get(prefix + INCLUDE_ENV_VARS_PARAM));
    }

    public boolean isRunLicenseChecks() {
        return Boolean.parseBoolean(env.get(RUN_LICENSE_CHECKS));
    }

    public String getLicenseViolationRecipients() {
        return env.get(prefix + LICENSE_VIOLATION_RECIPIENTS);
    }

    public String getScopes() {
        return env.get(prefix + LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES);
    }

    public boolean isIncludePublishedArtifacts() {
        return Boolean.parseBoolean(env.get(prefix + INCLUDE_PUBLISHED_ARTIFACTS));
    }

    public boolean isDisableAutomaticLicenseDiscovery() {
        return Boolean.parseBoolean(env.get(prefix + DISABLE_AUTOMATIC_LICENSE_DISCOVERY));
    }

    public boolean isPublishArtifacts() {
        return Boolean.parseBoolean(env.get(PUBLISH_ARTIFACTS_PARAM));
    }

    public boolean isPublishMavenDescriptors() {
        return Boolean.parseBoolean(env.get(prefix + PUBLISH_MAVEN_DESCRIPTORS_PARAM));
    }

    public boolean isPublishIvyDescriptors() {
        return Boolean.parseBoolean(env.get(prefix + PUBLISH_IVY_DESCRIPTORS_PARAM));
    }

    public boolean isMaven2Compatible() {
        return Boolean.parseBoolean(env.get(USE_M2_COMPATIBLE_PATTERNS_PARAM));
    }

    public String getIvyPattern() {
        return env.get(prefix + IVY_PATTERN_PARAM);
    }

    public String getArtifactPattern() {
        return env.get(prefix + ARTIFACT_PATTERN_PARAM);
    }

    public String getIncludePattern() {
        return env.get(prefix + PUBLISH_INCLUDE_PATTERNS_PARAM);
    }

    public String getExcludePattern() {
        return env.get(prefix + PUBLISH_EXCLUDE_PATTERNS_PARAM);
    }

    public String getResolutionRepo() {
        return env.get(prefix + RESOLUTION_REPO_PARAM);
    }

    public void resetContextToDefault() {
        env.put(prefix + SERVER_ID_PARAM, "-1");
        env.put(prefix + RESOLUTION_REPO_PARAM, "");
        env.put(prefix + DEPLOYER_USERNAME_PARAM, "");
        env.put(prefix + DEPLOYER_PASSWORD_PARAM, "");
        env.put(USE_M2_COMPATIBLE_PATTERNS_PARAM, "true");
        env.put(prefix + IVY_PATTERN_PARAM, "");
        env.put(prefix + ARTIFACT_PATTERN_PARAM, "");
        env.put(RUN_LICENSE_CHECKS, "false");
        env.put(prefix + LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES, "");
        env.put(prefix + INCLUDE_PUBLISHED_ARTIFACTS, "false");
        env.put(prefix + DISABLE_AUTOMATIC_LICENSE_DISCOVERY, "false");
        env.put(PUBLISH_ARTIFACTS_PARAM, "false");
        env.put(ENABLE_RELEASE_MANAGEMENT, "false");
    }

    public class ReleaseManagementContext {
        public static final String CREATE_VCS_TAG = "createVcsTag";
        public static final String USE_RELEASE_BRANCH = "useReleaseBranch";
        public static final String RELEASE_BRANCH = "releaseBranch";
        public static final String STAGING_COMMENT = "stagingComment";
        public static final String TAG_URL = "tagUrl";
        public static final String TAG_COMMENT = "tagComment";
        public static final String NEXT_DEVELOPMENT_COMMENT = "nextDevelopmentComment";
        public static final String RELEASE_REPO_KEY = "releaseRepoKey";

        public boolean isActivateReleaseManagement() {
            return Boolean.parseBoolean(env.get(ACTIVATE_RELEASE_MANAGEMENT));
        }

        public void setActivateReleaseManagement(boolean value) {
            env.put(ACTIVATE_RELEASE_MANAGEMENT, String.valueOf(value));
        }

        public String getTagUrl() {
            return env.get(TAG_URL);
        }

        public String getReleaseRepoKey() {
            return env.get(RELEASE_REPO_KEY);
        }

        public String getStagingComment() {
            return env.get(STAGING_COMMENT);
        }

        public boolean isUseReleaseBranch() {
            return Boolean.parseBoolean(env.get(USE_RELEASE_BRANCH));
        }

        public String getReleaseBranch() {
            return env.get(RELEASE_BRANCH);
        }

        public String getTagComment() {
            return env.get(TAG_COMMENT);
        }

        public boolean isCreateVcsTag() {
            return Boolean.parseBoolean(env.get(CREATE_VCS_TAG));
        }

        public String getNextDevelopmentComment() {
            return env.get(NEXT_DEVELOPMENT_COMMENT);
        }

        public List<ModuleVersionHolder> filterPropsForRelease(Map<String, String> props) {
            List<ModuleVersionHolder> result = Lists.newArrayList();
            String releaseProps = env.get(prefix + RELEASE_PROPS);
            if (StringUtils.isNotBlank(releaseProps)) {
                List<String> split = Lists.newArrayList(splitAndTrim(releaseProps));
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    if (Iterables.contains(split, entry.getKey())) {
                        result.add(new ModuleVersionHolder(entry.getKey(), entry.getValue(), true));
                    }
                }
            }
            String nextIntegProps = env.get(prefix + NEXT_INTEG_PROPS);
            if (StringUtils.isNotBlank(nextIntegProps)) {
                List<String> split = Lists.newArrayList(splitAndTrim(nextIntegProps));
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    final String propertyKey = entry.getKey();
                    if (Iterables.contains(split, propertyKey)) {
                        ModuleVersionHolder existingReleaseProp;
                        try {
                            existingReleaseProp = Iterables.find(result, new Predicate<ModuleVersionHolder>() {
                                @Override
                                public boolean apply(ModuleVersionHolder holder) {
                                    return (holder != null) && holder.getKey().equals(propertyKey);
                                }
                            });
                            existingReleaseProp.setReleaseProp(false);
                        } catch (NoSuchElementException e) {
                            result.add(new ModuleVersionHolder(propertyKey, entry.getValue(), false));
                        }
                    }
                }
            }
            return result;
        }

        public boolean isReleaseMgmtEnabled() {
            return Boolean.parseBoolean(env.get(ENABLE_RELEASE_MANAGEMENT));
        }

        public String getVcsTagBase() {
            return env.get(prefix + VCS_TAG_BASE);
        }

        public String getGitReleaseBranch() {
            return env.get(prefix + GIT_RELEASE_BRANCH);
        }

        public String getAlternativeTasks() {
            return env.get(prefix + ALTERNATIVE_TASKS);
        }

        public String getReleaseProps() {
            return env.get(prefix + RELEASE_PROPS);
        }

        public String getNextIntegProps() {
            return env.get(prefix + NEXT_INTEG_PROPS);
        }
    }

    private List<String> splitAndTrim(String releaseProps) {
        List<String> tokens = Lists.newArrayList();
        for (String token : StringUtils.split(releaseProps, ",")) {
            if (StringUtils.isNotBlank(token)) {
                tokens.add(token.trim());
            }
        }

        return tokens;
    }
}
