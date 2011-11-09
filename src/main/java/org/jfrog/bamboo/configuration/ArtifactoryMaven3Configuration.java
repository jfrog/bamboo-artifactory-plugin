package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.context.Maven3BuildContext;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryMaven3Task}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryMaven3Configuration extends AbstractArtifactoryConfiguration {
    private static final Set<String> FIELDS_TO_COPY = Maven3BuildContext.getFieldsToCopy();
    private static final String DEFAULT_TEST_RESULTS_FILE_PATTERN = "**/target/surefire-reports/*.xml";


    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("maven3Task", this);
        context.put("builderType", this);
        context.put("builder", this);
        //context.put("adminConfig", administrationConfiguration);
        context.put("baseUrl", administrationConfiguration.getBaseUrl());
        Plan plan = (Plan) context.get("plan");
        context.put("build", plan);
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("testDirectoryOption", "standardTestDirectory");
        context.put("selectedServerId", -1);
        context.put("selectedRepoKey", "");
        context.put("resolutionArtifactoryServerId", -1);
        context.put("selectedResolutionRepoKey", "");
        Repository repository = plan.getBuildDefinition().getRepository();
        if (repository != null) {
            String host = repository.getHost();
            context.put("builder.artifactoryMaven3Builder.vcsTagBase", host);
            context.put("builder.artifactoryMaven3Builder.gitReleaseBranch", "REL-BRANCH-");
        }
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        String publishingKey = Maven3BuildContext.PREFIX + Maven3BuildContext.DEPLOYABLE_REPO_KEY;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                Maven3BuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        Maven3BuildContext buildContext = Maven3BuildContext.createContextFromMap(context);
        String resolutionRepo = buildContext.getResolutionRepo();
        if (resolutionRepo == null) {
            resolutionRepo = "";
        }
        context.put("selectedResolutionRepoKey", resolutionRepo);
        context.put("selectedServerId", buildContext.getArtifactoryServerId());
        context.put("hasTests", buildContext.isTestChecked());
        context.put("serverConfigManager", serverConfigManager);
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        String publishingKey = Maven3BuildContext.PREFIX + Maven3BuildContext.DEPLOYABLE_REPO_KEY;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                Maven3BuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        Maven3BuildContext buildContext = Maven3BuildContext.createContextFromMap(context);
        long serverId = buildContext.getArtifactoryServerId();
        context.put("selectedServerId", serverId);
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        context.put("selectedServerUrl", serverConfig.getUrl());
        context.put("isRunLicenseChecks", buildContext.isRunLicenseChecks());
        context.put("isPublishArtifacts", buildContext.isPublishArtifacts());
        context.put("hasTests", buildContext.isTestChecked());
        context.put("serverConfigManager", serverConfigManager);
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
            @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(taskConfigMap, params, FIELDS_TO_COPY);
        Maven3BuildContext buildContext = new Maven3BuildContext(taskConfigMap);
        resetConfigIfNeeded(buildContext);
        taskConfigMap.put(Maven3BuildContext.PREFIX + Maven3BuildContext.TEST_RESULT_DIRECTORY,
                getTestDirectory(buildContext));
        return taskConfigMap;
    }

    @Override
    protected String getKey() {
        return "artifactoryMaven3Builder";
    }

    @Override
    protected String getDeployableRepoKey() {
        return Maven3BuildContext.DEPLOYABLE_REPO_KEY;
    }

    @Override
    protected String getDefaultTestDirectory() {
        return DEFAULT_TEST_RESULTS_FILE_PATTERN;
    }

    public boolean taskProducesTestResults(@NotNull TaskDefinition definition) {
        return new Maven3BuildContext(definition.getConfiguration()).isTestChecked();
    }
}
