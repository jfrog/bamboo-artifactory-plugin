package org.jfrog.bamboo.task;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.PublishBuildInfoContext;
import org.jfrog.bamboo.util.PublishedBuildDetails;
import org.jfrog.bamboo.util.PublishedBuilds;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.generic.GenericData;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;
import static org.jfrog.bamboo.util.ConstantValues.PUBLISHED_BUILDS_DETAILS;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryPublishBuildInfoTask extends ArtifactoryTaskType {
    public static final String TASK_NAME = "artifactoryPublishBuildInfoTask";

    @Inject
    private EnvironmentVariableAccessor environmentVariableAccessor;
    @Inject
    private CustomVariableContext customVariableContext;
    private BuildInfoHelper buildInfoHelper;
    @Inject
    private ServerConfigManager serverConfigManager;

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
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        PublishBuildInfoContext publishBuildInfoContext = new PublishBuildInfoContext(context.getConfigurationMap());
        BuildParamsOverrideManager buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        BuildContext buildContext = ((TaskContext) context).getBuildContext();
        Map<String, String> runtimeContext = context.getRuntimeTaskContext();
        buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(publishBuildInfoContext.getBuildName(buildContext),
                publishBuildInfoContext.getBuildNumber(buildContext), context, buildContext, environmentVariableAccessor,
                publishBuildInfoContext.getArtifactoryServerId(),
                publishBuildInfoContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                publishBuildInfoContext.getOverriddenPassword(runtimeContext, buildInfoLog, true), buildParamsOverrideManager, serverConfigManager);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        Map<String, String> customBuildData = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        ArtifactoryManagerBuilder clientBuilder = buildInfoHelper.getClientBuilder(logger, log);
        try (ArtifactoryManager client = clientBuilder.build()) {
            String aggregatedBuildsJson = TaskUtils.getAndDeleteAggregatedBuildInfo(taskContext);
            BuildInfo build = buildInfoHelper.getBuilder(taskContext).build();
            // Aggregate relevant builds to one build.
            if (StringUtils.isNotBlank(aggregatedBuildsJson)) {
                addBuildsToPublish(taskContext, build, aggregatedBuildsJson);
            }
            client.publishBuildInfo(build, "");

            // Add build details to context.
            addPublishedBuildDetailsToBuildData(client.getUrl(), build.getName(), build.getNumber(), taskContext, customBuildData);
        } catch (IOException e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        if (!customBuildData.containsKey(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) {
            customBuildData.put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    /**
     * Append the build-infos which were aggregated during the plan execution, and have the same build name and number
     * as the executed Publish Build Info task.
     * The unpublished builds during this task execution are inserted back into the context.
     *
     * @param taskContext           - The task's context.
     * @param build                 - Build object to publish.
     * @param publishCandidatesJson - All build-infos collected during the build execution.
     * @throws IOException if buildInfoToJsonString fails
     */
    private void addBuildsToPublish(TaskContext taskContext, BuildInfo build, String publishCandidatesJson) throws IOException {
        // Deserialize builds string.
        GenericData buildsGenericData = BuildInfoExtractorUtils.jsonStringToGeneric(publishCandidatesJson, GenericData.class);

        // Aggregate build to publish while keeping them for removal from context.
        List<BuildInfo> allBuilds = buildsGenericData.getBuilds();
        List<BuildInfo> buildsToRemove = new ArrayList<>();
        for (BuildInfo buildFromContext : allBuilds) {
            if (buildFromContext.getName().equals(build.getName()) && buildFromContext.getNumber().equals(build.getNumber())) {
                build.append(buildFromContext);
                buildsToRemove.add(buildFromContext);
            }
        }

        // Remove the builds which are published in this task.
        allBuilds.removeAll(buildsToRemove);
        buildsGenericData.setBuilds(allBuilds);

        // Save unpublished builds in context.
        if (buildsGenericData.getBuilds().isEmpty()) {
            return;
        }
        String aggregatedBuildInfo = BuildInfoExtractorUtils.buildInfoToJsonString(buildsGenericData);
        TaskUtils.addBuildInfoToContext(taskContext, aggregatedBuildInfo);
    }

    /**
     * Add the current published build details to the build's build-data map.
     * These details are later used by BuildInfoAction to create the published Build Info UI in the build's summary screen.
     *
     * @param artifactoryUrl  - Artifactory server URL.
     * @param buildName       - Published build name.
     * @param buildNumber     - Published build number.
     * @param taskContext     - The task's context.
     * @param customBuildData - Plan's build-data object, used to store data.
     * @throws IOException if buildInfoToJsonString fails
     */
    private void addPublishedBuildDetailsToBuildData(String artifactoryUrl, String buildName, String buildNumber, TaskContext taskContext, Map<String, String> customBuildData) throws IOException {
        // Get existing PublishedBuilds from context.
        PublishedBuilds pb = new PublishedBuilds();
        if (customBuildData.containsKey(PUBLISHED_BUILDS_DETAILS)) {
            pb = BuildInfoExtractorUtils.jsonStringToGeneric(customBuildData.get(PUBLISHED_BUILDS_DETAILS), PublishedBuilds.class);
        }

        // Merge previous and new published build details.
        PublishedBuildDetails buildDetails = new PublishedBuildDetails(artifactoryUrl, buildName, buildNumber);
        pb.addBuild(buildDetails);

        // Add published build info data to context.
        String pbAsString = BuildInfoExtractorUtils.buildInfoToJsonString(pb);
        taskContext.getBuildContext().getBuildResult().getCustomBuildData().put(PUBLISHED_BUILDS_DETAILS, pbAsString);
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    @Override
    protected String getTaskUsageName() {
        return "publish_build_info";
    }

    @Override
    protected boolean shouldRemoveBuildInfoFromContext() {
        return false;
    }
}
