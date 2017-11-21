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
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.builder.ArtifactoryBuildInfoDataHelper;
import org.jfrog.bamboo.builder.BuilderDependencyHelper;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.util.MavenDataHelper;
import org.jfrog.bamboo.util.PluginProperties;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Invocation of the Maven 3 task
 *
 * @author Tomer Cohen
 */
public class ArtifactoryMaven3Task extends ArtifactoryTaskType {
    public static final String TASK_NAME = "maven3Task";
    private static final Logger log = Logger.getLogger(ArtifactoryMaven3Task.class);
    private final ProcessService processService;
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;
    private BuilderDependencyHelper dependencyHelper;
    private String mavenDependenciesDir;
    private String buildInfoPropertiesFile;
    private boolean activateBuildInfoRecording;

    public ArtifactoryMaven3Task(final ProcessService processService,
                                 final EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext,
                                 TestCollationService testCollationService) {
        super(testCollationService, environmentVariableAccessor);
        this.processService = processService;
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        dependencyHelper = new BuilderDependencyHelper("artifactoryMaven3Builder");
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @Override
    @NotNull
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        BuildLogger logger = getBuildLogger(taskContext);
        String artifactoryPluginVersion = getArtifactoryVersion();
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + artifactoryPluginVersion);
        final ErrorMemorisingInterceptor errorLines = new ErrorMemorisingInterceptor();
        logger.getInterceptorStack().add(errorLines);

        Maven3BuildContext mavenBuildContext = createBuildContext(taskContext);
        initEnvironmentVariables(mavenBuildContext);

        long serverId = mavenBuildContext.getResolutionArtifactoryServerId();
        if (serverId == -1) {
            serverId = mavenBuildContext.getArtifactoryServerId();
        }
        File rootDirectory = taskContext.getRootDirectory();
        try {
            mavenDependenciesDir = extractMaven3Dependencies(rootDirectory, serverId, mavenBuildContext);
        } catch (IOException e) {
            mavenDependenciesDir = null;
            logger.addBuildLogEntry(new ErrorLogEntry(
                    "Error occurred while preparing Artifactory Maven Runner dependencies. Build Info support is " +
                            "disabled: " + e.getMessage()));
            log.error("Error occurred while preparing Artifactory Maven Runner dependencies. " +
                    "Build Info support is disabled.", e);
        }
        List<String> systemProps = new ArrayList<>();
        if (StringUtils.isNotBlank(mavenDependenciesDir)) {
            ArtifactoryBuildInfoDataHelper mavenDataHelper = new MavenDataHelper(buildParamsOverrideManager, taskContext,
                    mavenBuildContext, environmentVariableAccessor, artifactoryPluginVersion);
            try {
                buildInfoPropertiesFile = mavenDataHelper.createBuildInfoPropsFileAndGetItsPath();
            } catch (IOException e) {
                throw new TaskException("Failed to create Build Info properties file.", e);
            }
            if (StringUtils.isNotBlank(buildInfoPropertiesFile)) {
                activateBuildInfoRecording = true;
            }
            mavenDataHelper.addPasswordsSystemProps(systemProps, mavenBuildContext);
        }
        String subDirectory = mavenBuildContext.getWorkingSubDirectory();
        if (StringUtils.isNotBlank(subDirectory)) {
            rootDirectory = new File(rootDirectory, subDirectory);
        }

        List<String> command = getCommand(mavenBuildContext);
        String mavenHome = getMavenHome(mavenBuildContext);
        if (StringUtils.isBlank(mavenHome)) {
            log.error(logger.addErrorLogEntry("Maven home is not defined!"));
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }
        appendClassPathArguments(command, mavenHome);
        appendClassWorldsConfArgument(command, mavenHome);
        appendBuildInfoPropertiesArgument(command);
        appendMavenOpts(command, mavenBuildContext);
        addMavenHome(command, mavenHome);
        addMavenMultiModuleProjectPath(command, rootDirectory);
        command.add("org.codehaus.plexus.classworlds.launcher.Launcher");

        appendGoals(command, mavenBuildContext);
        appendAdditionalMavenParameters(command, mavenBuildContext);

        log.debug("Running maven command: " + command.toString());
        command.addAll(systemProps);

