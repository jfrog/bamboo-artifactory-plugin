package org.jfrog.bamboo.util;

import com.atlassian.bamboo.task.TaskDefinition;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.task.ArtifactoryGradleTask;
import org.jfrog.bamboo.task.ArtifactoryMaven3Task;
import org.jfrog.bamboo.task.ArtifactoryPublishBuildInfoTask;

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
     * @return Artifactory Gradle TaskDefinition if found, null if not.
     */
    @Nullable
    public static TaskDefinition findGradleDefinition(List<? extends TaskDefinition> taskDefinitions) {
        if (taskDefinitions != null) {
            for (TaskDefinition definition : taskDefinitions) {
                if (definition.isEnabled()) {
                    if (StringUtils.endsWith(definition.getPluginKey(), ArtifactoryGradleTask.TASK_NAME)) {
                        return definition;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return Artifactory Maven TaskDefinition if found, null if not.
     */
    @Nullable
    public static TaskDefinition findMavenDefinition(List<? extends TaskDefinition> taskDefinitions) {
        if (taskDefinitions != null) {
            for (TaskDefinition definition : taskDefinitions) {
                if (definition.isEnabled()) {
                    if (StringUtils.endsWith(definition.getPluginKey(), ArtifactoryMaven3Task.TASK_NAME)) {
                        return definition;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return Artifactory Maven TaskDefinition if found, null if not.
     */
    @Nullable
    public static TaskDefinition findReleaseTaskDefinition(List<? extends TaskDefinition> taskDefinitions) {
        if (taskDefinitions != null) {
            for (TaskDefinition taskDefinition : taskDefinitions) {
                if (taskDefinition.isEnabled() && isReleaseMgmtEnabled(taskDefinition)) {
                   return taskDefinition;
                }
            }
        }
        return null;
    }

    /**
     * @return true if Release management is enabled, false otherwise.
     */
    public static boolean isReleaseMgmtEnabled(TaskDefinition taskDefinition) {
        AbstractBuildContext config = AbstractBuildContext.createContextFromMap(taskDefinition.getConfiguration());
        // Check if release management is enabled
        return (config != null) && config.releaseManagementContext.isReleaseMgmtEnabled();
    }

    /**
     * @return True if a Publish Build Info task exists in the plan. Otherwise, false.
     */
    @Nullable
    public static boolean isBuildPublishTaskExists(List<? extends TaskDefinition> taskDefinitions) {
        if (taskDefinitions != null) {
            for (TaskDefinition definition : taskDefinitions) {
                if (definition.isEnabled() && StringUtils.endsWith(definition.getPluginKey(), ArtifactoryPublishBuildInfoTask.TASK_NAME)) {
                    return true;
                }
            }
        }
        return false;
    }
}
