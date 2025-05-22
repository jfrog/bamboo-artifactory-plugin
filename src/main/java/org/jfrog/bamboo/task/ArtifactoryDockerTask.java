package org.jfrog.bamboo.task;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.DockerBuildContext;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.docker.extractor.DockerPull;
import org.jfrog.build.extractor.docker.extractor.DockerPush;

import javax.inject.Inject;
import java.util.Map;

/**
 * Created by Bar Belity on 09/10/2020.
 */
public class ArtifactoryDockerTask extends ArtifactoryTaskType {
    @Inject
    @ComponentImport
    private EnvironmentVariableAccessor environmentVariableAccessor;
    private BuildParamsOverrideManager buildParamsOverrideManager;
    @Inject
    private CustomVariableContext customVariableContext;
    private Map<String, String> environmentVariables;
    private DockerBuildContext dockerBuildContext;
    private BuildInfoHelper buildInfoHelper;
    private String buildNumber;
    private String buildName;
    private ArtifactoryManagerBuilder artifactoryManagerBuilder;

    @Inject
    private ServerConfigManager serverConfigManager;

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        dockerBuildContext = new DockerBuildContext(taskContext.getConfigurationMap());
        buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        environmentVariables = TaskUtils.getEnvironmentVariables(dockerBuildContext, environmentVariableAccessor);
        BuildContext buildContext = ((TaskContext) context).getBuildContext();
        buildName = dockerBuildContext.getBuildName(buildContext);
        buildNumber = dockerBuildContext.getBuildNumber(buildContext);
        initBuildInfoHelper(buildContext);
        artifactoryManagerBuilder = TaskUtils.getArtifactoryManagerBuilderBuilder(buildInfoHelper.getServerConfig(), buildInfoLog);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        BuildInfo build;
        if (dockerBuildContext.isDockerCommandPull()) {
            build = executeDockerPull();
        } else {
            build = executeDockerPush();
        }

        // Fail run if no build is returned.
        if (build == null) {
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }

        // Append build info and add to context.
        if (dockerBuildContext.isCaptureBuildInfo()) {
            build.setName(buildName);
            build.setNumber(buildNumber);
            buildInfoHelper.addEnvVarsToBuild(dockerBuildContext, build);
            taskBuildInfo = build;
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private BuildInfo executeDockerPull() {
        String repo = buildInfoHelper.overrideParam(dockerBuildContext.getResolutionRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        String userName = buildInfoHelper.getServerConfig().getUsername();
        String password = buildInfoHelper.getServerConfig().getPassword();
        return new DockerPull(artifactoryManagerBuilder,
                dockerBuildContext.getImageName(), dockerBuildContext.getHost(), repo, userName, password,
                buildInfoLog, environmentVariables).execute();
    }

    private BuildInfo executeDockerPush() {
        String repo = buildInfoHelper.overrideParam(dockerBuildContext.getPublishingRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOY_REPO);
        String userName = buildInfoHelper.getServerConfig().getUsername();
        String password = buildInfoHelper.getServerConfig().getPassword();
        return new DockerPush(artifactoryManagerBuilder, dockerBuildContext.getImageName(),
                dockerBuildContext.getHost(), TaskUtils.getCommonArtifactPropertiesMap(buildInfoHelper), repo,
                userName, password, buildInfoLog, environmentVariables).execute();
    }

    private void initBuildInfoHelper(BuildContext buildContext) {
        Map<String, String> runtimeContext = taskContext.getRuntimeTaskContext();
        if (dockerBuildContext.isDockerCommandPull()) {
            buildInfoHelper = BuildInfoHelper.createResolveBuildInfoHelper(buildName, buildNumber, taskContext, buildContext,
                    environmentVariableAccessor, dockerBuildContext.getResolutionArtifactoryServerId(),
                    dockerBuildContext.getOverriddenUsername(runtimeContext, buildInfoLog, false),
                    dockerBuildContext.getOverriddenPassword(runtimeContext, buildInfoLog, false), buildParamsOverrideManager, serverConfigManager);
        } else {
            buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(buildName, buildNumber, taskContext, buildContext,
                    environmentVariableAccessor, dockerBuildContext.getArtifactoryServerId(),
                    dockerBuildContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                    dockerBuildContext.getOverriddenPassword(runtimeContext, buildInfoLog, true), buildParamsOverrideManager, serverConfigManager);
        }
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    public void setServerConfigManager(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
    }

    public void setEnvironmentVariableAccessor(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    @Override
    protected String getTaskUsageName() {
        return "docker";
    }
}
