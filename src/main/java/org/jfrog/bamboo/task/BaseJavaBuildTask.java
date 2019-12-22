package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.*;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.builder.MavenAndIvyBuildInfoDataHelperBase;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.BuildInfoFields;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.configuration.BuildParamsOverrideManager.OVERRIDE_JDK_ENV_VAR_KEY;
import static org.jfrog.bamboo.configuration.BuildParamsOverrideManager.SHOULD_OVERRIDE_JDK_KEY;

/**
 * Common super type for all tasks
 *
 * @author Tomer Cohen
 */
public abstract class BaseJavaBuildTask extends ArtifactoryTaskType {
    protected static final String JDK_LABEL_KEY = "system.jdk.";
    public static final String JAVA_HOME = "JAVA_HOME";

    protected Map<String, String> environmentVariables;
    protected final EnvironmentVariableAccessor environmentVariableAccessor;
    private final TestCollationService testCollationService;
    protected BuildParamsOverrideManager buildParamsOverrideManager;
    protected CustomVariableContext customVariableContext;
    private final ProcessService processService;
    String buildInfoPropertiesFile;
    boolean activateBuildInfoRecording;

    protected BaseJavaBuildTask(TestCollationService testCollationService,
                                EnvironmentVariableAccessor environmentVariableAccessor, ProcessService processService) {
        ContainerManager.autowireComponent(this);
        this.testCollationService = testCollationService;
        this.processService = processService;
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    void initEnvironmentVariables(AbstractBuildContext buildContext) {
        environmentVariables = TaskUtils.getEnvironmentVariables(buildContext, environmentVariableAccessor);
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

    public TaskResult collectTestResults(AbstractBuildContext buildContext, TaskContext taskContext,
                                         ExternalProcess process) {
        TaskResultBuilder builder = TaskResultBuilder.newBuilder(taskContext).checkReturnCode(process);
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
    protected String getConfiguredJdkPath(BuildParamsOverrideManager buildParamsOverrideManager, AbstractBuildContext context,
                                          CapabilityContext capabilityContext) {
        // If the relevant Bamboo variables have been configured, read the build JDK from the configured
        if (shouldOverrideJdk()) {
            String jdkEnvVarName = buildParamsOverrideManager.getOverrideValue(OVERRIDE_JDK_ENV_VAR_KEY);
            if (StringUtils.isEmpty(jdkEnvVarName)) {
                jdkEnvVarName = JAVA_HOME;
            }
            String envVarValue = environmentVariables.get(jdkEnvVarName);
            if (envVarValue == null) {
                throw new RuntimeException("The task is configured to use the '" + jdkEnvVarName + "' environment variable for the build JDK, but this environment variable is not defined.");
            }
            return getPathBuilder(envVarValue).toString();
        }

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

    private boolean shouldOverrideJdk() {
        return Boolean.valueOf(buildParamsOverrideManager.getOverrideValue(SHOULD_OVERRIDE_JDK_KEY));

    }

    @NotNull
    ExternalProcess getExternalProcess(@NotNull TaskContext taskContext, File rootDirectory, List<String> command, Map<String, String> environmentVariables) {
        com.atlassian.bamboo.process.ExternalProcessBuilder processBuilder =
                new ExternalProcessBuilder().workingDirectory(rootDirectory).command(command).env(environmentVariables);
        return processService.createExternalProcess(taskContext, processBuilder);
    }

    void executeExternalProcess(BuildLogger logger, ExternalProcess process, Logger log) {
        process.execute();
        if (process.getHandler() != null && !process.getHandler().succeeded()) {
            String externalProcessOutput = getErrorMessage(process);
            logger.addBuildLogEntry(externalProcessOutput);
            log.debug("Process command error: " + externalProcessOutput);
        }
    }

    void addGeneratedBuildInfoToAggregatedBuildInfo(@NotNull TaskContext taskContext) throws TaskException {
        String generatedBuildInfo = environmentVariables.get(BuildInfoFields.GENERATED_BUILD_INFO);
        try {
            TaskUtils.addBuildInfoFromFileToContext(taskContext, generatedBuildInfo);
        } catch (Exception ex) {
            throw new TaskException("Failed to add Build Info to context.", ex);
        }
    }

    @NotNull
    Map<String, String> getCombinedConfiguration(TaskContext context) {
        Map<String, String> combinedMap = Maps.newHashMap();
        combinedMap.putAll(context.getConfigurationMap());
        BuildContext parentBuildContext = context.getBuildContext().getParentBuildContext();
        if (parentBuildContext != null) {
            Map<String, String> customBuildData = parentBuildContext.getBuildResult().getCustomBuildData();
            combinedMap.putAll(customBuildData);
        }
        return combinedMap;
    }

    void createBuildInfoFiles(boolean shouldCaptureBuildInfo, MavenAndIvyBuildInfoDataHelperBase dataHelper) throws TaskException {
        try {
            if (shouldCaptureBuildInfo) {
                String buildInfoJsonPath = dataHelper.createBuildInfoJSonFileAndGetItsPath();
                environmentVariables.put(BuildInfoFields.GENERATED_BUILD_INFO, buildInfoJsonPath);
            }
            buildInfoPropertiesFile = dataHelper.createBuildInfoPropsFileAndGetItsPath();
        } catch (IOException e) {
            throw new TaskException("Failed to create Build Info properties file.", e);
        }
        if (StringUtils.isNotBlank(buildInfoPropertiesFile)) {
            activateBuildInfoRecording = true;
        }
    }
}
