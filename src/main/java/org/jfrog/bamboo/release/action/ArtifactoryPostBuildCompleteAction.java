package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.CustomBuildProcessor;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.release.provider.AbstractReleaseProvider;
import org.jfrog.bamboo.release.provider.ReleaseProvider;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.version.ScmHelper;

import java.util.List;
import java.util.Map;

/**
 * Listener that is called <b>after</b> the build has been completed. It will determine the state of the build, and
 * whether the build ran in a release mode, and will fire the appropriate events if so.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPostBuildCompleteAction extends AbstractBuildAction implements CustomBuildProcessor {
    private static final Logger log = Logger.getLogger(ArtifactoryPostBuildCompleteAction.class);

    private BuildLoggerManager buildLoggerManager;
    private CustomVariableContext customVariableContext;
    private CredentialsAccessor credentialsAccessor;

    @Override
    @NotNull
    public BuildContext call() throws Exception {
        BuildLogger logger = buildLoggerManager.getLogger(buildContext.getPlanResultKey());
        setBuildLogger(logger);
        List<TaskDefinition> taskDefinitions = buildContext.getBuildDefinition().getTaskDefinitions();
        TaskDefinition mavenOrGradleDefinition = TaskDefinitionHelper.findMavenOrGradleDefinition(taskDefinitions);
        if (mavenOrGradleDefinition == null) {
            log.debug("[RELEASE] Task definition is not Maven or Gradle");
            return buildContext;
        }
        Map<String, String> configuration = mavenOrGradleDefinition.getConfiguration();
        BuildContext parentBuildContext = buildContext.getParentBuildContext();
        if (parentBuildContext == null) {
            log.debug("[RELEASE] No parent build context found, resuming normally");
            return buildContext;
        }
        Map<String, String> customBuildData = parentBuildContext.getBuildResult().getCustomBuildData();
        configuration.putAll(customBuildData);
        AbstractBuildContext config = AbstractBuildContext.createContextFromMap(configuration);
        if ((config == null) || !config.releaseManagementContext.isActivateReleaseManagement()) {
            log.debug("[RELEASE] Release management is not active, resuming normally");
            return buildContext;
        }

        ReleaseProvider provider = AbstractReleaseProvider.createReleaseProvider(config, buildContext, logger,
                customVariableContext, credentialsAccessor);
        if (provider == null) {
            return buildContext;
        }
        // re-prepare the provider, since this is a while new object.
        provider.prepare();
        try {
            // set again the working branch/checkout branch/ and the base commit hash (this is for git mainly) that was
            // brought from the pre-release action, this is due to the fact that the current provider/coordinator
            // are completely new objects, and need to be set to the proper state prior to continuing
            provider.reloadFromConfig(configuration);
            provider.afterReleaseVersionChange(
                    Boolean.parseBoolean(customBuildData.get(ReleaseProvider.MODIFIED_FILES_FOR_RELEASE)));
            CurrentBuildResult result = buildContext.getBuildResult();
            if (BuildState.SUCCESS.equals(result.getBuildState())) {
                log("Build completed successfully");
                provider.afterSuccessfulReleaseVersionBuild();
                provider.beforeDevelopmentVersionChange();
                Repository repository = ScmHelper.getRepository(buildContext);
                if (repository == null) {
                    log("No VCS repository found, resuming normally");
                    return buildContext;
                }
                boolean modified = provider.transformDescriptor(configuration, false);
                provider.afterDevelopmentVersionChange(modified);
            }
        } finally {
            // always call this method, since if the build failed, there will be a way to revert the working copy
            // and tags.
            provider.buildCompleted(buildContext);
        }
        return buildContext;
    }

    public void setBuildLoggerManager(BuildLoggerManager buildLoggerManager) {
        this.buildLoggerManager = buildLoggerManager;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    public void setCredentialsAccessor(CredentialsAccessor credentialsAccessor) {
        this.credentialsAccessor = credentialsAccessor;
    }
}
