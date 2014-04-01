package org.jfrog.bamboo.release.provider;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;
import org.jfrog.bamboo.release.scm.AbstractScmCoordinator;
import org.jfrog.bamboo.release.scm.ScmCoordinator;
import org.jfrog.bamboo.util.version.ScmHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Tomer Cohen
 */
public abstract class AbstractReleaseProvider implements ReleaseProvider {
    private static final Logger log = Logger.getLogger(AbstractReleaseProvider.class);

    private boolean isReleaseEnabled;
    protected ScmCoordinator coordinator;
    protected final AbstractBuildContext buildContext;
    protected final BuildLogger buildLogger;
    protected final BuildContext context;

    protected AbstractReleaseProvider(AbstractBuildContext buildContext, BuildContext context,
                                      BuildLogger buildLogger, CustomVariableContext customVariableContext, CredentialsAccessor credentialsAccessor) {
        this.context = context;
        this.buildContext = buildContext;
        this.buildLogger = buildLogger;
        this.isReleaseEnabled = buildContext.releaseManagementContext.isReleaseMgmtEnabled();
        this.coordinator = AbstractScmCoordinator.createScmCoordinator(context,
                getTaskConfiguration(context.getBuildDefinition()), buildLogger, customVariableContext, credentialsAccessor);
    }

    protected abstract Map<? extends String, ? extends String> getTaskConfiguration(BuildDefinition definition);

    public static ReleaseProvider createReleaseProvider(AbstractBuildContext buildContext, BuildContext context,
                                                        BuildLogger buildLogger,
                                                        CustomVariableContext customVariableContext,
                                                        CredentialsAccessor credentialsAccessor) {
        if (buildContext instanceof GradleBuildContext) {
            return new GradleReleaseProvider(buildContext, context, buildLogger, customVariableContext, credentialsAccessor);
        }
        if (buildContext instanceof Maven3BuildContext) {
            return new MavenReleaseProvider(buildContext, context, buildLogger, customVariableContext, credentialsAccessor);
        }
        return null;
    }

    @Override
    public void prepare() throws IOException {
        if (isReleaseEnabled) {
            try {
                coordinator.prepare();
            } catch (Exception e) {
                failBuild(e);
                throw new IOException(e);
            }
        }
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException {
        try {
            coordinator.afterDevelopmentVersionChange(modified);
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    @Override
    public void beforeReleaseVersionChange() throws IOException, InterruptedException {
        try {
            coordinator.beforeReleaseVersionChange();
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    @Override
    public void afterReleaseVersionChange(boolean modified) throws IOException {
        try {
            coordinator.afterReleaseVersionChange(modified);
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    @Override
    public void afterSuccessfulReleaseVersionBuild() throws IOException {
        try {
            coordinator.afterSuccessfulReleaseVersionBuild();
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    @Override
    public void beforeDevelopmentVersionChange() throws IOException {
        try {
            coordinator.beforeDevelopmentVersionChange();
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    @Override
    public void buildCompleted(BuildContext buildContext) throws IOException {
        try {
            coordinator.buildCompleted(buildContext);
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    private void failBuild(Exception e) {
        CurrentBuildResult result = context.getBuildResult();
        result.addBuildErrors(Arrays.asList(e.getMessage()));
        result.setBuildState(BuildState.FAILED);
    }

    @Override
    public String getCurrentCheckoutBranch() {
        return coordinator.getCheckoutBranch();
    }

    @Override
    public void setCurrentCheckoutBranch(String checkoutBranch) {
        coordinator.setCheckoutBranch(checkoutBranch);
    }

    @Override
    public String getCurrentWorkingBranch() {
        return coordinator.getCurrentWorkingBranch();
    }

    @Override
    public void setCurrentWorkingBranch(String currentWorkingBranch) {
        coordinator.setCurrentWorkingBranch(currentWorkingBranch);
    }

    @Override
    public void setBaseCommitIsh(String commitIsh) {
        coordinator.setCommitIsh(commitIsh);
    }

    @Override
    public String getBaseCommitIsh() {
        return coordinator.getCommitIsh();
    }

    @Override
    public void setReleaseBranchCreated(boolean releaseBranchCreated) {
        coordinator.setReleaseBranchCreated(releaseBranchCreated);
    }

    @Override
    public boolean isReleaseBranchCreated() {
        return coordinator.isReleaseBranchCreated();
    }

    protected Map<String, String> buildMapAccordingToStatus(Map<String, String> conf, boolean release) {
        List<ModuleVersionHolder> holders = ModuleVersionHolder.buildFromConf(conf);
        Map<String, String> result = Maps.newHashMap();
        for (ModuleVersionHolder holder : holders) {
            String value = release ? holder.getReleaseValue() : holder.getNextIntegValue();
            result.put(holder.getKey(), value);
        }
        return result;
    }

    @Nullable
    protected File getSourceDir() throws RepositoryException {
        return ScmHelper.getCheckoutDirectory(context);
    }

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }

    @Override
    public int getCurrentChangeListId() {
        return coordinator.getCurrentChangeListId();
    }

    @Override
    public void setCurrentChangeListId(int changeListId) {
        coordinator.setCurrentChangeListId(changeListId);
    }

    @Override
    public void reloadFromConfig(Map<String, String> configuration) {
        // Git variables
        String checkoutBranch = configuration.get(ReleaseProvider.CURRENT_CHECKOUT_BRANCH);
        setCurrentCheckoutBranch(checkoutBranch);
        String workingBranch = configuration.get(ReleaseProvider.CURRENT_WORKING_BRANCH);
        setCurrentWorkingBranch(workingBranch);
        String baseCommitIsh = configuration.get(ReleaseProvider.BASE_COMMIT_ISH);
        setBaseCommitIsh(baseCommitIsh);
        String releaseBranchCreated = configuration.get(ReleaseProvider.RELEASE_BRANCH_CREATED);
        setReleaseBranchCreated(Boolean.parseBoolean(releaseBranchCreated));

        // Perforce variables
        String currentChangeListId = configuration.get(ReleaseProvider.CURRENT_CHANGE_LIST_ID);
        setCurrentChangeListId(Integer.parseInt(currentChangeListId));
    }
}