        ExternalProcessBuilder processBuilder =
                new ExternalProcessBuilder().workingDirectory(rootDirectory).command(command).env(environmentVariables);

        try {
            Utils.hideExternalProcessBuilderLog(taskContext);
            ExternalProcess process = processService.createExternalProcess(taskContext, processBuilder);
            process.execute();

            if (process.getHandler() != null && !process.getHandler().succeeded()) {
                String externalProcessOutput = getErrorMessage(process);
                logger.addBuildLogEntry(externalProcessOutput);
                log.debug("Process command error: " + externalProcessOutput);
            }

            return collectTestResults(mavenBuildContext, taskContext, process);
        } finally {
            taskContext.getBuildContext().getBuildResult().addBuildErrors(errorLines.getErrorStringList());
        }
    }

    /**
     * Returns the path of the java executable of the select JDK
     *
     * @return Java bin path
     */
    public String getExecutable(AbstractBuildContext context) throws TaskException {
        String jdkPath = getConfiguredJdkPath(buildParamsOverrideManager, context, capabilityContext);
        StringBuilder binPathBuilder = new StringBuilder(jdkPath);
        if (SystemUtils.IS_OS_WINDOWS) {
            binPathBuilder.append("bin").append(File.separator).append("java.exe");
        } else {
            // IBM's AIX JDK has different locations
            String aixJdkLocation = "jre" + File.separator + "sh" + File.separator + "java";
            File aixJdk = new File(binPathBuilder.toString() + aixJdkLocation);
            if (aixJdk.isFile()) {
                binPathBuilder.append(aixJdkLocation);
            } else {
                binPathBuilder.append("bin").append(File.separator).append("java");
            }
        }
        String binPath = binPathBuilder.toString();
        binPath = getCanonicalPath(binPath);
        return binPath;
    }

    private Maven3BuildContext createBuildContext(TaskContext context) {
        Map<String, String> combinedMap = Maps.newHashMap();
        combinedMap.putAll(context.getConfigurationMap());
        BuildContext parentBuildContext = context.getBuildContext().getParentBuildContext();
        if (parentBuildContext != null) {
            Map<String, String> customBuildData = parentBuildContext.getBuildResult().getCustomBuildData();
            combinedMap.putAll(customBuildData);
        }
        return new Maven3BuildContext(combinedMap);
    }

    private void addMavenHome(List<String> command, String mavenHome) {
        command.add(Commandline.quoteArgument("-Dmaven.home" + "=" + mavenHome));
    }

    //Starting from Maven 3.3.3
    private void addMavenMultiModuleProjectPath(List<String> command, File rootDirectory) {
        command.add(Commandline.quoteArgument("-Dmaven.multiModuleProjectDirectory" + "=" + rootDirectory.getPath()));
    }

    private List<String> getCommand(Maven3BuildContext mavenBuildContext) throws TaskException {
        List<String> command = Lists.newArrayList();
        String executable = getExecutable(mavenBuildContext);
        if (StringUtils.isBlank(executable)) {
            log.error("No Maven executable found");
            return command;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
            command.add("call");
            command.add(Commandline.quoteArgument(executable));
        } else {
            command.add(Commandline.quoteArgument(executable));
        }
        return command;
    }

    private void appendMavenOpts(List<String> arguments, Maven3BuildContext mavenBuildContext) {
        String mavenOpts = mavenBuildContext.getMavenOpts();
        if (StringUtils.isNotBlank(mavenOpts)) {
            String[] mavenOptsToken = StringUtils.split(mavenOpts, " ");
            for (String opt : mavenOptsToken) {
                if (StringUtils.isNotBlank(opt)) {
                    arguments.add(Commandline.quoteArgument(opt));
                }
            }
        }
    }

    /**
     * Appends the maven classworlds classpath arguments to the command
     *
     * @param arguments     Aggregated command arguments
     * @param mavenHomePath Path to Maven installation home
     */
    private void appendClassPathArguments(List<String> arguments, String mavenHomePath) {
        arguments.add("-cp");

        StringBuilder classPathBuilder = getPathBuilder(mavenHomePath).append("boot");
        String mavenBootPath = classPathBuilder.toString();

        File mavenBootFolder = new File(mavenBootPath);
        if (!mavenBootFolder.isDirectory()) {
            throw new IllegalStateException("Could not find the Maven lib directory in the expected path: " +
                    mavenBootPath + ".");
        }
        String[] bootJars = mavenBootFolder.list();
        for (String bootJar : bootJars) {
            if (StringUtils.startsWithIgnoreCase(bootJar, "plexus-classworlds") &&
                    StringUtils.endsWithIgnoreCase(bootJar, ".jar")) {
                classPathBuilder.append(File.separator).append(bootJar);
                String classPath = getCanonicalPath(classPathBuilder.toString());
                arguments.add(Commandline.quoteArgument(classPath));
                return;
            }
        }
        throw new IllegalStateException("Could not find plexus classworlds jar in " + mavenBootPath + ".");
    }

    private void appendGoals(List<String> arguments, Maven3BuildContext context) {
        String goals = context.getGoals();
        if (context.releaseManagementContext.isActivateReleaseManagement()) {
            String altTasks = context.releaseManagementContext.getAlternativeTasks();
            if (StringUtils.isNotBlank(altTasks)) {
                goals = altTasks;
            }
        }
        goals = getStringWithoutNewLines(goals);
        String[] goalArray = StringUtils.split(goals, " ");
        arguments.addAll(Arrays.asList(goalArray));
    }

    /**
     * Appends the maven classworlds configuration file argument to the command
     *
     * @param arguments     Aggregated command arguments
     * @param mavenHomePath Path to Maven installation home
     */
    private void appendClassWorldsConfArgument(List<String> arguments, String mavenHomePath) {
        String originalConfPath = getPathBuilder(mavenHomePath).append("bin").append(File.separator).append("m2.conf").
                toString();

        String m2ConfPath;

        /**
         * Customize the classworlds conf to activate the build info recorder only if received a valid dependency
         * directory path
         */
        if (activateBuildInfoRecording) {
            try {
                List m2ConfLines = FileUtils.readLines(new File(originalConfPath), "utf-8");
                m2ConfLines.add("load " + mavenDependenciesDir + File.separator + "*.jar");

                File tempM2Conf = File.createTempFile("artifactoryM2", "conf");
                FileUtils.writeLines(tempM2Conf, m2ConfLines);
                m2ConfPath = tempM2Conf.getCanonicalPath();
            } catch (IOException ioe) {
                throw new RuntimeException("Error occurred while writing Maven 3 customized m2.conf", ioe);
            }
        } else {
            m2ConfPath = originalConfPath;
        }

        arguments.add(Commandline.quoteArgument("-Dclassworlds.conf=" + m2ConfPath));
    }

    private void appendBuildInfoPropertiesArgument(List<String> arguments) {
        if (activateBuildInfoRecording) {
            TaskUtils.appendBuildInfoPropertiesArgument(arguments, buildInfoPropertiesFile);
        }
    }


    /**
     * Extracts the Artifactory Maven 3 recorder and all the needed to dependencies
     *
     * @return Path of recorder and dependency jar folder if extraction succeeded. Null if not
     */
    private String extractMaven3Dependencies(File rootDir, long artifactoryServerId, Maven3BuildContext mavenBuildContext)
            throws IOException {

        if (artifactoryServerId == -1) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(rootDir, mavenBuildContext,
                PluginProperties.getPluginProperty(PluginProperties.MAVEN3_DEPENDENCY_FILENAME_KEY));
    }

    private void appendAdditionalMavenParameters(List<String> arguments, Maven3BuildContext context) {
        String additionalParams = context.getAdditionalMavenParams();
        if (StringUtils.isNotBlank(additionalParams)) {
            String formattedParams = getStringWithoutNewLines(additionalParams);
            String[] paramArray = StringUtils.split(formattedParams, " ");
            arguments.addAll(Arrays.asList(paramArray));
        }
        String projectFile = context.getProjectFile();
        if (StringUtils.isNotBlank(projectFile)) {
            arguments.addAll(Arrays.asList("-f", projectFile));
        }
    }

    private String getMavenHome(Maven3BuildContext context) {
        return capabilityContext.getCapabilityValue("system.builder.maven." + context.getExecutable());
    }

    private String getStringWithoutNewLines(String stringToModify) {
        return StringUtils.replaceChars(stringToModify, "\r\n", "  ");
    }
}
