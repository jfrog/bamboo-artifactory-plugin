package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.CustomPreBuildAction;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.release.provider.AbstractReleaseProvider;
import org.jfrog.bamboo.release.provider.ReleaseProvider;
import org.jfrog.bamboo.util.TaskDefinitionHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This event is fired <b>before</b> the actual build starts. It will determine if this is a valid build for release
 * (Maven/Gradle) and will determine if this is build is running in release mode, if it is it will fire the appropriate
 * events.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPreBuildAction extends AbstractBuildAction implements CustomPreBuildAction {
    private static final Logger log = Logger.getLogger(ArtifactoryPreBuildAction.class);

    private BuildLoggerManager buildLoggerManager;

    @NotNull
    public BuildContext call() throws Exception {
        PlanKey planKey = PlanKeys.getPlanKey(buildContext.getPlanKey());
        BuildLogger logger = buildLoggerManager.getBuildLogger(planKey);
        setBuildLogger(logger);
        logger.startStreamingBuildLogs(buildContext.getPlanResultKey());
        List<TaskDefinition> definitions = buildContext.getBuildDefinition().getTaskDefinitions();
        if (definitions.isEmpty()) {
            log("No task definitions found for this build");
            return buildContext;
        }
        TaskDefinition definition = TaskDefinitionHelper.findMavenOrGradleTask(definitions);
        if (definition == null) {
            log.debug("[RELEASE] Build is not a Maven or Gradle build");
            return buildContext;
        }
        Map<String, String> configuration = definition.getConfiguration();
        BuildContext parentBuildContext = buildContext.getParentBuildContext();
        if (parentBuildContext == null) {
            log.debug("[RELEASE] Release management is not active, resuming normally");
            return buildContext;
        }
        Map<String, String> customBuildData = parentBuildContext.getBuildResult().getCustomBuildData();
        configuration.putAll(customBuildData);
        AbstractBuildContext config = AbstractBuildContext.createContextFromMap(configuration);
        if ((config == null) || !config.releaseManagementContext.isActivateReleaseManagement()) {
            log.debug("[RELEASE] Release management is not active, resuming normally");
            return buildContext;
        }
        ReleaseProvider provider = AbstractReleaseProvider.createReleaseProvider(config, buildContext, logger);
        if (provider == null) {
            String message = "Release Provider could not be built";
            log.error(logger.addBuildLogEntry(message));
            buildContext.getBuildResult().addBuildErrors(Arrays.asList(message));
            return buildContext;
        }
        log.info(logger.addBuildLogEntry("[RELEASE] Release Build Active"));
        provider.prepare();
        provider.beforeReleaseVersionChange();
        boolean modified = provider.transformDescriptor(configuration, true);
        customBuildData.put(ReleaseProvider.MODIFIED_FILES_FOR_RELEASE, String.valueOf(modified));
        customBuildData.put(ReleaseProvider.CURRENT_CHECKOUT_BRANCH, provider.getCurrentCheckoutBranch());
        customBuildData.put(ReleaseProvider.CURRENT_WORKING_BRANCH, provider.getCurrentWorkingBranch());
        customBuildData.put(ReleaseProvider.BASE_COMMIT_ISH, provider.getBaseCommitIsh());
        customBuildData.put(ReleaseProvider.RELEASE_BRANCH_CREATED, String.valueOf(provider.isReleaseBranchCreated()));
        return buildContext;
    }

    public void setBuildLoggerManager(BuildLoggerManager buildLoggerManager) {
        this.buildLoggerManager = buildLoggerManager;
    }
}