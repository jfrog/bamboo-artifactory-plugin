package org.jfrog.bamboo.task;


import com.atlassian.bamboo.build.ErrorLogEntry;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
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
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.builder.BuilderDependencyHelper;
import org.jfrog.bamboo.builder.GradleInitScriptHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.util.ConfigurationPathHolder;
import org.jfrog.bamboo.util.PluginProperties;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Invocation of the Gradle build.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGradleTask extends ArtifactoryTaskType {
    public static final String TASK_NAME = "artifactoryGradleTask";
    public static final String EXECUTABLE_NAME = SystemUtils.IS_OS_WINDOWS ? "gradle.bat" : "gradle";
    public static final String EXECUTABLE_WRAPPER_NAME = SystemUtils.IS_OS_WINDOWS ? "./gradlew.bat" : "./gradlew";
    private static final Logger log = Logger.getLogger(ArtifactoryGradleTask.class);
    private static final String GRADLE_KEY = "system.builder.gradle.";
    private final ProcessService processService;
    private final CapabilityContext capabilityContext;
    private BuilderDependencyHelper dependencyHelper;
    private String gradleDependenciesDir = null;
    private AdministrationConfiguration administrationConfiguration;

    public ArtifactoryGradleTask(final ProcessService processService,
                                 final EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext,
                                 TestCollationService testCollationService) {
        super(testCollationService, environmentVariableAccessor);
        this.processService = processService;
        this.capabilityContext = capabilityContext;
        dependencyHelper = new BuilderDependencyHelper("artifactoryGradleBuilder");
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @Override
    @NotNull
    public TaskResult execute(@NotNull TaskContext context) throws TaskException {
        BuildLogger logger = getBuildLogger(context);
        String artifactoryPluginVersion = getArtifactoryVersion();
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + artifactoryPluginVersion);
        final ErrorMemorisingInterceptor errorLines = new ErrorMemorisingInterceptor();
        logger.getInterceptorStack().add(errorLines);

        GradleBuildContext gradleBuildContext = createBuildContext(context);
        initEnvironmentVariables(gradleBuildContext);

        long serverId = gradleBuildContext.getArtifactoryServerId();
        File rootDirectory = context.getRootDirectory();
        try {
            gradleDependenciesDir = extractGradleDependencies(serverId, rootDirectory, gradleBuildContext);
        } catch (IOException e) {
            gradleDependenciesDir = null;
            logger.addBuildLogEntry(new ErrorLogEntry(
                    "Error occurred while preparing Artifactory Gradle Runner dependencies. Build Info support is " +
                            "disabled: " + e.getMessage()));
            log.error("Error occurred while preparing Artifactory Gradle Runner dependencies. " +
                    "Build Info support is disabled.", e);
        }
        String gradleCommandLine = getExecutable(gradleBuildContext);
        if (StringUtils.isBlank(gradleCommandLine)) {
            log.error(logger.addErrorLogEntry("Gradle executable is not defined!"));
            return TaskResultBuilder.newBuilder(context).failed().build();
        }
        List<String> command = Lists.newArrayList(gradleCommandLine);
        String switches = gradleBuildContext.getSwitches();
        if (StringUtils.isNotBlank(switches)) {
            String[] switchTokens = StringUtils.split(switches, ' ');
            command.addAll(Arrays.asList(switchTokens));
        }
        String tasks = gradleBuildContext.getTasks();
        if (gradleBuildContext.releaseManagementContext.isActivateReleaseManagement()) {
            String altTasks = gradleBuildContext.releaseManagementContext.getAlternativeTasks();
            if (StringUtils.isNotBlank(altTasks)) {
                tasks = altTasks;
            }
        }
        if (StringUtils.isNotBlank(tasks)) {
            String[] taskTokens = StringUtils.split(tasks, ' ');
            command.addAll(Arrays.asList(taskTokens));
        }

        ConfigurationPathHolder pathHolder = getGradleInitScriptFile(context, gradleBuildContext, artifactoryPluginVersion);
        if (pathHolder != null) {
            if (!gradleBuildContext.useArtifactoryGradlePlugin()) {
                command.add("-I");
                command.add(Commandline.quoteArgument(pathHolder.getInitScriptPath()));
            }
            TaskUtils.appendBuildInfoPropertiesArgument(command, pathHolder.getClientConfPath());
            command.add(BuildInfoBaseTask.BUILD_INFO_TASK_NAME);
        }

        String subDirectory = gradleBuildContext.getBuildScript();
        if (StringUtils.isNotBlank(subDirectory)) {
            rootDirectory = new File(rootDirectory, subDirectory);
        }

        // Override the JAVA_HOME according to the build configuration:
        String jdkPath = getConfiguredJdkPath(buildParamsOverrideManager, gradleBuildContext, capabilityContext);
        environmentVariables.put("JAVA_HOME", jdkPath);
        environmentVariables.putAll(getPasswordsMap(gradleBuildContext));

        log.debug("Running Gradle command: " + command.toString());
        ExternalProcessBuilder processBuilder =
                new ExternalProcessBuilder().workingDirectory(rootDirectory).command(command).env(environmentVariables);
        try {
            ExternalProcess process = processService.createExternalProcess(context, processBuilder);
            process.execute();

            if (process.getHandler() != null && !process.getHandler().succeeded()) {
                String externalProcessOutput = getErrorMessage(process);
                logger.addBuildLogEntry(externalProcessOutput);
                log.debug("Process command error: " + externalProcessOutput);
            }

            return collectTestResults(gradleBuildContext, context, process);
        } finally {
            context.getBuildContext().getBuildResult().addBuildErrors(errorLines.getErrorStringList());
        }
    }

    private GradleBuildContext createBuildContext(TaskContext context) {
        Map<String, String> combinedMap = Maps.newHashMap();
        combinedMap.putAll(context.getConfigurationMap());
        BuildContext parentBuildContext = context.getBuildContext().getParentBuildContext();
        if (parentBuildContext != null) {
            Map<String, String> customBuildData = parentBuildContext.getBuildResult().getCustomBuildData();
            combinedMap.putAll(customBuildData);
        }
        return new GradleBuildContext(combinedMap);
    }

    private ConfigurationPathHolder getGradleInitScriptFile(TaskContext taskContext, GradleBuildContext buildContext,
                                                            String artifactoryPluginVersion) {
        File gradleJarFile = new File(gradleDependenciesDir, PluginProperties
                .getPluginProperty(PluginProperties.GRADLE_DEPENDENCY_FILENAME_KEY));
        if (!gradleJarFile.exists()) {
            log.warn("Unable to locate the Gradle extractor. Build-info task will not be added.");
            return null;
        }

        InputStream initScriptStream = null;
        JarFile gradleJar = null;
        try {
            gradleJar = new JarFile(gradleJarFile);
            ZipEntry initScriptEntry = gradleJar.getEntry("initscripttemplate.gradle");

            if (initScriptEntry == null) {
                log.warn("Unable to locate the Gradle init script. Build-info task will not be added.");
                return null;
            }

            initScriptStream = gradleJar.getInputStream(initScriptEntry);
            if (initScriptStream == null) {
                log.warn("Unable to locate the gradle init script template. Build-info task will not be added.");
                return null;
            }

            String scriptTemplate = IOUtils.toString(initScriptStream);
            GradleInitScriptHelper initScriptHelper = new GradleInitScriptHelper();
            initScriptHelper.init(buildParamsOverrideManager, taskContext.getBuildContext());
            initScriptHelper.setAdministrationConfiguration(administrationConfiguration);
            return initScriptHelper
                    .createAndGetGradleInitScriptPath(gradleDependenciesDir, buildContext, taskContext.getBuildLogger(),
                            scriptTemplate, environmentVariableAccessor.getEnvironment(taskContext),
                            environmentVariableAccessor.getEnvironment(), artifactoryPluginVersion);
        } catch (IOException e) {
            log.warn("Unable to read from the Gradle extractor jar. Build-info task will not be added: " +
                    e.getMessage());
            return null;
        } finally {
            IOUtils.closeQuietly(initScriptStream);
            try {
                if (gradleJar != null) {
                    gradleJar.close();
                }
            } catch (IOException e) {
                log.warn("Unable to close the Gradle extractor jar: " + e.getMessage());
            }
        }
    }

    public String getExecutable(AbstractBuildContext buildContext) throws TaskException {
        if ((buildContext instanceof GradleBuildContext) && ((GradleBuildContext) buildContext).isUseGradleWrapper()) {
            String gradleWrapperLocation = ((GradleBuildContext) buildContext).getGradleWrapperLocation();
            if (StringUtils.isNotBlank(gradleWrapperLocation)) {
                return gradleWrapperLocation;
            }
            return EXECUTABLE_WRAPPER_NAME;
        } else {
            ReadOnlyCapabilitySet capabilitySet = capabilityContext.getCapabilitySet();
            if (capabilitySet == null) {
                return null;
            }
            Capability capability = capabilitySet.getCapability(GRADLE_KEY + buildContext.getExecutable());
            if (capability == null) {
                throw new TaskException(
                        "Gradle capability: " + buildContext.getExecutable() + " is not defined, please check " +
                                "job configuration");
            }

            String path = capability.getValue() + File.separator + "bin" + File.separator + EXECUTABLE_NAME;
            if (!new File(path).exists()) {
                throw new TaskException("Executable '" + EXECUTABLE_NAME + "'  does not exist at path '" + path + "'");
            }
            return path;
        }
    }

    /**
     * Extracts the Artifactory Gradle recorder and all the needed to dependencies
     *
     * @return Path of recorder and dependency jar folder if extraction succeeded. Null if not
     */

    private String extractGradleDependencies(long artifactoryServerId, File rootDirectory,
                                             GradleBuildContext context) throws IOException {

        if (artifactoryServerId == -1) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(rootDirectory, context,
                PluginProperties.getPluginProperty(PluginProperties.GRADLE_DEPENDENCY_FILENAME_KEY));
    }

    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }

    @NotNull
    private Map<String, String> getPasswordsMap(GradleBuildContext gradleBuildContext) {
        Map<String, String> result = new HashMap<>();
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        long selectedServerId = gradleBuildContext.getArtifactoryServerId();
        if (selectedServerId == -1) {
            return result;
        }
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(selectedServerId);
        if (serverConfig == null) {
            String warningMessage =
                    "Found an ID of a selected Artifactory server configuration (" + selectedServerId +
                            ") but could not find a matching configuration. Build info collection is disabled.";
            log.warn(warningMessage);
            return result;
        }
        String deployerPassword =
                buildParamsOverrideManager.getOverrideValue(BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD);
        if (StringUtils.isBlank(deployerPassword)) {
            deployerPassword = serverConfigManager.substituteVariables(gradleBuildContext.getDeployerPassword());
        }
        if (StringUtils.isBlank(deployerPassword)) {
            deployerPassword = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        ArtifactoryClientConfiguration clientConf = new ArtifactoryClientConfiguration(null);
        result.put(clientConf.resolver.getPrefix() + "password", serverConfig.getPassword());
        result.put(clientConf.publisher.getPrefix() + "password", deployerPassword);
        return result;
    }
}
