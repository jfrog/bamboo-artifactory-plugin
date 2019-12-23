package org.jfrog.bamboo.task;

import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.generic.GenericData;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.IOException;

/**
 * Created by Bar Belity on 02/12/2019.
 */
public abstract class ArtifactoryTaskType extends ArtifactoryTaskBase implements TaskType {
    // Build object created by the Artifactory task, will be aggregated to the plan's build-info.
    protected Build taskBuildInfo;

    protected abstract TaskResult runTask(@NotNull TaskContext context) throws TaskException;

    protected abstract void initTask(@NotNull TaskContext context) throws TaskException;

    public TaskResult execute(@NotNull TaskContext context) throws TaskException {
        // Remove aggregated build-info from plan context.
        // This is done in order to prevent it from being added as an environment-variable when a task
        // collecting environment variables is executed.
        String buildInfoFromContext = "";
        if (shouldRemoveBuildInfoFromContext()) {
            buildInfoFromContext = TaskUtils.getAndDeleteAggregatedBuildInfo(context);
        }

        // Initialize task.
        initTask(context);

        // Report task usage to Artifactory.
        ServerConfig server = getUsageServerConfig();
        if (server != null) {
            reportUsage(server, getTaskUsageName(), getLog());
        }

        // Run task execution.
        TaskResult taskResult = runTask(context);

        if (StringUtils.isNotBlank(buildInfoFromContext) || taskBuildInfo != null) {
            // Append build-infos and add back to plan's context.
            aggregateBuildInfoAndAddToPlanContext(buildInfoFromContext, context);
        }

        return taskResult;
    }

    private void aggregateBuildInfoAndAddToPlanContext(String buildInfoFromContext, TaskContext context) throws TaskException {
        GenericData gd = new GenericData();
        try {
            if (StringUtils.isNotBlank(buildInfoFromContext)) {
                gd = BuildInfoExtractorUtils.jsonStringToGeneric(buildInfoFromContext, GenericData.class);
            }
            if (taskBuildInfo != null) {
                gd.addBuild(taskBuildInfo);
            }
            String aggregatedBuildInfo = BuildInfoExtractorUtils.buildInfoToJsonString(gd);
            TaskUtils.addBuildInfoToContext(context, aggregatedBuildInfo);
        } catch (IOException ex) {
            throw new TaskException("Failed to add Build Info to context.", ex);
        }
    }

    protected boolean shouldRemoveBuildInfoFromContext() {
        return true;
    }
}
