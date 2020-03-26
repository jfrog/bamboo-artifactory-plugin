package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.util.TaskConfigurationValidations;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryMaven3Task}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryMaven3Configuration extends AbstractArtifactoryConfiguration {
    public static final String KEY = "artifactoryMaven3Builder";
    private static final Set<String> FIELDS_TO_COPY = Maven3BuildContext.getFieldsToCopy();
    private static final String DEFAULT_TEST_RESULTS_FILE_PATTERN = "**/target/surefire-reports/*.xml";

    public ArtifactoryMaven3Configuration() {
        super(Maven3BuildContext.PREFIX, CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".maven");
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateLegacyContextForCreate(context);
        context.put("maven3Task", this);
        context.put("builderType", this);
        context.put("builder", this);
        context.put("baseUrl", administrationConfiguration.getBaseUrl());
        Plan plan = (Plan) context.get("plan");
        context.put("build", plan);
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("testDirectoryOption", "standardTestDirectory");
        context.put("selectedServerId", -1);
        context.put("selectedRepoKey", "");
        context.put("selectedResolutionArtifactoryServerId", -1);
        context.put("selectedResolutionRepoKey", "");
        context.put("builder.artifactoryMaven3Builder.gitReleaseBranch", "REL-BRANCH-");
        context.put("artifactory.vcs.git.vcs.type.list", getVcsTypes());
        context.put("artifactory.vcs.git.authenticationType.list", getGitAuthenticationTypes());
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);

        String publishingKey = Maven3BuildContext.PREFIX + Maven3BuildContext.DEPLOYABLE_REPO_KEY;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                Maven3BuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        Maven3BuildContext buildContext = Maven3BuildContext.createMavenContextFromMap(context);
        String resolutionRepo = buildContext.getResolutionRepo();
        if (resolutionRepo == null) {
            resolutionRepo = "";
        }
        context.put("selectedResolutionArtifactoryServerId", buildContext.getResolutionArtifactoryServerId());
        context.put("selectedResolutionRepoKey", resolutionRepo);
        context.put("selectedServerId", buildContext.getArtifactoryServerId());
        context.put("hasTests", buildContext.isTestChecked());
        context.put("serverConfigManager", serverConfigManager);
        String envVarsExcludePatterns = (String) context.get(AbstractBuildContext.ENV_VARS_EXCLUDE_PATTERNS);
        if (envVarsExcludePatterns == null) {
            context.put(AbstractBuildContext.ENV_VARS_EXCLUDE_PATTERNS, AbstractBuildContext.ENV_VARS_TO_EXCLUDE);
        }
        context.put("artifactory.vcs.git.vcs.type.list", getVcsTypes());
        context.put("artifactory.vcs.git.authenticationType.list", getGitAuthenticationTypes());
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
            @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(taskConfigMap, params, FIELDS_TO_COPY);
        Maven3BuildContext buildContext = new Maven3BuildContext(taskConfigMap);
        resetDeployerConfigIfNeeded(buildContext);
        resetResolverConfigIfNeeded(buildContext);
        taskConfigMap.put(Maven3BuildContext.PREFIX + Maven3BuildContext.TEST_RESULT_DIRECTORY,
                getTestDirectory(buildContext));
        taskConfigMap.putAll(super.getSshFileContent(params, previousTaskDefinition));
        decryptFields(taskConfigMap);
        return taskConfigMap;
    }

    @Override
    protected void resetResolverConfigIfNeeded(AbstractBuildContext buildContext) {
        long serverId = buildContext.getResolutionArtifactoryServerId();
        boolean resolveFromArtifactory = ((Maven3BuildContext) buildContext).isResolveFromArtifactory();
        if (serverId == -1 || !resolveFromArtifactory) {
            buildContext.resetResolverContextToDefault();
        }
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    protected String getDefaultTestDirectory() {
        return DEFAULT_TEST_RESULTS_FILE_PATTERN;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition definition) {
        return new Maven3BuildContext(definition.getConfiguration()).isTestChecked();
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        // Validate resolution server.
        String resolutionServerKey = Maven3BuildContext.PREFIX + Maven3BuildContext.RESOLUTION_SERVER_ID_PARAM;
        String resolutionRepoKey = Maven3BuildContext.PREFIX + Maven3BuildContext.RESOLUTION_REPO_PARAM;
        TaskConfigurationValidations.validateArtifactoryServerAndRepo(resolutionServerKey, resolutionRepoKey, serverConfigManager, params, errorCollection);

        // Validate deployment server.
        String deploymentServerKey = Maven3BuildContext.PREFIX + Maven3BuildContext.SERVER_ID_PARAM;
        String deploymentRepoKey = Maven3BuildContext.PREFIX + Maven3BuildContext.DEPLOYABLE_REPO_KEY;
        TaskConfigurationValidations.validateArtifactoryServerAndRepo(deploymentServerKey, deploymentRepoKey, serverConfigManager, params, errorCollection);

        // Validate Goals.
        String goalsKey = Maven3BuildContext.PREFIX + Maven3BuildContext.GOALS;
        if (StringUtils.isBlank(params.getString(goalsKey))) {
            errorCollection.addError(goalsKey, "Please specify Goals.");
        }

        // Validate Build JDK.
        String buildJdkKey = Maven3BuildContext.PREFIX + AbstractBuildContext.JDK;
        TaskConfigurationValidations.validateJdk(buildJdkKey, params, errorCollection);

        // Validate Executable.
        String executableKey = Maven3BuildContext.PREFIX + AbstractBuildContext.EXECUTABLE;
        TaskConfigurationValidations.validateExecutable(executableKey, params, errorCollection);

        // Validate release management.
        TaskConfigurationValidations.validateReleaseManagement(params, errorCollection);
    }
}
