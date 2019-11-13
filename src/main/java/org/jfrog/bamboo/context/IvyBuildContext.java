package org.jfrog.bamboo.context;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class IvyBuildContext extends AbstractBuildContext {
    public static final String PREFIX = "builder.artifactoryIvyBuilder.";
    public static final String ANT_OPTS_PARAM = "antOpts";
    public static final String TARGET_OPTS_PARAM = "target";
    public static final String BUILD_FILE = "buildFile";
    public static final String DEPLOYABLE_REPO_KEY = "deployableRepo";
    public static final String PUBLISH_ARTIFACTS = "deployArtifacts";
    public static final String EXCLUDE_PATTERN = "deployExcludePatterns";
    public static final String INCLUDE_PATTERN = "deployIncludePatterns";
    public static final String WORKING_SUB_DIRECTORY = "workingSubDirectory";

    public IvyBuildContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    @Override
    public String getPublishingRepo() {
        return env.get(PREFIX + DEPLOYABLE_REPO_KEY);
    }

    public String getAntOpts() {
        return env.get(PREFIX + ANT_OPTS_PARAM);
    }

    public String getBuildFile() {
        return env.get(PREFIX + BUILD_FILE);
    }

    @Override
    public boolean isPublishArtifacts() {
        return Boolean.parseBoolean(env.get(PUBLISH_ARTIFACTS));
    }

    public String getTargets() {
        return env.get(PREFIX + TARGET_OPTS_PARAM);
    }

    public String getWorkingSubDirectory() {
        return env.get(PREFIX + WORKING_SUB_DIRECTORY);
    }

    @Override
    public String getExcludePattern() {
        return env.get(PREFIX + EXCLUDE_PATTERN);
    }

    @Override
    public void resetDeployerContextToDefault() {
        super.resetDeployerContextToDefault();
        env.put(PREFIX + DEPLOYABLE_REPO_KEY, "");
        env.put(PREFIX + INCLUDE_PATTERN, "");
        env.put(PUBLISH_ARTIFACTS, "true");
        env.put(PREFIX + EXCLUDE_PATTERN, "");
        env.put(PREFIX + INCLUDE_PATTERN, "");
    }

    @Override
    public String getIncludePattern() {
        return env.get(PREFIX + INCLUDE_PATTERN);
    }

    public static IvyBuildContext createIvyContextFromMap(Map<String, Object> map) {
        Map<String, String> transformed = Maps.transformValues(map, new Function<Object, String>() {
            @Override
            public String apply(Object input) {
                return input.toString();
            }
        });
        return new IvyBuildContext(transformed);
    }

    /**
     * @return Get a set of all the fields to copy while populating the build context for an Ivy build.
     */
    public static Set<String> getFieldsToCopy() {
        Set<String> fieldsToCopy = Sets.newHashSet(PREFIX + ANT_OPTS_PARAM, PREFIX + SERVER_ID_PARAM, PREFIX +
                RESOLUTION_REPO_PARAM, PREFIX + DEPLOYABLE_REPO_KEY, PREFIX + DEPLOYER_USERNAME_PARAM,
                PREFIX + DEPLOYER_PASSWORD_PARAM, PUBLISH_BUILD_INFO_PARAM,
                RUN_LICENSE_CHECKS, PREFIX + LICENSE_VIOLATION_RECIPIENTS,
                PREFIX + LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES, PREFIX + ENVIRONMENT_VARIABLES,
                INCLUDE_ENV_VARS_PARAM, ENV_VARS_INCLUDE_PATTERNS, ENV_VARS_EXCLUDE_PATTERNS,
                PREFIX + INCLUDE_PUBLISHED_ARTIFACTS, PREFIX + DISABLE_AUTOMATIC_LICENSE_DISCOVERY,
                PUBLISH_ARTIFACTS, PREFIX + PUBLISH_MAVEN_DESCRIPTORS_PARAM, PREFIX + BUILD_FILE,
                PREFIX + PUBLISH_IVY_DESCRIPTORS_PARAM, USE_M2_COMPATIBLE_PATTERNS_PARAM,
                PREFIX + IVY_PATTERN_PARAM, PREFIX + TARGET_OPTS_PARAM, PREFIX + JDK,
                PREFIX + ARTIFACT_PATTERN_PARAM, PREFIX + INCLUDE_PATTERN, PREFIX + FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM, BUILD_INFO_AGGREGATION, CAPTURE_BUILD_INFO,
                PREFIX + EXECUTABLE, PREFIX + EXCLUDE_PATTERN, TEST_CHECKED, PREFIX + TEST_RESULT_DIRECTORY,
                TEST_DIRECTORY_OPTION, PREFIX + WORKING_SUB_DIRECTORY);
        fieldsToCopy.addAll(getBlackDuckFieldsToCopy());
        fieldsToCopy.addAll(getOldCheckBoxFieldsToCopy());
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
