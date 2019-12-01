package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.CollectIssuesContext;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGenericResolveTask}
 *
 * @author Lior Hasson
 */
public class ArtifactoryCollectIssuesConfiguration extends AbstractArtifactoryConfiguration {
    public static final String CFG_CONFIG_SOURCE_TASK_CONFIGURATION = "taskConfiguration";
    private static final String KEY = "artifactoryCollectIssuesBuilder";
    private static final Set<String> FIELDS_TO_COPY = CollectIssuesContext.getFieldsToCopy();
    private static final String CFG_CONFIG_SOURCE_FILE = "file";
    private static final Map<String, String> CFG_CONFIG_SOURCE_OPTIONS = ImmutableMap.of(CFG_CONFIG_SOURCE_TASK_CONFIGURATION, "Task Configuration", CFG_CONFIG_SOURCE_FILE, "File");


    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateConfigContext(context);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put("build", context.get("plan"));
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateConfigContext(context);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("selectedServerId", context.get(CollectIssuesContext.SERVER_ID_PARAM));
        context.put("serverConfigManager", serverConfigManager);
    }

    /**
     * Populate issues collection config
     */
    private void populateConfigContext(@NotNull Map<String, Object> context) {
        context.put("configSourceOptions", CFG_CONFIG_SOURCE_OPTIONS);
        context.put(CollectIssuesContext.CONFIG_SOURCE_CHOICE, CFG_CONFIG_SOURCE_TASK_CONFIGURATION);
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
