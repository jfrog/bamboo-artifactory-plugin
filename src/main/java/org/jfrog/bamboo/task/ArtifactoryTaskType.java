package org.jfrog.bamboo.task;

import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskType;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;

/**
 * Created by Bar Belity on 02/12/2019.
 */
public abstract class ArtifactoryTaskType extends ArtifactoryTaskBase implements TaskType {

    protected abstract TaskResult runTask(@NotNull TaskContext context) throws TaskException;

    protected abstract void initTask(@NotNull TaskContext context) throws TaskException;

    public TaskResult execute(@NotNull TaskContext context) throws TaskException {
        // Initialize task.
        initTask(context);

        // Report task usage to Artifactory.
        ServerConfig server = getUsageServerConfig();
        if (server != null) {
            reportUsage(server, getTaskUsageName(), getLog());
        }

        // Run task execution.
        return runTask(context);
    }
}
