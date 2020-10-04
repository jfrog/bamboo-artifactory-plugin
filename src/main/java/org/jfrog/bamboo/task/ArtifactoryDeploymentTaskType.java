package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.build.api.util.Log;

/**
 * Created by Bar Belity on 08/12/2019.
 */
public abstract class ArtifactoryDeploymentTaskType extends ArtifactoryTaskBase implements DeploymentTaskType {
    Logger log = Logger.getLogger(ArtifactoryDeploymentTaskType.class);
    BuildLogger buildLogger;
    Log buildInfoLog;

    protected abstract TaskResult runTask(@NotNull DeploymentTaskContext context) throws TaskException;

    protected void initTask(@NotNull DeploymentTaskContext context) {
        buildLogger = context.getBuildLogger();
        buildInfoLog = getLog();
    }

    @NotNull
    public TaskResult execute(@NotNull DeploymentTaskContext context) throws TaskException {
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
