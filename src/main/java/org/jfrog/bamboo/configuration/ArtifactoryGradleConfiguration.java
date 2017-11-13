package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GradleBuildContext;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGradleTask}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGradleConfiguration extends AbstractArtifactoryConfiguration {
    protected static final String DEFAULT_TEST_REPORTS_XML = "**/build/test-results/*.xml";

    private static final Set<String> FIELDS_TO_COPY = GradleBuildContext.getFieldsToCopy();

    public ArtifactoryGradleConfiguration() {
        super(GradleBuildContext.PREFIX);
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
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
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
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
        return "artifactoryGradleBuilder";
    }

    @Override
    protected String getDeployableRepoKey() {
        return GradleBuildContext.PUBLISHING_REPO_PARAM;
    }

    @Override
    protected String getDefaultTestDirectory() {
        return DEFAULT_TEST_REPORTS_XML;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition definition) {
        return new GradleBuildContext(definition.getConfiguration()).isTestChecked();
    }
}
