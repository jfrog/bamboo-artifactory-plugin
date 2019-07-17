package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.PublishBuildInfoContext;

import java.util.Map;
import java.util.Set;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryPublishBuildInfoConfiguration extends AbstractArtifactoryConfiguration {

    public static final String KEY = "artifactoryPublishBuildInfoBuilder";
    private static final Set<String> FIELDS_TO_COPY = PublishBuildInfoContext.getFieldsToCopy();

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("selectedServerId", context.get(PublishBuildInfoContext.SERVER_ID_PARAM));
        context.put("serverConfigManager", serverConfigManager);
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    protected String getDeployableRepoKey() {
        return null;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition taskDefinition) {
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
