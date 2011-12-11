package org.jfrog.bamboo.util;

import com.atlassian.bamboo.task.TaskDefinition;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.task.ArtifactoryGradleTask;
import org.jfrog.bamboo.task.ArtifactoryMaven3Task;

import java.util.List;

/**
 * Utility class to help find tasks of a certain type.
 *
 * @author Tomer Cohen
 */
public abstract class TaskDefinitionHelper {

    private TaskDefinitionHelper() {
        throw new IllegalAccessError();
    }

    /**
     * Find a Maven task defined by {@link ArtifactoryMaven3Task} or a Gradle task type {@link ArtifactoryGradleTask}
     *
     * @param taskDefinitions The list of of task definitions to search.
     * @return A Maven or Gradle task if found, null if not.
     */
    @Nullable
    public static TaskDefinition findMavenOrGradleDefinition(List<TaskDefinition> taskDefinitions) {
        if (taskDefinitions == null || taskDefinitions.isEmpty()) {
            return null;
        }
        TaskDefinition definition = findMavenDefinition(taskDefinitions);
        if (definition == null) {
            definition = findGradleDefinition(taskDefinitions);
        }
        return definition;
    }

    /**
     * @return Maven task if found, null if not.
     */
    @Nullable
    public static TaskDefinition findGradleDefinition(List<TaskDefinition> taskDefinitions) {
        if (taskDefinitions != null) {
            for (TaskDefinition definition : taskDefinitions) {
                if (StringUtils.endsWith(definition.getPluginKey(), ArtifactoryGradleTask.TASK_NAME)) {
                    return definition;
                }
            }
        }
        return null;
    }

    /**
     * @return Gradle task if found, null if not.
     */
    @Nullable
    public static TaskDefinition findMavenDefinition(List<TaskDefinition> taskDefinitions) {
        if (taskDefinitions != null) {
            for (TaskDefinition definition : taskDefinitions) {
                if (StringUtils.endsWith(definition.getPluginKey(), ArtifactoryMaven3Task.TASK_NAME)) {
                    return definition;
                }
            }
        }
        return null;
    }
}
