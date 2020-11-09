package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuilderDependencyHelper;
import org.jfrog.bamboo.builder.GradleDataHelper;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.util.ConfigurationPathHolder;
import org.jfrog.bamboo.util.PluginProperties;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Invocation of the Gradle build.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGradleTask extends BaseJavaBuildTask {
    public static final String TASK_NAME = "artifactoryGradleTask";

    public static final String EXECUTABLE_WRAPPER_NAME = SystemUtils.IS_OS_WINDOWS ? "./gradlew.bat" : "./gradlew";
    public static final String EXECUTABLE_NAME = SystemUtils.IS_OS_WINDOWS ? "gradle.bat" : "gradle";
    private static final String GRADLE_KEY = "system.builder.gradle.";
    private AdministrationConfiguration administrationConfiguration;
    private final BuilderDependencyHelper dependencyHelper;
    private final CapabilityContext capabilityContext;
    private GradleBuildContext gradleBuildContext;
    private GradleDataHelper gradleDataHelper;
    private String artifactoryPluginVersion;
    private String gradleDependenciesDir;

    public ArtifactoryGradleTask(final ProcessService processService,
                                 final EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext,
                                 TestCollationService testCollationService) {
        super(testCollationService, environmentVariableAccessor, processService);
        this.capabilityContext = capabilityContext;
        dependencyHelper = new BuilderDependencyHelper("artifactoryGradleBuilder");
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        artifactoryPluginVersion = Utils.getPluginVersion(pluginAccessor);
        gradleBuildContext = createBuildContext(context);
        initEnvironmentVariables(gradleBuildContext);
        aggregateBuildInfo = gradleBuildContext.shouldAggregateBuildInfo(context);
        gradleDataHelper = new GradleDataHelper(buildParamsOverrideManager, context, gradleBuildContext,
                administrationConfiguration, environmentVariableAccessor, artifactoryPluginVersion, aggregateBuildInfo);
    }

    @Override
    @NotNull
    public TaskResult runTask(@NotNull TaskContext context) throws TaskException {
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + artifactoryPluginVersion);
        final ErrorMemorisingInterceptor errorLines = new ErrorMemorisingInterceptor();
        logger.getInterceptorStack().add(errorLines);

        File rootDirectory = context.getRootDirectory();
        try {
            gradleDependenciesDir = extractGradleDependencies(gradleBuildContext.getArtifactoryServerId(), rootDirectory, gradleBuildContext);
        } catch (IOException e) {
            buildInfoLog.error("Error occurred while preparing Artifactory Gradle Runner dependencies. Build Info support is disabled: ", e);
        }

        // Get gradle executable.
        String gradleCommandLine = getExecutable(gradleBuildContext);
        if (StringUtils.isBlank(gradleCommandLine)) {
            log.error(logger.addErrorLogEntry("Gradle executable is not defined!"));
            return TaskResultBuilder.newBuilder(context).failed().build();
        }
        List<String> command = Lists.newArrayList(gradleCommandLine);

        // Add switches.
        String switches = gradleBuildContext.getSwitches();
        if (StringUtils.isNotBlank(switches)) {
            String[] switchTokens = StringUtils.split(switches, ' ');
            command.addAll(Arrays.asList(switchTokens));
        }

        // Add tasks.
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

        // Read init-script, create and write data to buildinfo.properties.
        ConfigurationPathHolder pathHolder = getGradleInitScriptFile(gradleDataHelper, gradleBuildContext, aggregateBuildInfo);
        if (pathHolder != null) {
            // Add initscript path and artifactoryPublish task to command.
            if (!gradleBuildContext.useArtifactoryGradlePlugin()) {
                command.add("-I");
                command.add(Commandline.quoteArgument(pathHolder.getInitScriptPath()));
            }
            TaskUtils.appendBuildInfoPropertiesArgument(command, pathHolder.getClientConfPath());
            command.add(ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME);
        } else {
            // Disable build-info aggregation.
            aggregateBuildInfo = false;
        }

        String subDirectory = gradleBuildContext.getBuildScript();
        if (StringUtils.isNotBlank(subDirectory)) {
            rootDirectory = new File(rootDirectory, subDirectory);
        }

        // Override the JAVA_HOME according to the build configuration.
        String jdkPath = getConfiguredJdkPath(buildParamsOverrideManager, gradleBuildContext, capabilityContext);
        environmentVariables.put("JAVA_HOME", jdkPath);

        gradleDataHelper.addPasswordsSystemProps(command, context);
        ExternalProcess process = getExternalProcess(context, rootDirectory, command, environmentVariables);
        try {
            executeExternalProcess(logger, process, log);
            if (aggregateBuildInfo) {
                convertGeneratedBuildInfoToBuild();
            }

            return collectTestResults(gradleBuildContext, context, process);
        } finally {
            context.getBuildContext().getBuildResult().addBuildErrors(errorLines.getErrorStringList());
        }
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return gradleDataHelper.getDeployServer();
    }

    @Override
    protected String getTaskUsageName() {
        return "gradle";
    }

    private GradleBuildContext createBuildContext(CommonTaskContext context) {
        Map<String, String> combinedMap = getCombinedConfiguration(context);
        return new GradleBuildContext(combinedMap);
    }

    private ConfigurationPathHolder getGradleInitScriptFile(GradleDataHelper initScriptHelper, GradleBuildContext buildContext, boolean aggregateBuildInfo) {
        File gradleJarFile = new File(gradleDependenciesDir, PluginProperties
                .getPluginProperty(PluginProperties.GRADLE_DEPENDENCY_FILENAME_KEY));
        if (!gradleJarFile.exists()) {
            log.warn(logger.addBuildLogEntry("Unable to locate the Gradle extractor. Build-info task will not be added."));
            return null;
        }

        try (JarFile gradleJar = new JarFile(gradleJarFile)) {
            ZipEntry initScriptEntry = gradleJar.getEntry("initscripttemplate.gradle");

            if (initScriptEntry == null) {
                log.warn(logger.addBuildLogEntry("Unable to locate the Gradle init script. Build-info task will not be added."));
                return null;
            }

            try (InputStream initScriptStream = gradleJar.getInputStream(initScriptEntry)) {
                if (initScriptStream == null) {
                    log.warn(logger.addBuildLogEntry("Unable to locate the gradle init script template. Build-info task will not be added."));
                    return null;
                }

                String scriptTemplate = IOUtils.toString(initScriptStream, StandardCharsets.UTF_8);
                ConfigurationPathHolder configurationPathHolder = initScriptHelper
                        .createAndGetGradleInitScriptPath(gradleDependenciesDir, buildContext, scriptTemplate,
                                environmentVariableAccessor.getEnvironment(), aggregateBuildInfo);
                if (aggregateBuildInfo) {
                    environmentVariables.put(BuildInfoFields.GENERATED_BUILD_INFO, initScriptHelper.getBuildInfoTempFilePath().getAbsolutePath());
                }
                return configurationPathHolder;
            }
        } catch (IOException e) {
            buildInfoLog.warn("Unable to read from the Gradle extractor jar. Build-info task will not be added: " + e.getMessage());
            return null;
        }
    }

    private String getExecutable(PackageManagersContext buildContext) throws TaskException {
        // Return gradle wrapper if required
        if ((buildContext instanceof GradleBuildContext) && ((GradleBuildContext) buildContext).isUseGradleWrapper()) {
            String gradleWrapperLocation = ((GradleBuildContext) buildContext).getGradleWrapperLocation();
            if (StringUtils.isNotBlank(gradleWrapperLocation)) {
                return gradleWrapperLocation;
            }
            return EXECUTABLE_WRAPPER_NAME;
        }
        // Return gradle executable
        return TaskUtils.getExecutablePath(buildContext, capabilityContext, GRADLE_KEY, EXECUTABLE_NAME, TASK_NAME);
    }

    /**
     * Extracts the Artifactory Gradle recorder and all the needed to dependencies
     *
     * @return Path of recorder and dependency jar folder if extraction succeeded. Null if not
     */
    private String extractGradleDependencies(long artifactoryServerId, File rootDirectory,
                                             GradleBuildContext context) throws IOException {
        if (artifactoryServerId == -1 && !aggregateBuildInfo) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(rootDirectory, context,
                PluginProperties.getPluginProperty(PluginProperties.GRADLE_DEPENDENCY_FILENAME_KEY));
    }

    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }
}
