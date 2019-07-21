package org.jfrog.bamboo.configuration.util;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.configuration.Jdk;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class TaskConfiguratorHelperImpl {

    public void addJdkRequirement(@NotNull Set<Requirement> requirements, @NotNull TaskDefinition taskDefinition, @NotNull String cfgJdkLabel) {
        addSystemRequirementFromConfiguration(requirements, taskDefinition, cfgJdkLabel, Jdk.CAPABILITY_JDK_PREFIX);
    }

    public void addSystemRequirementFromConfiguration(@NotNull Set<Requirement> requirements, @NotNull TaskDefinition taskDefinition,
                                                      @NotNull String cfgKey, @NotNull String requirementPrefix) {
        final String builderLabel = taskDefinition.getConfiguration().get(cfgKey);
        if (builderLabel != null) {
            requirements.add(new RequirementImpl(requirementPrefix + "." + builderLabel, true, ".*"));
        }
    }

    public void populateContextWithConfiguration(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition, @NotNull Iterable<String> keys) {
        for (String key : keys) {
            context.put(key, taskDefinition.getConfiguration().get(key));
        }
    }

    public void populateTaskConfigMapWithActionParameters(@NotNull Map<String, String> config, @NotNull ActionParametersMap params, @NotNull Iterable<String> keys) {
        for (String key : keys) {
            config.put(key, params.getString(key));
        }
    }
}