package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.utils.process.*;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.context.AbstractBuildContext;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Common super type for all tasks
 *
 * @author Tomer Cohen
 */
public abstract class ArtifactoryTaskType implements TaskType {

    protected static final String JDK_LABEL_KEY = "system.jdk.";
    protected Map<String, String> environmentVariables;
    protected final EnvironmentVariableAccessor environmentVariableAccessor;
    private final TestCollationService testCollationService;

    protected ArtifactoryTaskType(TestCollationService testCollationService,
        EnvironmentVariableAccessor environmentVariableAccessor) {

        this.testCollationService = testCollationService;
        this.environmentVariableAccessor = environmentVariableAccessor;
    }

    protected void initEnvironmentVariables(AbstractBuildContext buildContext) {
        Map<String, String> env = Maps.newHashMap();
        env.putAll(environmentVariableAccessor.getEnvironment());
        if (StringUtils.isNotBlank(buildContext.getEnvironmentVariables())) {
            env.putAll(environmentVariableAccessor
                    .splitEnvironmentAssignments(buildContext.getEnvironmentVariables(), false));
        }

        environmentVariables = env;
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

    /**
     * Get the path to the JDK according to the build configuration.
     *
     * @param context           The build context which is defined for the current build environment.
     * @param capabilityContext The capability context of the build.
     * @return                  The path to the Java home.
     */
    protected String getConfiguredJdkPath(AbstractBuildContext context, CapabilityContext capabilityContext) {
        String jdkCapabilityKey = JDK_LABEL_KEY + context.getJdkLabel();
        ReadOnlyCapabilitySet capabilitySet = capabilityContext.getCapabilitySet();
        if (capabilitySet == null) {
            return null;
        }
        Capability capability = capabilitySet.getCapability(jdkCapabilityKey);
        String jdkHome;
        if (capability != null) {
            jdkHome = capability.getValue();
        } else {
            return null;
        }
        if (StringUtils.isBlank(jdkHome)) {
            return null;
        }
        StringBuilder binPathBuilder = getPathBuilder(jdkHome);
        return binPathBuilder.toString();
    }

    /**
     * Returns a {@link StringBuilder} starting with a given base path and ending with a file-system separator
     *
     * @param basePath Base path
     * @return String builder
     */
    public StringBuilder getPathBuilder(String basePath) {
        StringBuilder confPathBuilder = new StringBuilder(basePath);
        if (!basePath.endsWith(File.separator)) {
            confPathBuilder.append(File.separator);
        }
        return confPathBuilder;
    }

    /**
     * @return The canonical path for a path.
     */
    public String getCanonicalPath(String path) {
        if (StringUtils.contains(path, " ")) {
            try {
                File f = new File(path);
                path = f.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException("IO Exception trying to get canonical path of item: " + path, e);
            }
        }
        return path;
    }

    /**
     * Check if the external process got exception or some errors
     *
     * @param process - Bamboo external process
     * @return full message of errors, if exists
     */
    public String getErrorMessage(ExternalProcess process) {
        ProcessHandler handler = process.getHandler();
        String commandLine = process.getCommandLine();
        StringBuilder message = new StringBuilder();

        if (handler.getException() != null) {
            message.append("Exception executing command \"")
                    .append(commandLine).append(" \n")
                    .append(handler.getException().getMessage()).append("\n")
                    .append(handler.getException()).append("\n");
        }

        String reason = null;
        if (handler instanceof PluggableProcessHandler) {
            OutputHandler errorHandler = ((PluggableProcessHandler) handler).getErrorHandler();
            if (errorHandler instanceof StringOutputHandler) {
                StringOutputHandler errorStringHandler = (StringOutputHandler) errorHandler;
                if (errorStringHandler.getOutput() != null) {
                    reason = errorStringHandler.getOutput();
                }
            }
        }
        if (reason != null && reason.trim().length() > 0) {
            message.append("Error executing command \"").append(commandLine).append("\": ").append(reason);
        }

        return message.toString();
    }
}
