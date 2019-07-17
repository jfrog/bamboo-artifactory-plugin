package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.bamboo.util.buildInfo.BuildInfoHelper;
import org.jfrog.bamboo.util.generic.GenericArtifactsResolver;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.util.TaskUtils.getArtifactoryDependenciesClient;

/**
 * @author Lior Hasson
 */
public class ArtifactoryGenericResolveTask extends AbstractSpecTask implements TaskType {

    private static final Logger log = Logger.getLogger(ArtifactoryGenericResolveTask.class);
    private PluginAccessor pluginAccessor;
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private BuildParamsOverrideManager buildParamsOverrideManager;

    public ArtifactoryGenericResolveTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        BuildLogger logger = taskContext.getBuildLogger();
        logger.addBuildLogEntry("Bamboo Artifactory Plugin version: " + Utils.getArtifactoryVersion(pluginAccessor));
        BuildContext context = taskContext.getBuildContext();
        String json = BuildInfoHelper.removeBuildInfoFromContext(taskContext);
        GenericContext genericContext = new GenericContext(taskContext.getConfigurationMap());
        BuildInfoHelper buildInfoHelper = BuildInfoHelper.createBuildInfoHelper(taskContext, context, environmentVariableAccessor, genericContext.getSelectedServerId(), genericContext, buildParamsOverrideManager);
        ArtifactoryDependenciesClient client = getArtifactoryDependenciesClient(genericContext, buildParamsOverrideManager, log);
        try {
            org.jfrog.build.api.util.Log bambooBuildInfoLog = new BuildInfoLog(log, logger);
            List<BuildDependency> buildDependencies;
            List<Dependency> dependencies;
            if (genericContext.isUseFileSpecs()) {
                buildDependencies = Lists.newArrayList();
                initFileSpec(taskContext);
                SpecsHelper specsHelper = new SpecsHelper(bambooBuildInfoLog);
                dependencies = specsHelper.downloadArtifactsBySpec(fileSpec, client, taskContext.getWorkingDirectory().getCanonicalPath());
            } else {
                GenericArtifactsResolver resolver = new GenericArtifactsResolver(taskContext, client,
                        genericContext.getResolvePattern(), bambooBuildInfoLog);
                buildDependencies = resolver.retrieveBuildDependencies();
                dependencies = resolver.retrievePublishedDependencies();
            }

            if (genericContext.isCaptureBuildInfo()) {
                Build build = buildInfoHelper.getBuild(genericContext, taskContext);
                build.setBuildAgent(new BuildAgent("Generic"));
                Map<String, String> buildProperties = buildInfoHelper.getDynamicPropertyMap(build);
                build = buildInfoHelper.addBuildInfoParams(taskContext, build, buildProperties, Lists.newArrayList(), dependencies, buildDependencies);
                if (StringUtils.isNotBlank(json)) {
                    BuildInfoHelper.addBuildInfoToContext(taskContext, json);
                }
                BuildInfoHelper.addBuildToContext(taskContext, build);
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
}
