package org.jfrog.bamboo.context;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.bintray.PushToBintrayContext;

import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class Maven3BuildContext extends AbstractBuildContext {
    public static final String PREFIX = "builder.artifactoryMaven3Builder.";
    public static final String DEPLOYABLE_REPO_KEY = "deployableRepo";
    public static final String PUBLISH_ARTIFACTS = "deployMavenArtifacts";
    public static final String RECORD_ALL_DEPENDENCIES = "recordAllDependencies";
    public static final String INCLUDE_PATTERN = "deployIncludePatterns";
    public static final String EXCLUDE_PATTERN = "deployExcludePatterns";
    public static final String GOALS = "goal";
    public static final String MAVEN_OPTS = "mavenOpts";
    public static final String PROJECT_FILE = "projectFile";
    public static final String ADDITIONAL_MAVEN_PARAMS = "additionalMavenParams";
    public static final String WORKING_SUB_DIRECTORY = "workingSubDirectory";
    public static final String RESOLVE_FROM_ARTIFACTORY = "resolveFromArtifacts";
    public static final String RESOLUTION_SERVER_ID = "resolutionArtifactoryServerId";
    public static final String RESOLVER_USER_NAME = "resolverUsername";
    public static final String RESOLVER_PASSWORD = "resolverPassword";

    public Maven3BuildContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public String getGoals() {
        return env.get(PREFIX + GOALS);
    }

    public String getProjectFile() {
        return env.get(PREFIX + PROJECT_FILE);
    }

    public String getAdditionalMavenParams() {
        return env.get(PREFIX + ADDITIONAL_MAVEN_PARAMS);
    }

    @Override
    public String getPublishingRepo() {
        return env.get(PREFIX + DEPLOYABLE_REPO_KEY);
    }

    @Override
    public boolean isPublishArtifacts() {
        return Boolean.parseBoolean(env.get(PUBLISH_ARTIFACTS));
    }

    public Boolean isRecordAllDependencies() {
        return Boolean.parseBoolean(env.get(RECORD_ALL_DEPENDENCIES));
    }

    public boolean isResolveFromArtifactory() {
        return Boolean.parseBoolean(env.get(RESOLVE_FROM_ARTIFACTORY));
    }

    public long getResolutionArtifactoryServerId() {
        String serverId = env.get(PREFIX + RESOLUTION_SERVER_ID);
        if (StringUtils.isBlank(serverId)) {
            return -1;
        }
        return Long.parseLong(serverId);
    }

    public String getResolverUserName() {
        return env.get(PREFIX + RESOLVER_USER_NAME);
    }

    public String getResolverPassword() {
        return env.get(PREFIX + RESOLVER_PASSWORD);
    }


    public String getMavenOpts() {
        return env.get(PREFIX + MAVEN_OPTS);
    }

    public String getWorkingSubDirectory() {
        return env.get(PREFIX + WORKING_SUB_DIRECTORY);
    }

    @Override
    public String getIncludePattern() {
        return env.get(PREFIX + INCLUDE_PATTERN);
    }

    @Override
    public String getExcludePattern() {
        return env.get(PREFIX + EXCLUDE_PATTERN);
    }

    @Override
    public void resetDeployerContextToDefault() {
        super.resetDeployerContextToDefault();
        env.put(PREFIX + DEPLOYABLE_REPO_KEY, "");
        env.put(PUBLISH_ARTIFACTS, "true");
        env.put(RECORD_ALL_DEPENDENCIES, "false");
        env.put(PREFIX + INCLUDE_PATTERN, "");
        env.put(PREFIX + EXCLUDE_PATTERN, "");
    }

    public static Maven3BuildContext createMavenContextFromMap(Map<String, Object> map) {
        Map<String, String> transformed = Maps.transformValues(map, new Function<Object, String>() {
            @Override
            public String apply(Object input) {
                if (input == null) {
                    return "";
                }
                return input.toString();
            }
        });
        return new Maven3BuildContext(transformed);
    }

    /**
     * @return Get a set of all the fields to copy while populating the build context for a Maven 3 build.
     */
    public static Set<String> getFieldsToCopy() {
        Set<String> fieldsToCopy = Sets.newHashSet(PREFIX + SERVER_ID_PARAM, PREFIX + RESOLUTION_REPO_PARAM,
                PREFIX + DEPLOYABLE_REPO_KEY, PREFIX + DEPLOYER_USERNAME_PARAM,
                PREFIX + DEPLOYER_PASSWORD_PARAM, PUBLISH_BUILD_INFO_PARAM,
                INCLUDE_ENV_VARS_PARAM, ENV_VARS_EXCLUDE_PATTERNS, ENV_VARS_INCLUDE_PATTERNS,
                RUN_LICENSE_CHECKS, PREFIX + LICENSE_VIOLATION_RECIPIENTS,
                PREFIX + LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES, PREFIX + ENVIRONMENT_VARIABLES,
                PREFIX + INCLUDE_PUBLISHED_ARTIFACTS, PREFIX + DISABLE_AUTOMATIC_LICENSE_DISCOVERY,
                PUBLISH_ARTIFACTS, RECORD_ALL_DEPENDENCIES, PREFIX + PUBLISH_MAVEN_DESCRIPTORS_PARAM, PREFIX + PROJECT_FILE,
                PREFIX + PUBLISH_IVY_DESCRIPTORS_PARAM, USE_M2_COMPATIBLE_PATTERNS_PARAM,
                PREFIX + IVY_PATTERN_PARAM, PREFIX + ARTIFACT_PATTERN_PARAM, PREFIX + INCLUDE_PATTERN,
                PREFIX + EXCLUDE_PATTERN, PREFIX + FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM,
                PREFIX + GOALS, PREFIX + ADDITIONAL_MAVEN_PARAMS, PREFIX + JDK,
                PREFIX + MAVEN_OPTS, PREFIX + EXECUTABLE, TEST_CHECKED, PREFIX + TEST_RESULT_DIRECTORY, NEW_TASK_CREATED, CAPTURE_BUILD_INFO,
                TEST_DIRECTORY_OPTION, PREFIX + WORKING_SUB_DIRECTORY, ENABLE_RELEASE_MANAGEMENT, ENABLE_BINTRAY_CONFIGURATION,
                PREFIX + VCS_TAG_BASE, PREFIX + GIT_RELEASE_BRANCH, PREFIX + ALTERNATIVE_TASKS, RESOLVE_FROM_ARTIFACTORY
                , PREFIX + RESOLUTION_SERVER_ID, PREFIX + RESOLVER_USER_NAME, PREFIX + RESOLVER_PASSWORD);
        fieldsToCopy.addAll(getBlackDuckFieldsToCopy());
        fieldsToCopy.addAll(getOldCheckBoxFieldsToCopy());
        fieldsToCopy.addAll(PushToBintrayContext.bintrayFields);
        fieldsToCopy.addAll(getVcsFieldsToCopy());
        return fieldsToCopy;
    }

    /**
     * @return The deprecated checkbox fields that were used prior to the stripping of the namespace.
     */
    private static Set<String> getOldCheckBoxFieldsToCopy() {
        return Sets.newHashSet(PREFIX + PUBLISH_BUILD_INFO_PARAM, PREFIX + RUN_LICENSE_CHECKS,
                PREFIX + PUBLISH_ARTIFACTS, PREFIX + TEST_CHECKED, PREFIX + TEST_DIRECTORY_OPTION,
                PREFIX + ENABLE_RELEASE_MANAGEMENT, PREFIX + USE_M2_COMPATIBLE_PATTERNS_PARAM);
    }
}
