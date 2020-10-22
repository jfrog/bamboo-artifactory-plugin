package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.util.TaskConfigurationValidations;
import org.jfrog.bamboo.context.ArtifactoryBuildContext;
import org.jfrog.bamboo.context.DockerBuildContext;
import org.jfrog.bamboo.context.PackageManagersContext;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryDockerTask}
 *
 * Created by Bar Belity on 09/10/2020.
 */
public class ArtifactoryDockerConfiguration extends AbstractArtifactoryConfiguration {
    public static final String CFG_DOCKER_COMMAND_PULL = "pull";
    private static final String CFG_DOCKER_COMMAND_PUSH = "push";
    private static final String KEY = "artifactoryDockerBuilder";
    private static final Set<String> FIELDS_TO_COPY = DockerBuildContext.getFieldsToCopy();
    private static final Map<String, String> CFG_DOCKER_COMMAND_OPTIONS = ImmutableMap.of(CFG_DOCKER_COMMAND_PULL, "pull", CFG_DOCKER_COMMAND_PUSH, "push");

    public ArtifactoryDockerConfiguration() {
        super(DockerBuildContext.PREFIX, CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".docker");
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateDockerCommandContext(context);
        context.put("artifactoryDockerTask", this);
        context.put("builderType", this);
        context.put("builder", this);
        context.put("adminConfig", administrationConfiguration);
        context.put("baseUrl", administrationConfiguration.getBaseUrl());
        Plan plan = (Plan) context.get("plan");
        context.put("build", plan);
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedResolutionServerId", -1);
        context.put("selectedResolutionRepoKey", "");
        context.put("selectedPublishingServerId", -1);
        context.put("selectedPublishingRepoKey", "");
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateDockerCommandContext(context);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("selectedPublishingServerId", context.get(DockerBuildContext.DOCKER_PUSH_SERVER_ID));
        context.put("selectedResolutionServerId", context.get(DockerBuildContext.DOCKER_PULL_SERVER_ID));
        String selectedResolutionRepoKey = context.get(DockerBuildContext.DOCKER_PULL_REPO) != null ?
                context.get(DockerBuildContext.DOCKER_PULL_REPO).toString() : DockerBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED;
        context.put("selectedResolutionRepoKey", selectedResolutionRepoKey);
        String selectedPublishingRepoKey = context.get(DockerBuildContext.DOCKER_PUSH_REPO) != null ?
                context.get(DockerBuildContext.DOCKER_PUSH_REPO).toString() : DockerBuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedPublishingRepoKey", selectedPublishingRepoKey);
        context.put("serverConfigManager", serverConfigManager);
        String envVarsExcludePatterns = (String) context.get(PackageManagersContext.ENV_VARS_EXCLUDE_PATTERNS);
        if (envVarsExcludePatterns == null) {
            context.put(PackageManagersContext.ENV_VARS_EXCLUDE_PATTERNS, PackageManagersContext.ENV_VARS_TO_EXCLUDE);
        }
    }

    private void populateDockerCommandContext(@NotNull Map<String, Object> context) {
        context.put("dockerCommandOptions", CFG_DOCKER_COMMAND_OPTIONS);
        context.put(DockerBuildContext.COMMAND_CHOICE, CFG_DOCKER_COMMAND_PULL);
    }

    @Override
    protected void resetResolverConfigIfNeeded(PackageManagersContext buildContext) {
        long serverId = buildContext.getResolutionArtifactoryServerId();
        if (serverId == -1) {
            buildContext.resetResolverContextToDefault();
        }
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
                                                     @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(taskConfigMap, params, FIELDS_TO_COPY);
        DockerBuildContext buildContext = new DockerBuildContext(taskConfigMap);
        resetDeployerConfigIfNeeded(buildContext);
        resetResolverConfigIfNeeded(buildContext);
        taskConfigMap.putAll(super.getSshFileContent(params, previousTaskDefinition));
        decryptFields(taskConfigMap);
        return taskConfigMap;
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition taskDefinition) {
        return false;
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        String commandChoiceKey = DockerBuildContext.COMMAND_CHOICE;
        if (CFG_DOCKER_COMMAND_PUSH.equals(params.getString(commandChoiceKey))) {
            // Validate push server.
            TaskConfigurationValidations.validateArtifactoryServerAndRepo(DockerBuildContext.DOCKER_PUSH_SERVER_ID,
                    DockerBuildContext.DOCKER_PUSH_REPO, serverConfigManager, params, errorCollection);
        }
        if (CFG_DOCKER_COMMAND_PULL.equals(params.getString(commandChoiceKey))) {
            // Validate pull server.
            TaskConfigurationValidations.validateArtifactoryServerAndRepo(DockerBuildContext.DOCKER_PULL_SERVER_ID,
                    DockerBuildContext.DOCKER_PULL_REPO, serverConfigManager, params, errorCollection);
        }

        // Validate image name.
        String imageName = DockerBuildContext.IMAGE_NAME;
        if (StringUtils.isBlank(params.getString(imageName))) {
            errorCollection.addError(imageName, "Please specify an Image Name.");
        }

        // Validate build name and number.
        TaskConfigurationValidations.validateCaptureBuildInfoParams(ArtifactoryBuildContext.BUILD_NAME,
                ArtifactoryBuildContext.BUILD_NUMBER, DockerBuildContext.CAPTURE_BUILD_INFO, params, errorCollection);
    }
}
