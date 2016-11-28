package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.context.IvyBuildContext;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGenericResolveTask}
 *
 * @author Lior Hasson
 */
public class ArtifactoryGenericResolveConfiguration extends AbstractArtifactoryConfiguration {
    private static final Set<String> FIELDS_TO_COPY = GenericContext.getFieldsToCopy();

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("build", context.get("plan"));
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put("selectedRepoKey", "");
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        migrateServerKeyIfNeeded(taskDefinition.getConfiguration());
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);

        String publishingKey = GenericContext.REPO_KEY;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                IvyBuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        context.put("selectedServerId", context.get(GenericContext.PREFIX + GenericContext.SERVER_ID_PARAM));
        context.put("serverConfigManager", serverConfigManager);
    }

    @Override
    protected String getKey() {
        return "artifactoryGenericBuilder";
    }

    @Override
    protected String getDeployableRepoKey() {
        return null;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition definition) {
        return false;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
                                                     @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> configMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(configMap, params, FIELDS_TO_COPY);
        decryptFields(configMap);
        return configMap;
    }
}
