package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.context.IvyBuildContext;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGenericDeployTask}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGenericBuildConfiguration extends AbstractArtifactoryConfiguration {
    private static final Set<String> FIELDS_TO_COPY = GenericContext.getFieldsToCopy();

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("build", context.get("plan"));
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put("selectedRepoKey", StringUtils.EMPTY);
        context.put(GenericContext.PUBLISH_BUILD_INFO, "true");
        context.put(GenericContext.ENV_VARS_EXCLUDE_PATTERNS, "*password*,*secret*");
        context.put(GenericContext.SIGN_METHOD_MAP_KEY, GenericContext.SIGN_METHOD_MAP);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        migrateServerKeyIfNeeded(taskDefinition.getConfiguration());
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);

        String selectedPublishingRepoKey = context.get(GenericContext.REPO_KEY) != null ? context.get(GenericContext.REPO_KEY).toString() : null;
        if (StringUtils.isBlank(selectedPublishingRepoKey)) {
            // Compatibility with 1.8.0
            selectedPublishingRepoKey = taskDefinition.getConfiguration().get("artifactory.generic.deployableRepo");
        }
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        context.put("selectedServerId", context.get(GenericContext.PREFIX + GenericContext.SERVER_ID_PARAM));
        context.put(GenericContext.SIGN_METHOD_MAP_KEY, GenericContext.SIGN_METHOD_MAP);
        context.put("serverConfigManager", serverConfigManager);
        String envVarsExcludePatterns = (String)context.get(GenericContext.ENV_VARS_EXCLUDE_PATTERNS);
        if (envVarsExcludePatterns == null) {
            context.put(GenericContext.ENV_VARS_EXCLUDE_PATTERNS, "*password*,*secret*");
        }
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("serverConfigManager", serverConfigManager);
        IvyBuildContext buildContext = IvyBuildContext.createIvyContextFromMap(context);
        long serverId = buildContext.getArtifactoryServerId();
        context.put("selectedServerId", serverId);
    }

    @Override
    protected String getKey() {
        return "artifactoryGenericBuilder";
    }

    @Override
    protected String getDeployableRepoKey() {
        return "deployableRepo";
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition definition) {
        return false;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
                                                     @Nullable final TaskDefinition previousTaskDefinition) {
        Map<String, String> configMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(configMap, params, FIELDS_TO_COPY);
        decryptFields(configMap);
        return configMap;
    }
}
