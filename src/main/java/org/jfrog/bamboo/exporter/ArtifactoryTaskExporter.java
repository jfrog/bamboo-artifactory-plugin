package org.jfrog.bamboo.exporter;

import com.atlassian.bamboo.specs.api.builders.AtlassianModule;
import com.atlassian.bamboo.specs.api.builders.task.AnyTask;
import com.atlassian.bamboo.specs.api.builders.task.Task;
import com.atlassian.bamboo.specs.api.model.task.TaskProperties;
import com.atlassian.bamboo.specs.api.validators.common.ValidationProblem;
import com.atlassian.bamboo.task.TaskContainer;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.export.TaskDefinitionExporter;
import com.atlassian.bamboo.task.export.TaskValidationContext;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArtifactoryTaskExporter implements TaskDefinitionExporter {

    private static final Logger log = LogManager.getLogger(ArtifactoryTaskExporter.class);

    /**
     * Converts properties to Bamboo task configuration.
     * Can use all Bamboo server side services and can access DB if necessary.
     * Should throw a runtime exception if conversion fails.
     *
     * @param taskContainer  current task container (a job or an environment for instance)
     * @param taskProperties the current task
     * @return task configuration
     */
    @NotNull
    @Override
    public Map<String, String> toTaskConfiguration(@NotNull TaskContainer taskContainer, @NotNull TaskProperties taskProperties) {
        return Maps.newHashMap();
    }

    /**
     * Create Bamboo Specs object representing this task. Implementors don't need to handle common task properties like e.g. enabled/disabled, but
     * must task specific configuration.
     */
    @NotNull
    @Override
    public Task toSpecsEntity(@NotNull TaskDefinition taskDefinition) {
        log.debug(String.format("Exporting task definition by id: %s and plugin key: %s",
                taskDefinition.getId(), taskDefinition.getPluginKey()));
        Map<String, String> configurationMap = Maps.newHashMap();
        for (Map.Entry<String, String> entry : taskDefinition.getConfiguration().entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains("password") || key.contains("passphrase") || key.contains("key")) {
                configurationMap.put(entry.getKey(), "/* SENSITIVE INFORMATION */");
                continue;
            }
            configurationMap.put(entry.getKey(), entry.getValue());
        }
        return new AnyTask(new AtlassianModule(taskDefinition.getPluginKey())).configuration(configurationMap);
    }

    /**
     * Validates task properties in context of enclosing plan or deployment properties.
     * Should check for any inconsistencies between task definition and the rest of plan or deployment content, as it is provided in the validation context.
     * Returns list of validation errors or empty if everything is ok.
     */
    @NotNull
    @Override
    public List<ValidationProblem> validate(@NotNull TaskValidationContext taskValidationContext, @NotNull TaskProperties taskProperties) {
        return Collections.emptyList();
    }
}
