package org.jfrog.bamboo.context;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
        Map<String, String> transformed = Maps.transformValues(map, input -> {
            if (input == null) {
                return "";
            }
            return input.toString();
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
                PUBLISH_ARTIFACTS, RECORD_ALL_DEPENDENCIES, PREFIX + PUBLISH_MAVEN_DESCRIPTORS_PARAM, PREFIX + PROJECT_FILE,
                PREFIX + PUBLISH_IVY_DESCRIPTORS_PARAM, USE_M2_COMPATIBLE_PATTERNS_PARAM,
                PREFIX + IVY_PATTERN_PARAM, PREFIX + ARTIFACT_PATTERN_PARAM, PREFIX + INCLUDE_PATTERN,
                PREFIX + EXCLUDE_PATTERN, PREFIX + FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM,
                PREFIX + GOALS, PREFIX + ADDITIONAL_MAVEN_PARAMS, PREFIX + JDK,
                PREFIX + MAVEN_OPTS, PREFIX + EXECUTABLE, TEST_CHECKED, PREFIX + TEST_RESULT_DIRECTORY,
                BUILD_INFO_AGGREGATION, CAPTURE_BUILD_INFO, PREFIX + ENVIRONMENT_VARIABLES,
                TEST_DIRECTORY_OPTION, PREFIX + WORKING_SUB_DIRECTORY, ENABLE_RELEASE_MANAGEMENT,
                PREFIX + VCS_TAG_BASE, PREFIX + GIT_RELEASE_BRANCH, PREFIX + ALTERNATIVE_TASKS, RESOLVE_FROM_ARTIFACTORY,
                PREFIX + RESOLUTION_SERVER_ID_PARAM, PREFIX + RESOLVER_USERNAME_PARAM, PREFIX + RESOLVER_PASSWORD_PARAM,
                RESOLVER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS, RESOLVER_SHARED_CREDENTIALS);
        fieldsToCopy.addAll(getVcsFieldsToCopy());
        return fieldsToCopy;
    }
}
