package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.BuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.CollectBuildIssuesContext;
import org.jfrog.bamboo.util.FileSpecUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Issues;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.issuesCollection.IssuesCollector;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;

public class ArtifactoryCollectBuildIssuesTask extends ArtifactoryTaskType {
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private CollectBuildIssuesContext collectBuildIssuesContext;
    private CustomVariableContext customVariableContext;
    private BuildInfoHelper buildInfoHelper;

    public ArtifactoryCollectBuildIssuesTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
        ContainerManager.autowireComponent(this);
    }

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        collectBuildIssuesContext = new CollectBuildIssuesContext(taskContext.getConfigurationMap());
        BuildParamsOverrideManager buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
        Map<String, String> runtimeContext = context.getRuntimeTaskContext();
        BuildContext buildContext = ((TaskContext) context).getBuildContext();
        buildInfoHelper = BuildInfoHelper.createDeployBuildInfoHelper(collectBuildIssuesContext.getBuildName(buildContext),
                collectBuildIssuesContext.getBuildNumber(buildContext), taskContext, buildContext,
                environmentVariableAccessor, collectBuildIssuesContext.getArtifactoryServerId(),
                collectBuildIssuesContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                collectBuildIssuesContext.getOverriddenPassword(runtimeContext, buildInfoLog, true), buildParamsOverrideManager);
    }

    @NotNull
    @Override
    public TaskResult runTask(@NotNull TaskContext taskContext) {
        try {
            File projectRootDir = getWorkingDirectory();
            if (projectRootDir == null) {
                buildInfoLog.error("No build directory found!");
                return TaskResultBuilder.newBuilder(taskContext).success().build();
            }
            Issues issues = collectBuildIssues(logger, projectRootDir);
            addIssuesToBuildInfoInContext(taskContext, issues);
        } catch (IOException | InterruptedException e) {
            buildInfoLog.error("Exception occurred while executing task", e);
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

    protected File getWorkingDirectory() {
        return TaskUtils.getVcsWorkingDirectory((TaskContext) taskContext);
    }

    /**
     * Reads the issues collection config from the provided source
     *
     * @return the provided issues collection config
     */
    private String getIssuesCollectionConfig(@NotNull CommonTaskContext context, BuildLogger buildLogger) throws IOException {
        if (isConfigSourceTaskConfiguration(context)) {
            buildLogger.addBuildLogEntry("Using task configuration config");
            return getTaskConfigurationConfig(context);
        }
        String configFileLocation = getConfigFilePath(context);
        buildLogger.addBuildLogEntry("Using config from file located at: " + configFileLocation);
        String config = FileSpecUtils.getSpecFromFile(getWorkingDirectory(), configFileLocation);
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
     *
     * @return the provided issues collection config.
     */
    private String getTaskConfigurationConfig(@NotNull CommonTaskContext context) {
        return new CollectBuildIssuesContext(context.getConfigurationMap()).getTaskConfigurationConfig();
    }

    private String getConfigFilePath(@NotNull CommonTaskContext context) {
        return new CollectBuildIssuesContext(context.getConfigurationMap()).getConfigFilePath();
    }

    private Issues collectBuildIssues(BuildLogger logger, File projectRootDir) throws IOException, InterruptedException {
        String config = getIssuesCollectionConfig(taskContext, logger);
        ArtifactoryBuildInfoClientBuilder clientBuilder = buildInfoHelper.getClientBuilder(logger, log);
        String buildName = collectBuildIssuesContext.getBuildName(((TaskContext) taskContext).getBuildContext());
        return new IssuesCollector().collectIssues(projectRootDir, buildInfoLog, config, clientBuilder, buildName);
    }

    private void addIssuesToBuildInfoInContext(@NotNull TaskContext taskContext, Issues issues) {
        Build build = buildInfoHelper.getBuilder(taskContext).build();
        build.setIssues(issues);
        taskBuildInfo = build;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
