package org.jfrog.bamboo.context;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jfrog.bamboo.bintray.PushToBintrayContext;

import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class GradleBuildContext extends AbstractBuildContext {
    public static final String PREFIX = "builder.artifactoryGradleBuilder.";
    public static final String SWITCHES_PARAM = "switches";
    public static final String TASKS_PARAM = "tasks";
    public static final String BUILD_SCRIPT_PARAM = "buildScript";
    public static final String BUILD_FILE_PARAM = "buildFile";
    public static final String USE_GRADLE_WRAPPER_PARAM = "useGradleWrapper";
    public static final String GRADLE_WRAPPER_LOCATION_PARAM = "gradleWrapperLocation";

    public GradleBuildContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public String getSwitches() {
        return env.get(PREFIX + SWITCHES_PARAM);
    }

    public String getTasks() {
        return env.get(PREFIX + TASKS_PARAM);
    }

    public String getBuildScript() {
        return env.get(PREFIX + BUILD_SCRIPT_PARAM);
    }

    public String getBuildFile() {
        return env.get(PREFIX + BUILD_FILE_PARAM);
    }

    public String getReleaseProps() {
        return env.get(PREFIX + RELEASE_PROPS);
    }

    public String getNextIntegProps() {
        return env.get(PREFIX + NEXT_INTEG_PROPS);
    }

    public String getArtifactSpecs() {
        return env.get(PREFIX + ARTIFACT_SPECS_PARAM);
    }

    public boolean isUseGradleWrapper() {
        return Boolean.valueOf(env.get(PREFIX + USE_GRADLE_WRAPPER_PARAM));
    }

    public String getGradleWrapperLocation() {
        return env.get(PREFIX + GRADLE_WRAPPER_LOCATION_PARAM);
    }

    public static GradleBuildContext createGradleContextFromMap(Map<String, Object> map) {
        Map<String, String> transformed = Maps.transformValues(map, new Function<Object, String>() {
            @Override
            public String apply(Object input) {
                if (input == null) {
                    return "";
                }
                return input.toString();
            }
        });
        return new GradleBuildContext(transformed);
    }

    /**
     * @return Get a set of all the fields to copy while populating the build context for a Gradle build.
     */
    public static Set<String> getFieldsToCopy() {
        Set<String> fieldsToCopy = Sets.newHashSet(PREFIX + SWITCHES_PARAM, PREFIX + TASKS_PARAM,
                PREFIX + BUILD_SCRIPT_PARAM, PREFIX + BUILD_FILE_PARAM, PREFIX + USE_GRADLE_WRAPPER_PARAM,
                PREFIX + GRADLE_WRAPPER_LOCATION_PARAM, PREFIX + SERVER_ID_PARAM, PREFIX + RESOLUTION_REPO_PARAM,
                PREFIX + PUBLISHING_REPO_PARAM, PREFIX + DEPLOYER_USERNAME_PARAM, PREFIX + DEPLOYER_PASSWORD_PARAM,
                PREFIX + USE_ARTIFACTORY_GRADLE_PLUGIN, PUBLISH_BUILD_INFO_PARAM,
                INCLUDE_ENV_VARS_PARAM, ENV_VARS_EXCLUDE_PATTERNS, ENV_VARS_INCLUDE_PATTERNS,
                RUN_LICENSE_CHECKS, PREFIX + LICENSE_VIOLATION_RECIPIENTS,
                PREFIX + LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES, PREFIX + ENVIRONMENT_VARIABLES,
                PREFIX + INCLUDE_PUBLISHED_ARTIFACTS, PREFIX + DISABLE_AUTOMATIC_LICENSE_DISCOVERY,
                PUBLISH_ARTIFACTS_PARAM, PREFIX + PUBLISH_MAVEN_DESCRIPTORS_PARAM,
                PREFIX + PUBLISH_IVY_DESCRIPTORS_PARAM, USE_M2_COMPATIBLE_PATTERNS_PARAM,
                PREFIX + IVY_PATTERN_PARAM, PREFIX + JDK, PREFIX + ARTIFACT_PATTERN_PARAM,
                PREFIX + PUBLISH_INCLUDE_PATTERNS_PARAM, PREFIX + PUBLISH_EXCLUDE_PATTERNS_PARAM,
                PREFIX + FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM,
                PREFIX + ARTIFACT_SPECS_PARAM, PREFIX + EXECUTABLE, TEST_CHECKED, PREFIX + TEST_RESULT_DIRECTORY,
                TEST_DIRECTORY_OPTION, ENABLE_RELEASE_MANAGEMENT, ENABLE_BINTRAY_CONFIGURATION, PREFIX + VCS_TAG_BASE, PREFIX + GIT_RELEASE_BRANCH,
                PREFIX + ALTERNATIVE_TASKS, PREFIX + RELEASE_PROPS, PREFIX + NEXT_INTEG_PROPS);
        fieldsToCopy.addAll(getBlackDuckFieldsToCopy());
        fieldsToCopy.addAll(getOldCheckBoxFieldsToCopy());
        fieldsToCopy.addAll(PushToBintrayContext.bintrayFields);
        return fieldsToCopy;
    }

    /**
     * @return The deprecated checkbox fields that were used prior to the stripping of the namespace.
     */
    private static Set<String> getOldCheckBoxFieldsToCopy() {
        return Sets.newHashSet(PREFIX + PUBLISH_BUILD_INFO_PARAM, PREFIX + INCLUDE_ENV_VARS_PARAM,
                PREFIX + RUN_LICENSE_CHECKS, PREFIX + PUBLISH_ARTIFACTS_PARAM, PREFIX + TEST_CHECKED,
                PREFIX + TEST_DIRECTORY_OPTION, PREFIX + ENABLE_RELEASE_MANAGEMENT,
                PREFIX + USE_M2_COMPATIBLE_PATTERNS_PARAM);
    }
}
