package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.ErrorLogEntry;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuilderDependencyHelper;
import org.jfrog.bamboo.builder.MavenDataHelper;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.context.PackageManagersContext;
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
public class ArtifactoryMaven3Task extends BaseJavaBuildTask {
    public static final String TASK_NAME = "maven3Task";

    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final BuilderDependencyHelper dependencyHelper;
    private final CapabilityContext capabilityContext;
    private Maven3BuildContext mavenBuildContext;
    private MavenDataHelper mavenDataHelper;
    private String artifactoryPluginVersion;
    private String mavenDependenciesDir;

    public ArtifactoryMaven3Task(final ProcessService processService,
                                 final EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext,
                                 TestCollationService testCollationService) {

        super(testCollationService, environmentVariableAccessor, processService);
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        dependencyHelper = new BuilderDependencyHelper("artifactoryMaven3Builder");
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @Override
    protected void initTask(@NotNull CommonTaskContext taskContext) {
        super.initTask(taskContext);
        artifactoryPluginVersion = Utils.getPluginVersion(pluginAccessor);
        mavenBuildContext = createBuildContext(taskContext);
        initEnvironmentVariables(mavenBuildContext);
        aggregateBuildInfo = mavenBuildContext.shouldAggregateBuildInfo(taskContext);
        mavenDataHelper = new MavenDataHelper(buildParamsOverrideManager, (TaskContext) taskContext,
                mavenBuildContext, environmentVariableAccessor, artifactoryPluginVersion, aggregateBuildInfo);
    }

    @Override
    @NotNull
    public TaskResult runTask(@NotNull TaskContext taskContext) throws TaskException {
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + artifactoryPluginVersion);
        final ErrorMemorisingInterceptor errorLines = new ErrorMemorisingInterceptor();
        logger.getInterceptorStack().add(errorLines);

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
            // Save config to buildinfo.properties.
            createBuildInfoFiles(aggregateBuildInfo, mavenDataHelper);
            mavenDataHelper.addPasswordsSystemProps(systemProps, mavenBuildContext, taskContext);
        } else {
            // If buildinfo.properties was not created, cannot collect build info.
            aggregateBuildInfo = false;
        }

        String subDirectory = mavenBuildContext.getWorkingSubDirectory();
        if (StringUtils.isNotBlank(subDirectory)) {
            rootDirectory = new File(rootDirectory, subDirectory);
        }

        // Create maven command.
        String mavenHome = getMavenHome(mavenBuildContext);
        if (StringUtils.isBlank(mavenHome)) {
            log.error(logger.addErrorLogEntry("Maven home is not defined!"));
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }
        List<String> command = buildCommand(mavenHome, rootDirectory, systemProps);

        ExternalProcess process = getExternalProcess(taskContext, rootDirectory, command, environmentVariables);
        try {
            executeExternalProcess(logger, process, log);
            if (aggregateBuildInfo) {
                convertGeneratedBuildInfoToBuild();
            }
            return collectTestResults(mavenBuildContext, taskContext, process);
        } finally {
            taskContext.getBuildContext().getBuildResult().addBuildErrors(errorLines.getErrorStringList());
        }
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        ServerConfig config = mavenDataHelper.getDeployServer();
        if (config == null) {
            config = mavenDataHelper.getResolveServer();
        }
        return config;
    }

    @Override
    protected String getTaskUsageName() {
        return "maven";
    }

    /**
     * Returns the path of the java executable of the select JDK
     *
     * @return Java bin path
     */
    public String getJavaExecutable(PackageManagersContext context) throws TaskException {
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

    private List<String> buildCommand(String mavenHome, File rootDirectory, List<String> systemProps) throws TaskException {
        List<String> command = getCommand(mavenBuildContext);
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
        return command;
    }

    private Maven3BuildContext createBuildContext(CommonTaskContext context) {
        Map<String, String> combinedMap = getCombinedConfiguration(context);
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
        String executable = getJavaExecutable(mavenBuildContext);
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

        /*
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

        if (artifactoryServerId == -1 && !aggregateBuildInfo) {
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
