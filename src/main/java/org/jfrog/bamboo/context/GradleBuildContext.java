package org.jfrog.bamboo.context;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Tomer Cohen
 */
public class GradleBuildContext extends PackageManagersContext {
    public static final String PREFIX = "builder.artifactoryGradleBuilder.";
    public static final String SWITCHES_PARAM = "switches";
    public static final String TASKS_PARAM = "tasks";
    public static final String BUILD_SCRIPT_PARAM = "buildScript";
    public static final String BUILD_FILE_PARAM = "buildFile";
    public static final String USE_GRADLE_WRAPPER_PARAM = "useGradleWrapper";
    public static final String GRADLE_WRAPPER_LOCATION_PARAM = "gradleWrapperLocation";
    public static final String PUBLISH_FORK_COUNT_PARAM = "publishForkCount";
    private static final List<Integer> PUBLISH_FORK_COUNT_OPTIONS = ImmutableList.of(1, 2, 4, 8);

    public GradleBuildContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    @Override
    public String getResolverUsername() {
        return env.get(PREFIX + DEPLOYER_USERNAME_PARAM);
    }

    @Override
    public String getResolverPassword() {
        return env.get(PREFIX + DEPLOYER_PASSWORD_PARAM);
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
        return Boolean.parseBoolean(env.get(PREFIX + USE_GRADLE_WRAPPER_PARAM));
    }

    public String getGradleWrapperLocation() {
        return env.get(PREFIX + GRADLE_WRAPPER_LOCATION_PARAM);
    }

    public int getPublishForkCount() {
        String forkCount = env.get(PREFIX + PUBLISH_FORK_COUNT_PARAM);
        return Integer.parseInt(StringUtils.isNotBlank(forkCount) ? (forkCount) : getDefaultPublishForkCount());
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
                PUBLISH_ARTIFACTS_PARAM, PREFIX + PUBLISH_FORK_COUNT_PARAM, PREFIX + PUBLISH_MAVEN_DESCRIPTORS_PARAM,
                PREFIX + PUBLISH_IVY_DESCRIPTORS_PARAM, USE_M2_COMPATIBLE_PATTERNS_PARAM,
                PREFIX + IVY_PATTERN_PARAM, PREFIX + JDK, PREFIX + ARTIFACT_PATTERN_PARAM,
                PREFIX + PUBLISH_INCLUDE_PATTERNS_PARAM, PREFIX + PUBLISH_EXCLUDE_PATTERNS_PARAM,
                PREFIX + FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD_PARAM, PREFIX + ENVIRONMENT_VARIABLES,
                PREFIX + ARTIFACT_SPECS_PARAM, PREFIX + EXECUTABLE, TEST_CHECKED,
                PREFIX + TEST_RESULT_DIRECTORY, BUILD_INFO_AGGREGATION, CAPTURE_BUILD_INFO,
                TEST_DIRECTORY_OPTION, ENABLE_RELEASE_MANAGEMENT, PREFIX + VCS_TAG_BASE, PREFIX + GIT_RELEASE_BRANCH,
                PREFIX + ALTERNATIVE_TASKS, PREFIX + RELEASE_PROPS, PREFIX + NEXT_INTEG_PROPS,
                BUILD_NAME, BUILD_NUMBER, RESOLVER_OVERRIDE_CREDENTIALS_CHOICE, RESOLVER_SHARED_CREDENTIALS,
                DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS);
        fieldsToCopy.addAll(getVcsFieldsToCopy());
        return fieldsToCopy;
    }

    public static List<String> getPublishForkCountList() {
        return PUBLISH_FORK_COUNT_OPTIONS.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    public static String getDefaultPublishForkCount() {
        return String.valueOf(PUBLISH_FORK_COUNT_OPTIONS.stream()
                .mapToInt(v -> v)
                .max().orElseThrow(NoSuchElementException::new));
    }
}
