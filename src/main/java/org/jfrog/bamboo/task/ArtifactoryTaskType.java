package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.utils.process.ExternalProcess;
import org.jfrog.bamboo.context.AbstractBuildContext;

/**
 * Common super type for all tasks
 *
 * @author Tomer Cohen
 */
public abstract class ArtifactoryTaskType implements TaskType {

    private final TestCollationService testCollationService;

    protected ArtifactoryTaskType(TestCollationService testCollationService) {
        this.testCollationService = testCollationService;
    }

    /**
     * Get the build logger that will print messages to Bamboo's log from the context.
     *
     * @param taskContext The task context.
     * @return The build logger.
     */
    public BuildLogger getBuildLogger(TaskContext taskContext) {
        return taskContext.getBuildLogger();
    }

    /**
     * Get the executable to run for the build based upon the build's context.
     *
     * @param buildContext The build context.
     * @return The path to the executable to run for the build
     * @throws TaskException Thrown if the path to the executable defined in the build's {@link
     *                       com.atlassian.bamboo.v2.build.agent.capability.Capability} does not exist.
     */
    public abstract String getExecutable(AbstractBuildContext buildContext) throws TaskException;

    public TaskResult collectTestResults(AbstractBuildContext buildContext, TaskContext taskContext,
            ExternalProcess process) {
        TaskResultBuilder builder = TaskResultBuilder.create(taskContext).checkReturnCode(process);
        if (buildContext.isTestChecked() && buildContext.getTestDirectory() != null) {
            testCollationService.collateTestResults(taskContext, buildContext.getTestDirectory());
            builder.checkTestFailures();
        }
        return builder.build();
    }
}
