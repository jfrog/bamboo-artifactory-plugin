package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.util.TaskConfigurationValidations;
import org.jfrog.bamboo.context.ArtifactoryBuildContext;
import org.jfrog.bamboo.context.DotNetBuildContext;
import org.jfrog.bamboo.context.PackageManagersContext;

import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 13/10/2020.
 */
public abstract class AbstractDotNetBuildConfiguration extends AbstractArtifactoryConfiguration {

    private static final Set<String> FIELDS_TO_COPY = DotNetBuildContext.getFieldsToCopy();
    public static final String CFG_COMMAND_RESTORE = "restore";
    private static final String CFG_COMMAND_PUSH = "push";
    private static final Map<String, String> CFG_COMMAND_OPTIONS = ImmutableMap.of(CFG_COMMAND_RESTORE, "restore",
            CFG_COMMAND_PUSH, "push");

    @Override
    protected String getBuilderContextPrefix() {
        return DotNetBuildContext.PREFIX;
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateCommandsContext(context);
        context.put("adminConfig", getAdministrationConfiguration());
        context.put("baseUrl", getAdministrationConfiguration().getBaseUrl());
        Plan plan = (Plan) context.get("plan");
        context.put("build", plan);
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedResolutionServerId", -1);
        context.put("selectedResolutionRepoKey", "");
        context.put("selectedPublishingServerId", -1);
        context.put("selectedPublishingRepoKey", "");
        context.put(DotNetBuildContext.PUSH_PATTERN, DotNetBuildContext.PUSH_PATTERN_DEFAULT_VALUE);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateCommandsContext(context);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);

        context.put("selectedResolutionServerId", context.get(DotNetBuildContext.RESOLVER_SERVER_ID));
        context.put("selectedPublishingServerId", context.get(DotNetBuildContext.DEPLOYER_SERVER_ID));

        String selectedResolutionRepoKey = context.get(DotNetBuildContext.RESOLUTION_REPO) != null ?
                context.get(DotNetBuildContext.RESOLUTION_REPO).toString() : DotNetBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED;
        context.put("selectedResolutionRepoKey", selectedResolutionRepoKey);

        String selectedPublishingRepoKey = context.get(DotNetBuildContext.PUBLISHING_REPO) != null ?
                context.get(DotNetBuildContext.PUBLISHING_REPO).toString() : DotNetBuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedPublishingRepoKey", selectedPublishingRepoKey);

        context.put("serverConfigManager", serverConfigManager);
        String envVarsExcludePatterns = (String) context.get(PackageManagersContext.ENV_VARS_EXCLUDE_PATTERNS);
        if (envVarsExcludePatterns == null) {
            context.put(PackageManagersContext.ENV_VARS_EXCLUDE_PATTERNS, PackageManagersContext.ENV_VARS_TO_EXCLUDE);
        }

        String pushPattern = (String) context.get(DotNetBuildContext.PUSH_PATTERN);
        if (pushPattern == null) {
            context.put(DotNetBuildContext.PUSH_PATTERN, DotNetBuildContext.PUSH_PATTERN_DEFAULT_VALUE);
        }
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
                                                     @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(taskConfigMap, params, FIELDS_TO_COPY);
        DotNetBuildContext buildContext = new DotNetBuildContext(taskConfigMap);
        resetResolverConfigIfNeeded(buildContext);
        taskConfigMap.putAll(super.getSshFileContent(params, previousTaskDefinition));
        decryptFields(taskConfigMap);
        return taskConfigMap;
    }

    private void populateCommandsContext(Map<String, Object> context) {
        context.put("commandOptions", CFG_COMMAND_OPTIONS);
        context.put(DotNetBuildContext.COMMAND_CHOICE, CFG_COMMAND_RESTORE);
    }

    @Override
    protected void resetResolverConfigIfNeeded(PackageManagersContext buildContext) {
        long serverId = buildContext.getResolutionArtifactoryServerId();
        if (serverId == -1) {
            buildContext.resetResolverContextToDefault();
        }
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition taskDefinition) {
        return false;
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        String commandChoiceKey = DotNetBuildContext.COMMAND_CHOICE;
        if (CFG_COMMAND_RESTORE.equals(params.getString(commandChoiceKey))) {
            // Validate resolution server.
            String serverKey = DotNetBuildContext.RESOLVER_SERVER_ID;
            String repoKey = DotNetBuildContext.RESOLUTION_REPO;
            TaskConfigurationValidations.validateArtifactoryServerAndRepo(serverKey, repoKey, serverConfigManager, params, errorCollection);
        }

        if (CFG_COMMAND_PUSH.equals(params.getString(commandChoiceKey))) {
            // Validate deployment server.
            String serverKey = DotNetBuildContext.DEPLOYER_SERVER_ID;
            String repoKey = DotNetBuildContext.PUBLISHING_REPO;
            TaskConfigurationValidations.validateArtifactoryServerAndRepo(serverKey, repoKey, serverConfigManager, params, errorCollection);
        }

        // Validate Executable.
        String executableKey = DotNetBuildContext.DOTNET_EXECUTABLE;
        if (StringUtils.isBlank(params.getString(executableKey))) {
            errorCollection.addError(executableKey, "Please specify an Executable.");
        }

        // Validate build name and number.
        TaskConfigurationValidations.validateCaptureBuildInfoParams(ArtifactoryBuildContext.BUILD_NAME, ArtifactoryBuildContext.BUILD_NUMBER, DotNetBuildContext.CAPTURE_BUILD_INFO, params, errorCollection);
    }
}
