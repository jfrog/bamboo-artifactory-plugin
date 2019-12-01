package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.CollectIssuesContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.buildInfo.BuildInfoHelper;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Issues;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.issuesCollection.IssuesCollector;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;

public class ArtifactoryCollectIssuesTask extends AbstractSpecTask implements TaskType {
    private static final Logger log = Logger.getLogger(ArtifactoryPublishBuildInfoTask.class);
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    protected CustomVariableContext customVariableContext;
    private TaskContext taskContext;
    private BuildInfoHelper buildInfoHelper;

    public ArtifactoryCollectIssuesTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        this.taskContext = taskContext;
        BuildLogger logger = taskContext.getBuildLogger();
        String previousBiJson = BuildInfoHelper.removeBuildInfoFromContext(taskContext);
        BuildParamsOverrideManager buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        CollectIssuesContext collectIssuesContext = new CollectIssuesContext(taskContext.getConfigurationMap());
        buildInfoHelper = BuildInfoHelper.createBuildInfoHelper(taskContext, taskContext.getBuildContext(),
                environmentVariableAccessor, collectIssuesContext.getArtifactoryServerId(), collectIssuesContext.getUsername(),
                collectIssuesContext.getPassword(), buildParamsOverrideManager);

        try {
            File sourceCodeDirectory = getWorkingDirectory(taskContext);
            if (sourceCodeDirectory == null) {
                log.error(logger.addErrorLogEntry("No build directory found!"));
                return TaskResultBuilder.newBuilder(taskContext).success().build();
            }
            Issues issues = collectIssues(logger, sourceCodeDirectory);
            addIssuesToBuildInfoInContext(taskContext, issues, previousBiJson);

        } catch (IOException | InterruptedException e) {
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
    protected File getWorkingDirectory(@NotNull CommonTaskContext context) {
        return TaskUtils.getVcsWorkingDirectory(this.taskContext);
    }

    /**
     * Reads the config from the provided source
     */
    private String getIssuesCollectionConfig(@NotNull CommonTaskContext context, BuildLogger buildLogger) throws IOException {
        if (isConfigSourceTaskConfiguration(context)) {
            buildLogger.addBuildLogEntry("Using task configuration config");
            return getTaskConfigurationConfig(context);
        }
        String configFileLocation = getConfigFilePath(context);
        buildLogger.addBuildLogEntry("Using config from file located at: " + configFileLocation);
        String config = TaskUtils.getSpecFromFile(getWorkingDirectory(context), configFileLocation);
        return customVariableContext.substituteString(config);
    }

    private boolean isConfigSourceTaskConfiguration(@NotNull CommonTaskContext context) {
        return new CollectIssuesContext(context.getConfigurationMap()).isConfigSourceTaskConfiguration();
    }

    private String getTaskConfigurationConfig(@NotNull CommonTaskContext context) {
        return new CollectIssuesContext(context.getConfigurationMap()).getTaskConfigurationConfig();
    }

    private String getConfigFilePath(@NotNull CommonTaskContext context) {
        return new CollectIssuesContext(context.getConfigurationMap()).getConfigFilePath();
    }

    private String getBuildName(@NotNull TaskContext taskContext) {
        return taskContext.getBuildContext().getPlanName();
    }

    private Issues collectIssues(BuildLogger logger, File sourceCodeDirectory) throws IOException, InterruptedException {
        org.jfrog.build.api.util.Log bambooBuildInfoLog = new BuildInfoLog(log, logger);
        String config = getIssuesCollectionConfig(taskContext, logger);
        ArtifactoryBuildInfoClientBuilder clientBuilder = buildInfoHelper.getClientBuilder(taskContext.getBuildLogger(), log);
        String buildName = getBuildName(taskContext);
        IssuesCollector issuesCollector = new IssuesCollector();
        return issuesCollector.collectIssues(sourceCodeDirectory, bambooBuildInfoLog, config, clientBuilder, buildName);
    }

    private void addIssuesToBuildInfoInContext(@NotNull TaskContext taskContext, Issues issues, String previousBiJson) throws IOException {
        Build build = buildInfoHelper.getBuilder(taskContext).build();
        build.setIssues(issues);
        if (StringUtils.isNotBlank(previousBiJson)) {
            BuildInfoHelper.addBuildInfoToContext(taskContext, previousBiJson);
        }
        BuildInfoHelper.addBuildToContext(taskContext, build);
    }
}
