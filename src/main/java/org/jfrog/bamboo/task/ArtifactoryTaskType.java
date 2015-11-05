package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.*;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.ConstantValues;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.bamboo.configuration.BuildParamsOverrideManager.SHOULD_OVERRIDE_JDK_KEY;
import static org.jfrog.bamboo.configuration.BuildParamsOverrideManager.OVERRIDE_JDK_ENV_VAR_KEY;

/**
 * Common super type for all tasks
 *
 * @author Tomer Cohen
 */
public abstract class ArtifactoryTaskType implements TaskType {
    protected static final String JDK_LABEL_KEY = "system.jdk.";
    public static final String JAVA_HOME = "JAVA_HOME";

    protected Map<String, String> environmentVariables;
    protected PluginAccessor pluginAccessor;
    protected final EnvironmentVariableAccessor environmentVariableAccessor;
    private final TestCollationService testCollationService;
    protected BuildParamsOverrideManager buildParamsOverrideManager;
    protected CustomVariableContext customVariableContext;

    protected ArtifactoryTaskType(TestCollationService testCollationService,
        EnvironmentVariableAccessor environmentVariableAccessor) {
        ContainerManager.autowireComponent(this);
        this.testCollationService = testCollationService;
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor){
        this.pluginAccessor = pluginAccessor;
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
            return envVarValue;
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

    public String getArtifactoryVersion(){
        Plugin plugin = pluginAccessor.getPlugin(ConstantValues.ARTIFACTORY_PLUGIN_KEY);
        if (plugin != null) {
            return plugin.getPluginInformation().getVersion();
        }
        return StringUtils.EMPTY;
    }

    private boolean shouldOverrideJdk() {
        return Boolean.valueOf(buildParamsOverrideManager.getOverrideValue(SHOULD_OVERRIDE_JDK_KEY));

    }
}
