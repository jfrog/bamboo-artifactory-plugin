package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanHelper;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.admin.ServerConfig;
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
        Repository repository = PlanHelper.getDefaultRepository(plan);
        if (repository != null) {
            String host = repository.getHost();
            context.put("builder.artifactoryGradleBuilder.vcsTagBase", host);
        }
        context.put("builder.artifactoryGradleBuilder.gitReleaseBranch", "REL-BRANCH-");
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
            context.put(AbstractBuildContext.ENV_VARS_EXCLUDE_PATTERNS, "*password*,*secret*");
        }
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("serverConfigManager", serverConfigManager);
        GradleBuildContext buildContext = GradleBuildContext.createGradleContextFromMap(context);
        long serverId = buildContext.getArtifactoryServerId();
        context.put("selectedServerId", serverId);
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        context.put("selectedServerUrl", serverConfig.getUrl());
        context.put("hasTests", buildContext.isTestChecked());
        context.put("isPublishArtifacts", buildContext.isPublishArtifacts());
        context.put("isUseM2CompatiblePatterns", buildContext.isMaven2Compatible());
        context.put("useArtifactoryGradlePlugin", buildContext.useArtifactoryGradlePlugin());
        context.put("isPublishBuildInfo", buildContext.isPublishBuildInfo());
        context.put("isRunLicenseChecks", buildContext.isRunLicenseChecks());
    }


    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
            @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(taskConfigMap, params, FIELDS_TO_COPY);
        GradleBuildContext buildContext = new GradleBuildContext(taskConfigMap);
        resetConfigIfNeeded(buildContext);
        taskConfigMap.put(GradleBuildContext.PREFIX + GradleBuildContext.TEST_RESULT_DIRECTORY,
                getTestDirectory(buildContext));

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
