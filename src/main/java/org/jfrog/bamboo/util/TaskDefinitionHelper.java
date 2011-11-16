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
     * Find a Maven class defined by {@link ArtifactoryMaven3Task} or a Gradle task type {@link ArtifactoryGradleTask}
     * if not found in the list of task definitions return null.
     *
     * @param taskDefinitions The list of of task definitions to find the required task.
     * @return A Maven or Gradle task that is defined in the plan.
     */
    @Nullable
    public static TaskDefinition findMavenOrGradleTask(List<TaskDefinition> taskDefinitions) {
        if (taskDefinitions == null || taskDefinitions.isEmpty()) {
            return null;
        }
        TaskDefinition definition = findMavenBuild(taskDefinitions);
        if (definition == null) {
            definition = findGradleBuild(taskDefinitions);
        }
        return definition;
    }

    /**
     * @return True if this task is of type {@link ArtifactoryGradleTask}
     */
    @Nullable
    public static TaskDefinition findGradleBuild(List<TaskDefinition> taskDefinitions) {
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
     * @return True if this task is of type {@link ArtifactoryMaven3Task}
     */
    @Nullable
    public static TaskDefinition findMavenBuild(List<TaskDefinition> taskDefinitions) {
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
