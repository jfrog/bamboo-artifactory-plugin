package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.util.TaskConfigurationValidations;
import org.jfrog.bamboo.context.ArtifactoryBuildContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.context.PackageManagersContext;

import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.context.ArtifactoryBuildContext.DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGradleTask}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGradleConfiguration extends AbstractArtifactoryConfiguration {
    public static final String KEY = "artifactoryGradleBuilder";
    protected static final String DEFAULT_TEST_REPORTS_XML = "**/build/test-results/*.xml";
    private static final Set<String> FIELDS_TO_COPY = GradleBuildContext.getFieldsToCopy();
    private static final String PUBLISH_FORK_COUNT_OPTIONS_KEY = "publishForkCountList";
    private static final String PUBLISH_FORK_COUNT_KEY = "publishForkCount";

    public ArtifactoryGradleConfiguration() {
        super(GradleBuildContext.PREFIX, CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".gradle");
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateLegacyContextForCreate(context);
        context.put("artifactoryGradleTask", this);
        context.put("builderType", this);
        context.put("builder", this);
        context.put("adminConfig", administrationConfiguration);
        context.put("baseUrl", administrationConfiguration.getBaseUrl());
        Plan plan = (Plan) context.get("plan");
        context.put("build", plan);
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put("selectedResolutionRepoKey", "");
        context.put("selectedPublishingRepoKey", "");
        context.put("builder.artifactoryGradleBuilder.gitReleaseBranch", "REL-BRANCH-");
        context.put("artifactory.vcs.git.vcs.type.list", getVcsTypes());
        context.put("artifactory.vcs.git.authenticationType.list", getGitAuthenticationTypes());
        context.put(GradleBuildContext.PREFIX + PUBLISH_FORK_COUNT_OPTIONS_KEY, GradleBuildContext.getPublishForkCountList());
        context.put(GradleBuildContext.PREFIX + PUBLISH_FORK_COUNT_KEY, GradleBuildContext.getDefaultPublishForkCount());
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);

        context.put("selectedServerId", context.get(GradleBuildContext.PREFIX + GradleBuildContext.SERVER_ID_PARAM));
        String resolutionRepoKey = GradleBuildContext.PREFIX + GradleBuildContext.RESOLUTION_REPO_PARAM;
        String selectedResolutionRepoKey =
                context.get(resolutionRepoKey) != null ? context.get(resolutionRepoKey).toString() :
                        GradleBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED;
        context.put("selectedResolutionRepoKey", selectedResolutionRepoKey);
        String publishingKey = GradleBuildContext.PREFIX + GradleBuildContext.PUBLISHING_REPO_PARAM;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                GradleBuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedPublishingRepoKey", selectedPublishingRepoKey);
        GradleBuildContext buildContext = GradleBuildContext.createGradleContextFromMap(context);
        context.put("hasTests", buildContext.isTestChecked());
        context.put("serverConfigManager", serverConfigManager);
        context.put("artifactory.vcs.git.vcs.type.list", getVcsTypes());
        context.put("artifactory.vcs.git.authenticationType.list", getGitAuthenticationTypes());
        context.put(GradleBuildContext.PREFIX + PUBLISH_FORK_COUNT_OPTIONS_KEY, GradleBuildContext.getPublishForkCountList());
        context.put(GradleBuildContext.PREFIX + PUBLISH_FORK_COUNT_KEY, buildContext.getPublishForkCount());
        populateDefaultEnvVarsExcludePatternsInBuildContext(context);
        populateDefaultBuildNameNumberInBuildContext(context);

        // Backward compatibility for tasks with overridden username and password
        Map<String, String> taskConfiguration = taskDefinition.getConfiguration();
        GradleBuildContext taskContext = new GradleBuildContext(taskConfiguration);
        if (StringUtils.isBlank(taskContext.getDeployerOverrideCredentialsChoice()) &&
                StringUtils.isNoneBlank(taskContext.getDeployerUsername(), taskContext.getDeployerPassword())) {
            context.put(DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, CVG_CRED_USERNAME_PASSWORD);
        }
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
                                                     @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(taskConfigMap, params, FIELDS_TO_COPY);
        GradleBuildContext buildContext = new GradleBuildContext(taskConfigMap);
        resetDeployerConfigIfNeeded(buildContext);
        resetResolverConfigIfNeeded(buildContext);
        taskConfigMap.put(GradleBuildContext.PREFIX + GradleBuildContext.TEST_RESULT_DIRECTORY,
                getTestDirectory(buildContext));
        taskConfigMap.putAll(super.getSshFileContent(params, previousTaskDefinition));
        decryptFields(taskConfigMap);
        return taskConfigMap;
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    protected String getDefaultTestDirectory() {
        return DEFAULT_TEST_REPORTS_XML;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition definition) {
        return new GradleBuildContext(definition.getConfiguration()).isTestChecked();
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        // Validate resolution server.
        String resolutionServerKey = GradleBuildContext.PREFIX + GradleBuildContext.RESOLUTION_SERVER_ID_PARAM;
        String resolutionRepoKey = Maven3BuildContext.PREFIX + GradleBuildContext.RESOLUTION_REPO_PARAM;
        TaskConfigurationValidations.validateArtifactoryServerAndRepo(resolutionServerKey, resolutionRepoKey, serverConfigManager, params, errorCollection);

        // Validate deployment server.
        String deploymentServerKey = GradleBuildContext.PREFIX + GradleBuildContext.SERVER_ID_PARAM;
        String deploymentRepoKey = GradleBuildContext.PREFIX + GradleBuildContext.PUBLISHING_REPO_PARAM;
        TaskConfigurationValidations.validateArtifactoryServerAndRepo(deploymentServerKey, deploymentRepoKey, serverConfigManager, params, errorCollection);

        // Validate Build JDK.
        String buildJdkKey = GradleBuildContext.PREFIX + PackageManagersContext.JDK;
        TaskConfigurationValidations.validateJdk(buildJdkKey, params, errorCollection);

        // Validate Executable.
        if (!params.getBoolean(GradleBuildContext.PREFIX + GradleBuildContext.USE_GRADLE_WRAPPER_PARAM)) {
            String executableKey = GradleBuildContext.PREFIX + PackageManagersContext.EXECUTABLE;
            TaskConfigurationValidations.validateExecutable(executableKey, params, errorCollection);
        }

        // Validate release management.
        TaskConfigurationValidations.validateReleaseManagement(params, errorCollection);

        // Validate build name and number.
        TaskConfigurationValidations.validateCaptureBuildInfoParams(ArtifactoryBuildContext.BUILD_NAME, ArtifactoryBuildContext.BUILD_NUMBER, GradleBuildContext.CAPTURE_BUILD_INFO, params, errorCollection);
    }
}
