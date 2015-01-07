package org.jfrog.bamboo.task;


import com.atlassian.bamboo.build.ErrorLogEntry;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.builder.ArtifactoryBuildInfoPropertyHelper;
import org.jfrog.bamboo.builder.BuilderDependencyHelper;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.IvyBuildContext;
import org.jfrog.bamboo.util.IvyPropertyHelper;
import org.jfrog.bamboo.util.PluginProperties;
import org.jfrog.build.api.BuildInfoConfigProperties;

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
public class ArtifactoryIvyTask extends ArtifactoryTaskType {
    public static final String EXECUTABLE_NAME = SystemUtils.IS_OS_WINDOWS ? "ant.bat" : "ant";
    private static final Logger log = Logger.getLogger(ArtifactoryIvyTask.class);
    private static final String IVY_KEY = "system.builder.ivy.";
    private final ProcessService processService;
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;
    private BuilderDependencyHelper dependencyHelper;
    private String ivyDependenciesDir = "";
    private String buildInfoPropertiesFile = "";
    private boolean activateBuildInfoRecording;


    public ArtifactoryIvyTask(final ProcessService processService,
            final EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext,
            TestCollationService testCollationService) {
        super(testCollationService, environmentVariableAccessor);
        this.processService = processService;
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        dependencyHelper = new BuilderDependencyHelper("artifactoryIvyBuilder");
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @Override
    @NotNull
    public TaskResult execute(@NotNull TaskContext context) throws TaskException {
        BuildLogger logger = getBuildLogger(context);
        final ErrorMemorisingInterceptor errorLines = new ErrorMemorisingInterceptor();
        logger.getInterceptorStack().add(errorLines);
        Map<String, String> combinedMap = Maps.newHashMap();
        combinedMap.putAll(context.getConfigurationMap());
        combinedMap.putAll(context.getBuildContext().getBuildDefinition().getCustomConfiguration());
        IvyBuildContext ivyBuildContext = new IvyBuildContext(combinedMap);
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
        String executable = getExecutable(ivyBuildContext);
        if (StringUtils.isBlank(executable)) {
            log.error(logger.addErrorLogEntry("Cannot find ivy executable"));
            return TaskResultBuilder.create(context).failed().build();
        }
        Map<String, String> globalEnv = environmentVariableAccessor.getEnvironment();
        Map<String, String> environment = Maps.newHashMap(globalEnv);
        if (StringUtils.isNotBlank(ivyDependenciesDir)) {
            ArtifactoryBuildInfoPropertyHelper propertyHelper = new IvyPropertyHelper();
            propertyHelper.init(context.getBuildContext());
            buildInfoPropertiesFile = propertyHelper.createFileAndGetPath(ivyBuildContext, context.getBuildLogger(),
                    environmentVariableAccessor.getEnvironment(context), globalEnv);
            if (StringUtils.isNotBlank(buildInfoPropertiesFile)) {
                activateBuildInfoRecording = true;
                environment.put(BuildInfoConfigProperties.PROP_PROPS_FILE, buildInfoPropertiesFile);
            }
        }
        List<String> command = Lists.newArrayList(executable);
        if (activateBuildInfoRecording) {
            command.add("-lib");
            command.add(Commandline.quoteArgument(ivyDependenciesDir));
            command.add("-listener");
            command.add(Commandline.quoteArgument("org.jfrog.build.extractor.listener.ArtifactoryBuildListener"));
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

        String antOpts = ivyBuildContext.getAntOpts();
        if (StringUtils.isNotBlank(antOpts)) {
            environment.put("ANT_OPTS", antOpts);
        }
        if (StringUtils.isNotBlank(ivyBuildContext.getEnvironmentVariables())) {
            environment.putAll(environmentVariableAccessor
                    .splitEnvironmentAssignments(ivyBuildContext.getEnvironmentVariables(), false));
        }
        String subDirectory = ivyBuildContext.getWorkingSubDirectory();
        if (StringUtils.isNotBlank(subDirectory)) {
            rootDirectory = new File(rootDirectory, subDirectory);
        }

        // Override the JAVA_HOME according to the build configuration:
        String jdkPath = getConfiguredJdkPath(context.getBuildContext(), ivyBuildContext, capabilityContext);
        environment.put("JAVA_HOME", jdkPath);

        log.debug("Running Ant command: " + command.toString());

        ExternalProcessBuilder processBuilder =
                new ExternalProcessBuilder().workingDirectory(rootDirectory).command(command)
                        .env(environment);
        try {

            ExternalProcess process = processService.createExternalProcess(context, processBuilder);
            process.execute();

            if (process.getHandler() != null && !process.getHandler().succeeded()) {
                String externalProcessOutput = getErrorMessage(process);
                logger.addBuildLogEntry(externalProcessOutput);
                log.debug("Process command error: " + externalProcessOutput);
            }

            return TaskResultBuilder.newBuilder(context)
                    .checkReturnCode(process).build();
        } finally {
            context.getBuildContext().getBuildResult().addBuildErrors(errorLines.getErrorStringList());
        }
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
                PluginProperties.IVY_DEPENDENCY_FILENAME_KEY);
    }

    public String getExecutable(AbstractBuildContext buildContext) throws TaskException {
        ReadOnlyCapabilitySet capabilitySet = capabilityContext.getCapabilitySet();
        if (capabilitySet == null) {
            return null;
        }
        Capability capability = capabilitySet.getCapability(IVY_KEY + buildContext.getExecutable());
        if (capability == null) {
            throw new TaskException("Ivy capability: " + buildContext.getExecutable() +
                    " is not defined, please check job configuration");
        }
        final String path = new StringBuilder(capability.getValue())
                .append(File.separator)
                .append("bin")
                .append(File.separator)
                .append(EXECUTABLE_NAME)
                .toString();

        if (!new File(path).exists()) {
            throw new TaskException("Executable '" + EXECUTABLE_NAME + "'  does not exist at path '" + path + "'");
        }

        return path;
    }
}
