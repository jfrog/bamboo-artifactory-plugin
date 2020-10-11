package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.CollectBuildIssuesContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Issues;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.issuesCollection.IssuesCollector;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;

public class ArtifactoryCollectBuildIssuesTask extends ArtifactoryTaskType {
    private static final Logger log = Logger.getLogger(ArtifactoryCollectBuildIssuesTask.class);
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private CustomVariableContext customVariableContext;
    private TaskContext taskContext;
    private BuildInfoHelper buildInfoHelper;
    private BuildLogger logger;
    private CollectBuildIssuesContext collectBuildIssuesContext;

    public ArtifactoryCollectBuildIssuesTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
    }

    @Override
    protected void initTask(@NotNull TaskContext context) {
        taskContext = context;
        logger = taskContext.getBuildLogger();
        collectBuildIssuesContext = new CollectBuildIssuesContext(taskContext.getConfigurationMap());
        BuildParamsOverrideManager buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(collectBuildIssuesContext.getBuildName(taskContext.getBuildContext()),
                collectBuildIssuesContext.getBuildNumber(taskContext.getBuildContext()), taskContext, taskContext.getBuildContext(),
                environmentVariableAccessor, collectBuildIssuesContext.getArtifactoryServerId(), collectBuildIssuesContext.getUsername(),
                collectBuildIssuesContext.getPassword(), buildParamsOverrideManager);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        try {
            File projectRootDir = getWorkingDirectory(taskContext);
            if (projectRootDir == null) {
                log.error(logger.addErrorLogEntry("No build directory found!"));
                return TaskResultBuilder.newBuilder(taskContext).success().build();
            }
            Issues issues = collectBuildIssues(logger, projectRootDir);
            addIssuesToBuildInfoInContext(taskContext, issues);
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
    protected ServerConfig getUsageServerConfig() {
        return buildInfoHelper.getServerConfig();
    }

    @Override
    protected String getTaskUsageName() {
        return "collect_build_issues";
    }

    @Override
    protected Log getLog() {
        return new BuildInfoLog(log, logger);
    }

    protected File getWorkingDirectory(@NotNull CommonTaskContext context) {
        return TaskUtils.getVcsWorkingDirectory(this.taskContext);
    }

    /**
     * Reads the issues collection config from the provided source
     * @return the provided issues collection config
     */
    private String getIssuesCollectionConfig(@NotNull CommonTaskContext context, BuildLogger buildLogger) throws IOException {
        if (isConfigSourceTaskConfiguration(context)) {
            buildLogger.addBuildLogEntry("Using task configuration config");
            return getTaskConfigurationConfig(context);
        }
        String configFileLocation = getConfigFilePath(context);
        buildLogger.addBuildLogEntry("Using config from file located at: " + configFileLocation);
        String config = FileSpecUtils.getSpecFromFile(getWorkingDirectory(context), configFileLocation);
        return customVariableContext.substituteString(config);
    }

    /**
     * @return true if the issues collection config json is provided via task configuration.
     */
    private boolean isConfigSourceTaskConfiguration(@NotNull CommonTaskContext context) {
        return new CollectBuildIssuesContext(context.getConfigurationMap()).isConfigSourceTaskConfiguration();
    }

    /**
     * When the source of the issues collection config is task configuration, the config json is provided via a text box
     * in the task.
     * This function retreives the config from the task's text box.
     * @return the provided issues collection config.
     */
    private String getTaskConfigurationConfig(@NotNull CommonTaskContext context) {
        return new CollectBuildIssuesContext(context.getConfigurationMap()).getTaskConfigurationConfig();
    }

    private String getConfigFilePath(@NotNull CommonTaskContext context) {
        return new CollectBuildIssuesContext(context.getConfigurationMap()).getConfigFilePath();
    }

    private Issues collectBuildIssues(BuildLogger logger, File projectRootDir) throws IOException, InterruptedException {
        org.jfrog.build.api.util.Log bambooBuildInfoLog = new BuildInfoLog(log, logger);
        String config = getIssuesCollectionConfig(taskContext, logger);
        ArtifactoryBuildInfoClientBuilder clientBuilder = buildInfoHelper.getClientBuilder(taskContext.getBuildLogger(), log);
        String buildName = collectBuildIssuesContext.getBuildName(taskContext.getBuildContext());
        return new IssuesCollector().collectIssues(projectRootDir, bambooBuildInfoLog, config, clientBuilder, buildName);
    }

    private void addIssuesToBuildInfoInContext(@NotNull TaskContext taskContext, Issues issues) throws IOException {
        Build build = buildInfoHelper.getBuilder(taskContext).build();
        build.setIssues(issues);
        taskBuildInfo = build;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
