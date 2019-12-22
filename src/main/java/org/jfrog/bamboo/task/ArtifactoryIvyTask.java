package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.ErrorLogEntry;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuilderDependencyHelper;
import org.jfrog.bamboo.builder.IvyDataHelper;
import org.jfrog.bamboo.builder.MavenAndIvyBuildInfoDataHelperBase;
import org.jfrog.bamboo.context.IvyBuildContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.PluginProperties;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Invocation of the Ant/Ivy task.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryIvyTask extends BaseJavaBuildTask {
    public static final String TASK_NAME = "artifactoryIvyTask";
    public static final String EXECUTABLE_NAME = SystemUtils.IS_OS_WINDOWS ? "ant.bat" : "ant";
    private static final Logger log = Logger.getLogger(ArtifactoryIvyTask.class);
    private static final String IVY_KEY = "system.builder.ivy.";
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;
    private BuilderDependencyHelper dependencyHelper;
    private String ivyDependenciesDir = "";
    private String buildInfoPropertiesFile = "";
    private IvyBuildContext ivyBuildContext;
    private BuildLogger logger;
    private MavenAndIvyBuildInfoDataHelperBase ivyDataHelper;
    private String artifactoryPluginVersion;

    public ArtifactoryIvyTask(final ProcessService processService,
                              final EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext,
                              TestCollationService testCollationService) {
        super(testCollationService, environmentVariableAccessor, processService);
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        dependencyHelper = new BuilderDependencyHelper("artifactoryIvyBuilder");
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @Override
    protected void initTask(@NotNull TaskContext context) {
        logger = getBuildLogger(context);
        Map<String, String> combinedMap = Maps.newHashMap();
        combinedMap.putAll(context.getConfigurationMap());
        combinedMap.putAll(context.getBuildContext().getBuildDefinition().getCustomConfiguration());
        ivyBuildContext = new IvyBuildContext(combinedMap);
        initEnvironmentVariables(ivyBuildContext);
        artifactoryPluginVersion = Utils.getPluginVersion(pluginAccessor);
        ivyDataHelper = new IvyDataHelper(buildParamsOverrideManager, context, ivyBuildContext, environmentVariableAccessor, artifactoryPluginVersion);
    }

    @Override
    @NotNull
    public TaskResult runTask(@NotNull TaskContext context) throws TaskException {
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + artifactoryPluginVersion);
        final ErrorMemorisingInterceptor errorLines = new ErrorMemorisingInterceptor();
        logger.getInterceptorStack().add(errorLines);

        File rootDirectory = context.getRootDirectory();
        long serverId = ivyBuildContext.getArtifactoryServerId();
        try {
            ivyDependenciesDir = extractIvyDependencies(serverId, rootDirectory, ivyBuildContext);
            log.info(logger.addBuildLogEntry("Ivy dependency directory found at: " + ivyDependenciesDir));
        } catch (IOException ioe) {
            ivyDependenciesDir = null;
            logger.addBuildLogEntry(new ErrorLogEntry(
                    "Error occurred while preparing Artifactory Ivy Runner dependencies. Build Info support is " +
                            "disabled: " + ioe.getMessage()));
            log.error("Error occurred while preparing Artifactory Ivy Runner dependencies. " +
                    "Build Info support is disabled.", ioe);
        }
        if (ivyDependenciesDir == null) {
            String message = "Ivy dependency directory not found.";
            logger.addErrorLogEntry(message);
            log.error(message);
        }
        boolean aggregateBuildInfo = ivyBuildContext.shouldAggregateBuildInfo(context, serverId);

        String executable = TaskUtils.getExecutablePath(ivyBuildContext, capabilityContext, IVY_KEY, EXECUTABLE_NAME, TASK_NAME);
        if (StringUtils.isBlank(executable)) {
            log.error(logger.addErrorLogEntry("Cannot find ivy executable"));
            return TaskResultBuilder.newBuilder(context).failed().build();
        }

        if (StringUtils.isNotBlank(ivyDependenciesDir)) {
            // Save data to buildinfo.properties.
            createBuildInfoFiles(aggregateBuildInfo, ivyDataHelper);
        }
        List<String> command = Lists.newArrayList(executable);
        if (activateBuildInfoRecording) {
            command.add("-lib");
            command.add(Commandline.quoteArgument(ivyDependenciesDir));
            command.add("-listener");
            command.add(Commandline.quoteArgument("org.jfrog.build.extractor.listener.ArtifactoryBuildListener"));
            TaskUtils.appendBuildInfoPropertiesArgument(command, buildInfoPropertiesFile);
            ivyDataHelper.addPasswordsSystemProps(command, ivyBuildContext, context);
        }
        String buildFile = ivyBuildContext.getBuildFile();
        if (StringUtils.isNotBlank(buildFile)) {
            command.addAll(Arrays.asList("-f", buildFile));
        }
        String targets = ivyBuildContext.getTargets();
        if (StringUtils.isNotBlank(targets)) {
            String[] targetTokens = StringUtils.split(targets, ' ');
            command.addAll(Arrays.asList(targetTokens));
        }

        String subDirectory = ivyBuildContext.getWorkingSubDirectory();
        if (StringUtils.isNotBlank(subDirectory)) {
            rootDirectory = new File(rootDirectory, subDirectory);
        }

        String antOpts = ivyBuildContext.getAntOpts();
        if (StringUtils.isNotBlank(antOpts)) {
            environmentVariables.put("ANT_OPTS", antOpts);
        }

        // Override the JAVA_HOME according to the build configuration:
        String jdkPath = getConfiguredJdkPath(buildParamsOverrideManager, ivyBuildContext, capabilityContext);
        environmentVariables.put("JAVA_HOME", jdkPath);

        ExternalProcess process = getExternalProcess(context, rootDirectory, command, environmentVariables);

        try {
            executeExternalProcess(logger, process, log);
            if (aggregateBuildInfo) {
                addGeneratedBuildInfoToAggregatedBuildInfo(context);
            }
            return TaskResultBuilder.newBuilder(context)
                    .checkReturnCode(process).build();
        } finally {
            context.getBuildContext().getBuildResult().addBuildErrors(errorLines.getErrorStringList());
        }
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return ivyDataHelper.getDeployServer();
    }

    @Override
    protected String getTaskUsageName() {
        return "ivy";
    }

    @Override
    protected Log getLog() {
        return new BuildInfoLog(log, logger);
    }

    /**
     * Extracts the Artifactory Ivy recorder and all the needed to dependencies
     *
     * @return Path of recorder and dependency jar folder if extraction succeeded. Null if not
     */
    private String extractIvyDependencies(long artifactoryServerId, File rootDirectory, IvyBuildContext context)
            throws IOException {

        if (artifactoryServerId == -1) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(rootDirectory, context,
                PluginProperties.getPluginProperty(PluginProperties.IVY_DEPENDENCY_FILENAME_KEY));
    }
}
