package org.jfrog.bamboo.release.scm.git;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.release.scm.AbstractScmCoordinator;

import java.io.IOException;
import java.util.Map;

/**
 * Git coordinator that will perform SCM operations that are specific to the GIT SCM.
 *
 * @author Tomer Cohen
 */
public class GitCoordinator extends AbstractScmCoordinator {
    private GitManager scmManager;
    private String releaseBranch;
    private String checkoutBranch;
    // the commit hash of the initial checkout
    private String baseCommitIsh;
    private State state = new State();
    private final Map<String, String> configuration;
    private final BuildLogger buildLogger;

    private static class State {
        String currentWorkingBranch;
        boolean releaseBranchCreated;
        boolean releaseBranchPushed;
        boolean tagCreated;
        boolean tagPushed;
    }

    public GitCoordinator(BuildContext context, Repository repository, Map<String, String> configuration,
                          BuildLogger buildLogger, CustomVariableContext customVariableContext, CredentialsAccessor credentialsAccessor) {
        super(context, repository, buildLogger, customVariableContext, credentialsAccessor);
        this.configuration = configuration;
        this.buildLogger = buildLogger;
    }

    @Override
    public void prepare() throws IOException {
        releaseBranch = configuration.get(AbstractBuildContext.ReleaseManagementContext.RELEASE_BRANCH);
        scmManager = new GitManager(context, repository, buildLogger, customVariableContext, credentialsAccessor);
        baseCommitIsh = scmManager.getCurrentCommitHash();
        checkoutBranch = scmManager.getCurrentBranch();
    }

    @Override
    public void beforeReleaseVersionChange() throws IOException {
        if (Boolean.parseBoolean(configuration.get(AbstractBuildContext.ReleaseManagementContext.USE_RELEASE_BRANCH))) {
            scmManager.checkoutBranch(releaseBranch, true);
            state.currentWorkingBranch = releaseBranch;
            state.releaseBranchCreated = true;
        } else {
            // make sure we are on the checkout branch
            scmManager.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;
        }
    }

    @Override
    public void afterSuccessfulReleaseVersionBuild() throws IOException, InterruptedException {
        if (modifiedFilesForReleaseVersion) {
            // commit local changes
            log(String.format("Committing release version on branch '%s'", checkoutBranch));
            String comment = configuration.get(AbstractBuildContext.ReleaseManagementContext.TAG_COMMENT);
            if (StringUtils.isBlank(comment)) {
                comment = "";
            }
            scmManager.commitWorkingCopy(comment);
        }
        if (Boolean.parseBoolean(configuration.get(AbstractBuildContext.ReleaseManagementContext.CREATE_VCS_TAG))) {
            scmManager.createTag(configuration.get(AbstractBuildContext.ReleaseManagementContext.TAG_URL),
                    configuration.get(AbstractBuildContext.ReleaseManagementContext.TAG_COMMENT));
            state.tagCreated = true;
        }
        if (state.releaseBranchCreated) {
            // push the current branch
            scmManager.push(scmManager.getRemoteUrl(), state.currentWorkingBranch);
            state.releaseBranchPushed = true;
        }
        if (Boolean.parseBoolean(configuration.get(AbstractBuildContext.ReleaseManagementContext.CREATE_VCS_TAG))) {
            // push the tag
            scmManager.pushTag(scmManager.getRemoteUrl(),
                    configuration.get(AbstractBuildContext.ReleaseManagementContext.TAG_URL));
            state.tagPushed = true;
        }
    }

    @Override
    public void beforeDevelopmentVersionChange() throws IOException {
        if (Boolean.parseBoolean(configuration.get(AbstractBuildContext.ReleaseManagementContext.USE_RELEASE_BRANCH))) {
            // done working on the release branch, checkout back to master
            scmManager.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;
        }
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        super.afterDevelopmentVersionChange(modified);
        if (modified) {
            log(String.format("Committing next development version on branch '%s'", state.currentWorkingBranch));
            String comment = configuration.get(AbstractBuildContext.ReleaseManagementContext.NEXT_DEVELOPMENT_COMMENT);
            if (StringUtils.isBlank(comment)) {
                comment = "";
            }
            scmManager.commitWorkingCopy(comment);
        }
    }

