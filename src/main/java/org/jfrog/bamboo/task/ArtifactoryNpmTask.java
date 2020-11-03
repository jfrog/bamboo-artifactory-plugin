package org.jfrog.bamboo.task;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.NpmBuildContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmInstall;
import org.jfrog.build.extractor.npm.extractor.NpmPublish;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class ArtifactoryNpmTask extends ArtifactoryTaskType {
    private static final String TASK_NAME = "artifactoryNpmTask";
    private static final String NPM_KEY = "system.builder.npm.";
    private static final String EXECUTABLE_NAME = "npm";

    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;
    private BuildParamsOverrideManager buildParamsOverrideManager;
    private CustomVariableContext customVariableContext;
    private Map<String, String> environmentVariables;
    private NpmBuildContext npmBuildContext;
    private BuildInfoHelper buildInfoHelper;
    private String buildNumber;
    private String buildName;
    private Path packagePath;

    public ArtifactoryNpmTask(EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        ContainerManager.autowireComponent(this);
    }

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        npmBuildContext = new NpmBuildContext(taskContext.getConfigurationMap());
        buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        BuildContext buildContext = ((TaskContext) context).getBuildContext();
        buildName = npmBuildContext.getBuildName(buildContext);
        buildNumber = npmBuildContext.getBuildNumber(buildContext);
        initBuildInfoHelper(buildContext);
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

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private TaskResult failRun(@NotNull CommonTaskContext taskContext) {
        return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
    }

    /**
     * Initialise a BuildInfoHelper from the appropriate parameters (deploy / resolve)
     */
    private void initBuildInfoHelper(BuildContext buildContext) {
        Map<String, String> runtimeContext = taskContext.getRuntimeTaskContext();
        if (npmBuildContext.isNpmCommandInstall()) {
            buildInfoHelper = BuildInfoHelper.createResolveBuildInfoHelper(buildName, buildNumber, taskContext, buildContext,
                    environmentVariableAccessor, npmBuildContext.getResolutionArtifactoryServerId(),
                    npmBuildContext.getOverriddenUsername(runtimeContext, buildInfoLog, false),
                    npmBuildContext.getOverriddenPassword(runtimeContext, buildInfoLog, false), buildParamsOverrideManager);
        } else {
            buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(buildName, buildNumber, taskContext, buildContext,
                    environmentVariableAccessor, npmBuildContext.getArtifactoryServerId(),
                    npmBuildContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                    npmBuildContext.getOverriddenPassword(runtimeContext, buildInfoLog, true), buildParamsOverrideManager);
        }
    }

    /**
     * Handles the execution of npm Install.
     *
     * @return Build containing affected artifacts, null if execution failed.
     */
    private Build executeNpmInstall() {
        ArtifactoryDependenciesClientBuilder clientBuilder = TaskUtils.getArtifactoryDependenciesClientBuilder(buildInfoHelper.getServerConfig(), new BuildInfoLog(log, logger));
        String repo = buildInfoHelper.overrideParam(npmBuildContext.getResolutionRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        return new NpmInstall(clientBuilder, repo, npmBuildContext.getArguments(), buildInfoLog, packagePath, environmentVariables, "").execute();
    }

    /**
     * Handles the execution of npm Publish.
     *
     * @return Build containing affected artifacts, null if execution failed.
     */
    private Build executeNpmPublish() {
        String repo = buildInfoHelper.overrideParam(npmBuildContext.getPublishingRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOY_REPO);
        return new NpmPublish(buildInfoHelper.getClientBuilder(logger, log), TaskUtils.getCommonArtifactPropertiesMap(buildInfoHelper), packagePath, repo, buildInfoLog, environmentVariables, "").execute();
    }

    /**
     * Get package path from the root directory and append subdirectory if needed.
     *
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
        // Npm commands expect the npm executable to be in "PATH".
        return TaskUtils.addExecutablePathToEnv(env, npmBuildContext, capabilityContext, NPM_KEY, EXECUTABLE_NAME, TASK_NAME, containerized);
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    @Override
    protected String getTaskUsageName() {
        return "npm";
    }
}
