package org.jfrog.bamboo.deployment;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.IOException;

import static org.jfrog.bamboo.util.TaskUtils.getArtifactoryDependenciesClient;

/**
 * Bamboo Download from Artifactory in deployment task - Download files from Artifactory in order to use them in deployment task.
 *
 * @author Yahav Itzhak
 */
public class ArtifactoryDeploymentDownloadTask implements DeploymentTaskType {
    private static final Logger log = Logger.getLogger(ArtifactoryDeploymentDownloadTask.class);
    private BuildParamsOverrideManager buildParamsOverrideManager;

    public ArtifactoryDeploymentDownloadTask(CustomVariableContext customVariableContext) {
        this.buildParamsOverrideManager = new BuildParamsOverrideManager(customVariableContext);
    }

    @NotNull
    public TaskResult execute(@NotNull DeploymentTaskContext deploymentTaskContext) {
        BuildLogger logger = deploymentTaskContext.getBuildLogger();
        GenericContext genericContext = new GenericContext(deploymentTaskContext.getConfigurationMap());
        ArtifactoryDependenciesClient client = getArtifactoryDependenciesClient(genericContext, buildParamsOverrideManager, log);
        try {
            org.jfrog.build.api.util.Log bambooBuildInfoLog = new BuildInfoLog(log, logger);
            String spec = genericContext.isFileSpecInJobConfiguration() ? genericContext.getJobConfigurationSpec() : TaskUtils.getSpecFromFile(deploymentTaskContext.getWorkingDirectory(), genericContext.getFilePathSpec());
            if (StringUtils.isNotBlank(spec)) {
                SpecsHelper specsHelper = new SpecsHelper(bambooBuildInfoLog);
                specsHelper.downloadArtifactsBySpec(spec, client, deploymentTaskContext.getWorkingDirectory().getCanonicalPath());
            }
        } catch (IOException e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        } finally {
            client.close();
        }
        return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
    }
}
