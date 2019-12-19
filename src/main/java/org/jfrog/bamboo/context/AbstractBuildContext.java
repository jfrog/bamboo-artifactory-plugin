package org.jfrog.bamboo.context;

import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;
import org.jfrog.bamboo.util.TaskDefinitionHelper;

import java.util.Arrays;
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
    public static final String CAPTURE_BUILD_INFO = "captureBuildInfo";
    public static final String INCLUDE_ENV_VARS_PARAM = "includeEnvVars";
    public static final String ENV_VARS_INCLUDE_PATTERNS = "envVarsIncludePatterns";
    public static final String ENV_VARS_EXCLUDE_PATTERNS = "envVarsExcludePatterns";
    public static final String TEST_CHECKED = "testChecked";
    public static final String TEST_RESULT_DIRECTORY = "testResultsDirectory";
    public static final String TEST_DIRECTORY_OPTION = "testDirectoryOption";
    public static final String ENVIRONMENT_VARIABLES = "environmentVariables";
    public static final String BUILD_INFO_AGGREGATION = "buildInfoAggregation";
    public static final String PUBLISH_ARTIFACTS_PARAM = "publishArtifacts";
    public static final String PUBLISH_MAVEN_DESCRIPTORS_PARAM = "publishMavenDescriptors";
    public static final String PUBLISH_IVY_DESCRIPTORS_PARAM = "publishIvyDescriptors";
    public static final String USE_M2_COMPATIBLE_PATTERNS_PARAM = "useM2CompatiblePatterns";
    public static final String IVY_PATTERN_PARAM = "ivyPattern";
    public static final String ARTIFACT_PATTERN_PARAM = "artifactPattern";
    public static final String PUBLISH_INCLUDE_PATTERNS_PARAM = "publishIncludePatterns";
    public static final String PUBLISH_EXCLUDE_PATTERNS_PARAM = "publishExcludePatterns";
    public static final String FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM = "filterExcludedArtifactsFromBuild";
    public static final String ARTIFACT_SPECS_PARAM = "artifactSpecs";
    public static final String NO_RESOLUTION_REPO_KEY_CONFIGURED = "noResolutionRepoKeyConfigured";
    public static final String NO_PUBLISHING_REPO_KEY_CONFIGURED = "noPublishingRepoKeyConfigured";
    public static final String JDK = "buildJdk";
    public static final String EXECUTABLE = "executable";
    public static final String BASE_URL = "baseUrl";

    // release management props.
    public static final String ENABLE_RELEASE_MANAGEMENT = "enableReleaseManagement";
    public static final String ACTIVATE_RELEASE_MANAGEMENT = "activateReleaseManagement";
    public static final String VCS_TAG_BASE = "vcsTagBase";
    public static final String GIT_RELEASE_BRANCH = "gitReleaseBranch";
    public static final String ALTERNATIVE_TASKS = "alternativeTasks";
    public static final String RELEASE_PROPS = "releaseProps";
    public static final String NEXT_INTEG_PROPS = "nextIntegProps";
    public static final String VCS_TYPE = "type";
    public static final String GIT_URL = "git.url";
    public static final String GIT_AUTHENTICATION_TYPE = "git.authenticationType";
    public static final String GIT_USERNAME = "git.username";
    public static final String GIT_PASSWORD = "git.password";
    public static final String GIT_SSH_KEY = "git.ssh.key";
    public static final String GIT_PASSPHRASE = "git.ssh.passphrase";
    public static final String PERFORCE_PORT = "p4.port";
    public static final String PERFORCE_CLIENT = "p4.client";
    public static final String PERFORCE_DEPOT = "p4.depot";
    public static final String PERFORCE_USERNAME = "p4.username";
    public static final String PERFORCE_PASSWORD = "p4.password";
    public static final String VCS_PREFIX = "artifactory.vcs.";
    public static final String ENV_VARS_TO_EXCLUDE = "*password*,*secret*,*security*,*key*";

    public final ReleaseManagementContext releaseManagementContext = new ReleaseManagementContext();
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

    public static List<String> getVcsFieldsToCopy() {
        return Arrays.asList(
                VCS_PREFIX + VCS_TYPE,
                VCS_PREFIX + GIT_AUTHENTICATION_TYPE,
                VCS_PREFIX + GIT_USERNAME,
                VCS_PREFIX + GIT_PASSWORD,
                VCS_PREFIX + GIT_URL,
                VCS_PREFIX + GIT_PASSPHRASE,
                VCS_PREFIX + GIT_SSH_KEY,
                VCS_PREFIX + PERFORCE_CLIENT,
                VCS_PREFIX + PERFORCE_PORT,
                VCS_PREFIX + PERFORCE_DEPOT,
                VCS_PREFIX + PERFORCE_USERNAME,
                VCS_PREFIX + PERFORCE_PASSWORD
        );
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

    // Value is used by tasks created prior to plugin version 2.7.0.
    public boolean isPublishBuildInfo() {
        return Boolean.parseBoolean(env.get(PUBLISH_BUILD_INFO_PARAM));
    }

    // Value is sed by tasks created from plugin version 2.7.0.
    public boolean isCaptureBuildInfo() {
        return Boolean.parseBoolean(env.get(CAPTURE_BUILD_INFO));
    }

    public boolean isIncludeEnvVars() {
        return Boolean.parseBoolean(env.get(INCLUDE_ENV_VARS_PARAM));
    }

    public String getEnvVarsIncludePatterns() {
        return env.get(ENV_VARS_INCLUDE_PATTERNS);
    }

    public String getEnvVarsExcludePatterns() {
        return env.get(ENV_VARS_EXCLUDE_PATTERNS);
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

    public boolean isFilterExcludedArtifactsFromBuild() {
        return Boolean.parseBoolean(env.get(prefix + FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM));
    }

    public String getResolutionRepo() {
        return env.get(prefix + RESOLUTION_REPO_PARAM);
    }

    public String getArtifactSpecs() {
        return env.get(prefix + ARTIFACT_SPECS_PARAM);
    }


    public void resetDeployerContextToDefault() {
        env.put(prefix + SERVER_ID_PARAM, "-1");
        env.put(prefix + DEPLOYER_USERNAME_PARAM, "");
        env.put(prefix + DEPLOYER_PASSWORD_PARAM, "");
        env.put(USE_M2_COMPATIBLE_PATTERNS_PARAM, "true");
        env.put(prefix + IVY_PATTERN_PARAM, "");
        env.put(prefix + ARTIFACT_PATTERN_PARAM, "");
        env.put(prefix + FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM, "false");
        env.put(PUBLISH_ARTIFACTS_PARAM, "false");
        env.put(ENABLE_RELEASE_MANAGEMENT, "false");
        env.put(ENV_VARS_EXCLUDE_PATTERNS, ENV_VARS_TO_EXCLUDE);
    }

    public void resetPublishBuildInfo() {
        env.put(PUBLISH_BUILD_INFO_PARAM, "false");
    }

    public void resetResolverContextToDefault() {
        env.put(prefix + RESOLUTION_REPO_PARAM, "");
    }

    public class ReleaseManagementContext {
        public static final String CREATE_VCS_TAG = "createVcsTag";
        public static final String USE_RELEASE_BRANCH = "useReleaseBranch";
        public static final String RELEASE_BRANCH = "releaseBranch";
        public static final String STAGING_COMMENT = "stagingComment";
        public static final String TAG_URL = "tagUrl";
        public static final String TAG_COMMENT = "tagComment";
        public static final String NEXT_DEVELOPMENT_COMMENT = "nextDevelopmentComment";
        public static final String REPO_KEY = "release.management.repoKey";

        public boolean isActivateReleaseManagement() {
            return Boolean.parseBoolean(env.get(ACTIVATE_RELEASE_MANAGEMENT));
        }

        public void setActivateReleaseManagement(boolean value) {
            env.put(ACTIVATE_RELEASE_MANAGEMENT, String.valueOf(value));
        }

        public String getTagUrl() {
            return env.get(TAG_URL);
        }

        public String getRepoKey() {
            return env.get(REPO_KEY);
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

    public boolean shouldAggregateBuildInfo(@NotNull TaskContext taskContext, long serverId) {
        if (isCaptureBuildInfo()) {
            // Value of CAPTURE_BUILD_INFO is 'true' by default.
            // In case of no server-id provided, shouldn't collect build-info even though the value remains 'true'.
            if (serverId != -1) {
                return true;
            }
            return false;
        }
        if (isPublishBuildInfo()) {
            // Task was created prior to version 2.7.0, and set to publish build-info.
            // If the plan contains a publish build info task, disable task publishing and use build aggregation instead.
            List<? extends TaskDefinition> taskDefinitions = taskContext.getBuildContext().getRuntimeTaskDefinitions();
            if (TaskDefinitionHelper.isBuildPublishTaskExists(taskDefinitions)) {
                resetPublishBuildInfo();
                return true;
            }
        }
        return false;
    }
}
