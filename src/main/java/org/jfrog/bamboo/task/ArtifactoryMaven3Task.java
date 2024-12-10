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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuilderDependencyHelper;
import org.jfrog.bamboo.builder.MavenDataHelper;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.util.PluginProperties;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;

import static org.jfrog.bamboo.util.TaskUtils.getPlanKey;

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

    public ArtifactoryMaven3Task(final ProcessService processService,
                                 final EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext,
                                 TestCollationService testCollationService) {
        super(testCollationService, environmentVariableAccessor, processService);
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        this.dependencyHelper = new BuilderDependencyHelper("artifactoryMaven3Builder");
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @Override
    protected void initTask(@NotNull CommonTaskContext taskContext) throws TaskException {
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
        String mavenDependenciesDir;
        try {
            mavenDependenciesDir = extractMaven3Dependencies(serverId, mavenBuildContext);
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
        environmentVariables.put("MAVEN_HOME", mavenHome);

        String jdkPath = getConfiguredJdkPath(buildParamsOverrideManager, mavenBuildContext, capabilityContext);
        if (jdkPath == null) {
            throw new RuntimeException("JDK path is not configured. Please make sure the agent has a well configured JDK capability.");
        }
        List<String> command = buildCommand(mavenHome, jdkPath, rootDirectory, systemProps, mavenDependenciesDir);

        // Override the JAVA_HOME according to the build configuration.
        environmentVariables.put("JAVA_HOME", jdkPath);

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
    public String getJavaExecutable(String jdkPath) {
        StringBuilder binPathBuilder = new StringBuilder(jdkPath);
        if (SystemUtils.IS_OS_WINDOWS && !containerized) {
            binPathBuilder.append("bin").append(fileSeparator).append("java.exe");
        } else {
            // IBM's AIX JDK has different locations
            String aixJdkLocation = "jre" + fileSeparator + "sh" + File.separator + "java";
            File aixJdk = new File(binPathBuilder.toString() + aixJdkLocation);
            if (aixJdk.isFile()) {
                binPathBuilder.append(aixJdkLocation);
            } else {
                binPathBuilder.append("bin").append(fileSeparator).append("java");
            }
        }
        String binPath = binPathBuilder.toString();
        binPath = getCanonicalPath(binPath);
        return binPath;
    }

    private List<String> buildCommand(String mavenHome, String jdkPath, File rootDirectory, List<String> systemProps, String mavenDependenciesDir) {
        List<String> command = getCommand(jdkPath);
        appendClassPathArguments(command, mavenHome);
        addMavenHome(command, mavenHome);
        addMavenConf(command, mavenHome);
        addMavenPluginLib(command, mavenDependenciesDir);
        appendClassWorldsConfArgument(command, mavenHome);
        appendBuildInfoPropertiesArgument(command);
        appendMavenOpts(command, mavenBuildContext);
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

    private void addMavenConf(List<String> command, String mavenHome) {
        command.add(Commandline.quoteArgument("-Dmaven.conf" + "=" + mavenHome + fileSeparator + "conf"));
    }

    //Starting from Maven 3.3.3
    private void addMavenMultiModuleProjectPath(List<String> command, File rootDirectory) {
        command.add(Commandline.quoteArgument("-Dmaven.multiModuleProjectDirectory" + "=" + rootDirectory.getPath()));
    }

    private List<String> getCommand(String jdkPath) {
        List<String> command = Lists.newArrayList();
        String executable = getJavaExecutable(jdkPath);
        if (StringUtils.isBlank(executable)) {
            log.error("No Maven executable found");
            return command;
        }
        if (SystemUtils.IS_OS_WINDOWS && !containerized) {
            command.add("cmd.exe");
            command.add("/c");
            command.add("call");
        }
        command.add(Commandline.quoteArgument(executable));
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
        StringBuilder classPathBuilder = getPathBuilder(mavenHomePath).append("boot").append(fileSeparator).append("*");
        arguments.add(classPathBuilder.toString());
    }

    /**
     * Checks whether a specific configuration property in the Bamboo properties file
     * is set to enable deployment and appends the deploy goal to the Maven arguments list if necessary.
     *
     * The method first searches for a file named "artifactory.bamboo.plugin.properties" in the Bamboo Agent Home directory.
     * If the file is not found, it searches for the same file directly under the Bamboo Home directory.
     * If the file exists, it reads the property "artifactory.maven3.deploy.always" and checks if it is set to "true".
     * If the property is found and set to "true", the method appends the "deploy" goal to the provided arguments list.
     *
     * @param arguments The list of Maven arguments to which the deploy goal may be appended.
     */
    void appendDeployGoalIfNeeded(List<String> arguments) {
        // Check if the "deploy" phase already exists in the arguments list
        if (arguments.contains("deploy")) {
            return;
        }

        File propertiesFile = findPropertiesFile();

        if (propertiesFile == null || !propertiesFile.exists()) {
            logger.addBuildLogEntry("appendDeployGoalIfNeeded: Properties file not found in agent or server home directory.");
            return;
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
            properties.load(fis);
        } catch (IOException e) {
            logger.addBuildLogEntry("appendDeployGoalIfNeeded: Failed to read properties file: " + e.getMessage());
            return;
        }

        String deployAlwaysValue = properties.getProperty("artifactory.maven3.deploy.always");
        if ("true".equalsIgnoreCase(deployAlwaysValue)) {
            logger.addBuildLogEntry("appendDeployGoalIfNeeded: 'artifactory.maven3.deploy.always' is set to true. Appending deploy goal.");
            arguments.add("deploy");
        }
    }

    /**
     * Determines if the current build is running on a remote agent or the main Bamboo server
     * and searches for the properties file accordingly.
     *
     * If the build is on a remote agent, it checks the agent's home directory.
     * If the build is on the main server, it checks the Bamboo home directory using both the
     * system property "bamboo.home" and the environment variable "BAMBOO_HOME".
     *
     * @return The File object pointing to the properties file, or null if not found.
     */
    private File findPropertiesFile() {
        // Define possible locations for the properties file
        String[] possibleLocations = {
            // Bamboo Agent Home
            System.getenv("BAMBOO_AGENT_HOME"),
            // Bamboo Home from system property
            System.getProperty("bamboo.home"),
            // Bamboo Home from environment variable
            System.getenv("BAMBOO_HOME")
        };

        for (String location : possibleLocations) {
            if (location != null && !location.isEmpty()) {
                File propertiesFile = new File(location, "artifactory.bamboo.plugin.properties");
                if (propertiesFile.exists()) {
                    return propertiesFile;
                }
            }
        }

        return null;
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
        appendDeployGoalIfNeeded(arguments);
    }

    private void addMavenPluginLib(List<String> command, String mavenDependenciesDir) {
        if (!StringUtils.contains(mavenBuildContext.getMavenOpts(), "-Dm3plugin.lib")) {
            command.add("-Dm3plugin.lib=" + mavenDependenciesDir);
        }
    }

    /**
     * Appends the maven classworlds configuration file argument to the command
     *
     * @param arguments     - Aggregated command arguments
     * @param mavenHomePath - Path to Maven installation home
     */
    private void appendClassWorldsConfArgument(List<String> arguments, String mavenHomePath) {
        String classworldsConfPath;


        // Customize the classworlds conf to activate the build info recorder only if received a valid dependency directory path
        if (activateBuildInfoRecording) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("maven3/classworlds-freestyle.conf")) {
                if (is == null) {
                    throw new RuntimeException("Error occurred while writing Maven 3 customized m2.conf: 'maven3/classworlds-freestyle.conf' doesn't exist.");
                }
                File tempM2Conf = File.createTempFile("artifactoryM2", "conf", bambooTmp);
                Files.copy(is, tempM2Conf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                classworldsConfPath = tempM2Conf.toString();
            } catch (IOException ioe) {
                throw new RuntimeException("Error occurred while writing Maven 3 customized m2.conf", ioe);
            }
        } else {
            classworldsConfPath = getPathBuilder(mavenHomePath).append("bin").append(fileSeparator).append("m2.conf").toString();
        }

        arguments.add(Commandline.quoteArgument("-Dclassworlds.conf=" + classworldsConfPath));
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
    private String extractMaven3Dependencies(long artifactoryServerId, Maven3BuildContext mavenBuildContext)
            throws IOException {

        if (artifactoryServerId == -1 && !aggregateBuildInfo) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(bambooTmp, getPlanKey(customVariableContext), mavenBuildContext,
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
