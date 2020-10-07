package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.NpmBuildContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmInstall;
import org.jfrog.build.extractor.npm.extractor.NpmPublish;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;

public class ArtifactoryNpmTask extends ArtifactoryTaskType {
    private static final String TASK_NAME = "artifactoryNpmTask";
    private static final String EXECUTABLE_NAME = "npm";
    private static final String NPM_KEY = "system.builder.npm.";
    private static final Logger log = Logger.getLogger(ArtifactoryNpmTask.class);
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;
    private CustomVariableContext customVariableContext;
    private TaskContext taskContext;
    private BuildLogger logger;
    private NpmBuildContext npmBuildContext;
    private Path packagePath;
    private BuildInfoHelper buildInfoHelper;
    private BuildParamsOverrideManager buildParamsOverrideManager;
    private Map<String, String> environmentVariables;
    private String buildName;
    private String buildNumber;

    public ArtifactoryNpmTask(EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        ContainerManager.autowireComponent(this);
    }

    @Override
    protected void initTask(@NotNull TaskContext context) throws TaskException {
        taskContext = context;
        logger = taskContext.getBuildLogger();
        npmBuildContext = new NpmBuildContext(taskContext.getConfigurationMap());
        buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        buildName = TaskUtils.getBuildNameFromAbstractBuildContext(taskContext.getBuildContext(), npmBuildContext);
        buildNumber = TaskUtils.getBuildNumberFromAbstractBuildContext(taskContext.getBuildContext(), npmBuildContext);
        initBuildInfoHelper();
        environmentVariables = getEnv();
        packagePath = getPackagePath();
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        Build build;
        if (npmBuildContext.isNpmCommandInstall()) {
            build = executeNpmInstall();
        } else {
            build = executeNpmPublish();
        }

        // Fail run if no build is returned
        if (build == null) {
            return failRun(taskContext);
        }

        // Append build info and add to context
        if (npmBuildContext.isCaptureBuildInfo()) {
            build.setName(buildName);
            build.setNumber(buildNumber);
            buildInfoHelper.addEnvVarsToBuild(npmBuildContext, build);
            taskBuildInfo = build;
        }

        Map<String, String> customBuildData = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        if (!customBuildData.containsKey(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) {
            customBuildData.put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private TaskResult failRun(@NotNull CommonTaskContext taskContext) {
        return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
    }

    /**
     * Initialise a BuildInfoHelper from the appropriate parameters (deploy / resolve)
     */
    private void initBuildInfoHelper() {
        if (npmBuildContext.isNpmCommandInstall()) {
            buildInfoHelper = BuildInfoHelper.createResolveBuildInfoHelper(taskContext, taskContext.getBuildContext(),
                    environmentVariableAccessor, npmBuildContext.getResolutionArtifactoryServerId(), npmBuildContext.getResolverUsername(),
                    npmBuildContext.getResolverPassword(), buildName, buildNumber, buildParamsOverrideManager);
        } else {
            buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(taskContext, taskContext.getBuildContext(),
                    environmentVariableAccessor, npmBuildContext.getArtifactoryServerId(), npmBuildContext.getDeployerUsername(),
                    npmBuildContext.getDeployerPassword(), buildName, buildNumber, buildParamsOverrideManager);
        }
    }


    /**
     * Handles the execution of npm Install.
     * @return Build containing affected artifacts, null if execution failed.
     */
    private Build executeNpmInstall() {
        ArtifactoryDependenciesClientBuilder clientBuilder = TaskUtils.getArtifactoryDependenciesClientBuilder(buildInfoHelper.getServerConfig(), new BuildInfoLog(log, logger));
        String repo = buildInfoHelper.overrideParam(npmBuildContext.getResolutionRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        return new NpmInstall(clientBuilder, repo, npmBuildContext.getArguments(), getLog(), packagePath, environmentVariables, "").execute();
    }

    /**
     * Handles the execution of npm Publish.
     * @return Build containing affected artifacts, null if execution failed.
     */
    private Build executeNpmPublish() {
        String repo = buildInfoHelper.overrideParam(npmBuildContext.getPublishingRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOY_REPO);
        return new NpmPublish(buildInfoHelper.getClientBuilder(logger, log), getPropertiesMap(), packagePath, repo, getLog(), environmentVariables, "").execute();
    }

    /**
     * Get a map of the artifact properties needed to be set (Build name, Build number, etc...).
     * @return Map containing all properties.
     */
    private ArrayListMultimap<String, String> getPropertiesMap() {
        Map<String, String> propertiesMap = new HashMap<>();
        buildInfoHelper.addCommonProperties(propertiesMap);
        return ArrayListMultimap.create(Multimaps.forMap(propertiesMap));
    }

    /**
     * Get package path from the root directory and append subdirectory if needed.
     * @return Package path
     */
    private Path getPackagePath() {
        File rootDir = taskContext.getRootDirectory();
        String subDirectory = npmBuildContext.getWorkingSubdirectory();
        if (StringUtils.isNotBlank(subDirectory)) {
            return new File(rootDir, subDirectory).toPath();
        }
        return rootDir.toPath();
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    public Map<String, String> getEnv() throws TaskException {
        Map<String, String> env = TaskUtils.getEnvironmentVariables(npmBuildContext, environmentVariableAccessor);
        return addExecutablePathToEnv(env);
    }

    /**
     * Npm commands expect the npm executable to be in "PATH"
     * */
    public Map<String, String> addExecutablePathToEnv(Map<String, String> env) throws TaskException {
        String executablePath = TaskUtils.getExecutablePath(npmBuildContext, capabilityContext, NPM_KEY, EXECUTABLE_NAME, TASK_NAME);
        String path = env.get("PATH");
        if (SystemUtils.IS_OS_WINDOWS) {
            path = executablePath + ";" + path;
        } else {
            path = executablePath + ":" + path;
        }
        env.put("PATH", path);
        return env;
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    @Override
    protected String getTaskUsageName() {
        return "npm";
    }

    @Override
    protected Log getLog() {
        return new BuildInfoLog(log, logger);
    }
}
