package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.PublishBuildInfoContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.generic.GenericData;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.IOException;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;
import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryPublishBuildInfoTask extends ArtifactoryTaskType {
    public static final String TASK_NAME = "artifactoryPublishBuildInfoTask";

    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private static final Logger log = Logger.getLogger(ArtifactoryPublishBuildInfoTask.class);
    private BuildLogger logger;
    private CustomVariableContext customVariableContext;
    private BuildInfoHelper buildInfoHelper;
    private BuildParamsOverrideManager buildParamsOverrideManager;

    public ArtifactoryPublishBuildInfoTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @Override
    protected void initTask(@NotNull TaskContext context) {
        logger = context.getBuildLogger();
        PublishBuildInfoContext publishBuildInfoContext = new PublishBuildInfoContext(context.getConfigurationMap());
        buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(context, context.getBuildContext(), environmentVariableAccessor, publishBuildInfoContext.getArtifactoryServerId(), publishBuildInfoContext.getUsername(), publishBuildInfoContext.getPassword(), buildParamsOverrideManager);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) throws TaskException {
        String buildInfoJson = TaskUtils.getAndDeleteAggregatedBuildInfo(taskContext);
        Build build = buildInfoHelper.getBuilder(taskContext).build();
        ArtifactoryBuildInfoClientBuilder clientBuilder = buildInfoHelper.getClientBuilder(taskContext.getBuildLogger(), log);
        try (ArtifactoryBuildInfoClient client = clientBuilder.build()){
            if (StringUtils.isNotBlank(buildInfoJson)) {
                GenericData genericData = BuildInfoExtractorUtils.jsonStringToGeneric(buildInfoJson, GenericData.class);
                for (Build buildFromContext : genericData.getBuilds()) {
                    build.append(buildFromContext);
                }
            }
            client.sendBuildInfo(build);
            taskContext.getBuildContext().getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM, client.getArtifactoryUrl());
        } catch (IOException e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        Map<String, String> customBuildData = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        if (!customBuildData.containsKey(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) {
            customBuildData.put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
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
    protected Log getLog() {
        return new BuildInfoLog(log, logger);
    }

    @Override
    protected boolean shouldRemoveBuildInfoFromContext() {
        return false;
    }
}
