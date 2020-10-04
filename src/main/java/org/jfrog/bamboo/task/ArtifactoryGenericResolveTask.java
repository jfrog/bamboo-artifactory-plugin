package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.bamboo.util.generic.GenericArtifactsResolver;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lior Hasson
 */
public class ArtifactoryGenericResolveTask extends ArtifactoryTaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryGenericResolveTask.class);
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final BuildParamsOverrideManager buildParamsOverrideManager;
    private CustomVariableContext customVariableContext;
    private BuildInfoHelper buildInfoHelper;
    private GenericContext genericContext;
    private BuildLogger logger;
    private Log buildInfoLog;
    private String fileSpec;

    public ArtifactoryGenericResolveTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    @Override
    protected void initTask(@NotNull TaskContext context) {
        logger = context.getBuildLogger();
        buildInfoLog = getLog();
        genericContext = new GenericContext(context.getConfigurationMap());
        Map<String, String> runtimeContext = context.getRuntimeTaskContext();
        buildInfoHelper = BuildInfoHelper.createResolveBuildInfoHelper(context, context.getBuildContext(), environmentVariableAccessor, genericContext.getSelectedServerId(),
                genericContext.getOverriddenUsername(runtimeContext, buildInfoLog, false),
                genericContext.getOverriddenPassword(runtimeContext, buildInfoLog, false), buildParamsOverrideManager);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + Utils.getPluginVersion(pluginAccessor));

        try (ArtifactoryDependenciesClient client = TaskUtils.getArtifactoryDependenciesClient(buildInfoHelper.getServerConfig(), buildInfoLog)) {
            List<BuildDependency> buildDependencies;
            List<Dependency> dependencies;
            if (genericContext.isUseFileSpecs()) {
                buildDependencies = Lists.newArrayList();
                initFileSpec(taskContext, logger);
                SpecsHelper specsHelper = new SpecsHelper(buildInfoLog);
                dependencies = specsHelper.downloadArtifactsBySpec(fileSpec, client, taskContext.getWorkingDirectory().getCanonicalPath());
            } else {
                GenericArtifactsResolver resolver = new GenericArtifactsResolver(taskContext, client,
                        genericContext.getResolvePattern(), buildInfoLog);
                buildDependencies = resolver.retrieveBuildDependencies();
                dependencies = resolver.retrievePublishedDependencies();
            }

            if (genericContext.isCaptureBuildInfo()) {
                Build build = buildInfoHelper.getBuild(taskContext, genericContext);
                Map<String, String> buildProperties = buildInfoHelper.getDynamicPropertyMap(build);
                build = buildInfoHelper.addBuildInfoParams(taskContext, build, buildProperties, Lists.newArrayList(), dependencies, buildDependencies);
                taskBuildInfo = build;
            }
        } catch (IOException | InterruptedException e) {
            buildInfoLog.error("Exception occurred while executing task", e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    @Override
    protected String getTaskUsageName() {
        return "generic_resolve";
    }

    @Override
    protected Log getLog() {
        return new BuildInfoLog(log, logger);
    }

    private void initFileSpec(CommonTaskContext context, BuildLogger logger) throws IOException {
        fileSpec = FileSpecUtils.getFileSpec(genericContext.isFileSpecInJobConfiguration(),
                genericContext.getJobConfigurationSpec(), genericContext.getFilePathSpec(), context.getWorkingDirectory(),
                customVariableContext, logger);
        logger.addBuildLogEntry("Spec: " + fileSpec);
        FileSpecUtils.validateFileSpec(fileSpec);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
