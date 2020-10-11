package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.v2.build.BuildContext;
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

/**
 * @author Lior Hasson
 */
public class ArtifactoryGenericResolveTask extends ArtifactoryTaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryGenericResolveTask.class);
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private BuildParamsOverrideManager buildParamsOverrideManager;
    private BuildLogger logger;
    private CustomVariableContext customVariableContext;
    private String fileSpec;
    private BuildContext buildContext;
    private GenericContext genericContext;
    private BuildInfoHelper buildInfoHelper;

    public ArtifactoryGenericResolveTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    @Override
    protected void initTask(@NotNull TaskContext context) {
        logger = context.getBuildLogger();
        buildContext = context.getBuildContext();
        genericContext = new GenericContext(context.getConfigurationMap());
        buildInfoHelper = BuildInfoHelper.createResolveBuildInfoHelper(genericContext.getBuildName(buildContext),
                genericContext.getBuildNumber(buildContext), context, buildContext, environmentVariableAccessor,
                genericContext.getSelectedServerId(), genericContext.getUsername(), genericContext.getPassword(),
                buildParamsOverrideManager);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + Utils.getPluginVersion(pluginAccessor));

        ArtifactoryDependenciesClient client = TaskUtils.getArtifactoryDependenciesClient(buildInfoHelper.getServerConfig(), new BuildInfoLog(log, logger));
        try {
            org.jfrog.build.api.util.Log bambooBuildInfoLog = new BuildInfoLog(log, logger);
            List<BuildDependency> buildDependencies;
            List<Dependency> dependencies;
            if (genericContext.isUseFileSpecs()) {
                buildDependencies = Lists.newArrayList();
                initFileSpec(taskContext, logger);
                SpecsHelper specsHelper = new SpecsHelper(bambooBuildInfoLog);
                dependencies = specsHelper.downloadArtifactsBySpec(fileSpec, client, taskContext.getWorkingDirectory().getCanonicalPath());
            } else {
                GenericArtifactsResolver resolver = new GenericArtifactsResolver(taskContext, client,
                        genericContext.getResolvePattern(), bambooBuildInfoLog);
                buildDependencies = resolver.retrieveBuildDependencies();
                dependencies = resolver.retrievePublishedDependencies();
            }

            if (genericContext.isCaptureBuildInfo()) {
                Build build = buildInfoHelper.getBuild(taskContext, genericContext);
                build = buildInfoHelper.addBuildInfoParams(build, Lists.newArrayList(), dependencies, buildDependencies);
                taskBuildInfo = build;
            }
        } catch (IOException|InterruptedException e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        } finally {
            client.close();
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
