package org.jfrog.bamboo.task;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.DotNetBuildContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.build.extractor.nuget.extractor.NugetRun;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Bar Belity on 14/10/2020.
 */
public abstract class ArtifactoryDotNetTaskBase extends ArtifactoryTaskType {

    protected final EnvironmentVariableAccessor environmentVariableAccessor;
    protected final CapabilityContext capabilityContext;
    protected CustomVariableContext customVariableContext;
    protected DotNetBuildContext dotNetBuildContext;
    protected BuildInfoHelper buildInfoHelper;
    protected BuildParamsOverrideManager buildParamsOverrideManager;
    protected Map<String, String> environmentVariables;
    protected Path workingDir;
    protected String buildName;
    protected String buildNumber;
    protected TaskType taskType;
    protected String specTemplate = "{\n" +
            "  \"files\": [\n" +
            "    {\n" +
            "      \"pattern\": \"%s\",\n" +
            "      \"target\": \"%s\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    protected void initTask(CommonTaskContext context, String taskKey, String executable, String taskName, TaskType taskType) throws TaskException {
        super.initTask(context);
        this.taskType = taskType;
        dotNetBuildContext = new DotNetBuildContext(taskContext.getConfigurationMap());
        buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        BuildContext buildContext = ((TaskContext) context).getBuildContext();
        buildName = dotNetBuildContext.getBuildName(buildContext);
        buildNumber = dotNetBuildContext.getBuildNumber(buildContext);
        initBuildInfoHelper(buildContext);
        environmentVariables = getEnv(taskKey, executable, taskName);
        workingDir = getWorkPath();
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        Build build;
        if (dotNetBuildContext.isRestoreCommand()) {
            build = executeRestore();
        } else {
            try {
                build = executePush();
            } catch (Exception e) {
                buildInfoLog.error("Exception occurred while executing task", e);
                return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
            }
        }

        // Fail run if no build is returned.
        if (build == null) {
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }

        // Append build info and add to context.
        if (dotNetBuildContext.isCaptureBuildInfo()) {
            build.setName(buildName);
            build.setNumber(buildNumber);
            buildInfoHelper.addEnvVarsToBuild(dotNetBuildContext, build);
            taskBuildInfo = build;
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    protected ArtifactoryDotNetTaskBase(EnvironmentVariableAccessor environmentVariableAccessor, final CapabilityContext capabilityContext) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        ContainerManager.autowireComponent(this);
    }

    protected Build executeRestore() {
        ArtifactoryDependenciesClientBuilder clientBuilder = TaskUtils.getArtifactoryDependenciesClientBuilder(
                buildInfoHelper.getServerConfig(), new BuildInfoLog(log, logger));
        String repo = buildInfoHelper.overrideParam(dotNetBuildContext.getResolutionRepo(),
                BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        return new NugetRun(clientBuilder, repo, taskType == TaskType.DOTNET, String.format("restore %s", dotNetBuildContext.getArguments()),
                buildInfoLog, workingDir, environmentVariables, null, buildInfoHelper.getServerConfig().getUsername(),
                buildInfoHelper.getServerConfig().getPassword()).execute();
    }

    protected Build executePush() throws Exception {
        String repo = buildInfoHelper.overrideParam(dotNetBuildContext.getPublishingRepo(),
                BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOY_REPO);

        // Create spec.
        String artifactsPattern = dotNetBuildContext.getPushPattern();
        String targetPath = createTargetPath(repo, dotNetBuildContext.getPushTarget());
        String uploadSpec = String.format(specTemplate, artifactsPattern, targetPath);

        // Perform upload.
        SpecsHelper specsHelper = new SpecsHelper(new BuildInfoLog(log, logger));
        ArtifactoryBuildInfoClientBuilder clientBuilder = buildInfoHelper.getClientBuilder(taskContext.getBuildLogger(), log);
        List<Artifact> artifactList = specsHelper.uploadArtifactsBySpec(uploadSpec, workingDir.toFile(), new HashMap<>(), clientBuilder);

        // Create build object.
        Build build = buildInfoHelper.getBuilder((TaskContext) taskContext).build();
        return buildInfoHelper.addBuildInfoParams(build, artifactList, Lists.newArrayList(), Lists.newArrayList());
    }

    private String createTargetPath(String repo, String relativePath) {
        String targetPath = StringUtils.appendIfMissing(repo, "/");
        if (StringUtils.isNotBlank(relativePath)) {
            relativePath = StringUtils.removeStart(relativePath, "/");
            targetPath = targetPath + StringUtils.appendIfMissing(relativePath, "/");
        }
        return targetPath;
    }

    protected Map<String, String> getEnv(String taskKey, String executable, String taskName) throws TaskException {
        Map<String, String> env = TaskUtils.getEnvironmentVariables(dotNetBuildContext, environmentVariableAccessor);
        return TaskUtils.addExecutablePathToEnv(env, dotNetBuildContext, capabilityContext, taskKey, executable, taskName, containerized);
    }

    protected Path getWorkPath() {
        File rootDir = taskContext.getRootDirectory();
        String subDirectory = dotNetBuildContext.getWorkingSubdirectory();
        if (StringUtils.isNotBlank(subDirectory)) {
            return new File(rootDir, subDirectory).toPath();
        }
        return rootDir.toPath();
    }

    protected void initBuildInfoHelper(BuildContext buildContext) {
        Map<String, String> runtimeContext = taskContext.getRuntimeTaskContext();
        if (dotNetBuildContext.isRestoreCommand()) {
            buildInfoHelper = BuildInfoHelper.createResolveBuildInfoHelper(buildName, buildNumber, taskContext, buildContext,
                    environmentVariableAccessor, dotNetBuildContext.getResolutionArtifactoryServerId(),
                    dotNetBuildContext.getOverriddenUsername(runtimeContext, buildInfoLog, false),
                    dotNetBuildContext.getOverriddenPassword(runtimeContext, buildInfoLog, false), buildParamsOverrideManager);
        } else {
            buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(buildName, buildNumber, taskContext, buildContext,
                    environmentVariableAccessor, dotNetBuildContext.getArtifactoryServerId(),
                    dotNetBuildContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                    dotNetBuildContext.getOverriddenPassword(runtimeContext, buildInfoLog, true), buildParamsOverrideManager);
        }
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    protected enum TaskType {
        NUGET,
        DOTNET
    }
}