    @Override
    public void buildCompleted(BuildContext buildContext) throws IOException, InterruptedException {
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        CurrentBuildResult result = buildContext.getBuildResult();
        if (BuildState.SUCCESS.equals(result.getBuildState())) {
            // pull before attempting to push changes?
            //scmManager.pull(scmManager.getRemoteUrl(), checkoutBranch);
            if (modifiedFilesForDevVersion) {
                scmManager.push(scmManager.getRemoteUrl(), checkoutBranch);
            }
        } else {
            // go back to the original checkout branch (required to delete the release branch and reset the working copy)
            scmManager.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;

            if (state.releaseBranchCreated) {
                safeDeleteBranch(releaseBranch);
            }
            if (state.releaseBranchPushed) {
                safeDeleteRemoteBranch(scmManager.getRemoteUrl(), releaseBranch);
            }
            if (state.tagCreated) {
                safeDeleteTag(context.releaseManagementContext.getTagUrl());
            }
            if (state.tagPushed) {
                safeDeleteRemoteTag(scmManager.getRemoteUrl(), context.releaseManagementContext.getTagUrl());
            }
            // reset changes done on the original checkout branch (next dev version)
            safeRevertWorkingCopy();
        }
    }

    @Override
    public String getRemoteUrlForPom() {
        return null;
    }

    private void safeDeleteBranch(String branch) {
        try {
            scmManager.deleteLocalBranch(branch);
        } catch (Exception e) {
            log(buildLogger.addBuildLogEntry("Failed to delete release branch: " + e.getLocalizedMessage()));
        }
    }

    private void safeDeleteRemoteBranch(String remoteRepository, String branch) {
        try {
            scmManager.deleteRemoteBranch(remoteRepository, branch);
        } catch (Exception e) {
            log(buildLogger.addBuildLogEntry("Failed to delete remote release branch: " + e.getLocalizedMessage()));
        }
    }

    private void safeDeleteTag(String tag) {
        try {
            scmManager.deleteLocalTag(tag);
        } catch (Exception e) {
            log(buildLogger.addBuildLogEntry("Failed to delete tag: " + e.getLocalizedMessage()));
        }
    }

    private void safeDeleteRemoteTag(String remoteRepository, String tag) {
        try {
            scmManager.deleteRemoteTag(remoteRepository, tag);
        } catch (Exception e) {
            log(buildLogger.addBuildLogEntry("Failed to delete remote tag: " + e.getLocalizedMessage()));
        }
    }

    private void safeRevertWorkingCopy() {
        try {
            scmManager.revertWorkingCopy(baseCommitIsh);
        } catch (Exception e) {
            log(buildLogger.addBuildLogEntry("Failed to revert working copy: " + e.getLocalizedMessage()));
        }
    }

    @Override
    public String getCheckoutBranch() {
        return checkoutBranch;
    }

    @Override
    public boolean isReleaseBranchCreated() {
        return state.releaseBranchCreated;
    }

    @Override
    public void setReleaseBranchCreated(boolean releaseBranchCreated) {
        state.releaseBranchCreated = releaseBranchCreated;
    }

    @Override
    public boolean isSubversion() {
        return false;
    }

    @Override
    public void setCheckoutBranch(String checkoutBranch) {
        this.checkoutBranch = checkoutBranch;
    }

    @Override
    public String getCurrentWorkingBranch() {
        return state.currentWorkingBranch;
    }

    @Override
    public void setCurrentWorkingBranch(String currentWorkingBranch) {
        state.currentWorkingBranch = currentWorkingBranch;
    }

    @Override
    public void setCommitIsh(String commitIsh) {
        this.baseCommitIsh = commitIsh;
    }

    @Override
    public String getCommitIsh() {
        return this.baseCommitIsh;
    }
}
