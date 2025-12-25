package org.jfrog.bamboo.release.vcs;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfrog.bamboo.release.vcs.git.GitCoordinator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Base class for SCM operations that will be performed by the coordinator.
 *
 * @author Tomer Cohen
 */
public abstract class AbstractVcsCoordinator implements VcsCoordinator {
    private static final Logger log = LogManager.getLogger(AbstractVcsCoordinator.class);
    protected final BuildLogger buildLogger;
    protected final CustomVariableContext customVariableContext;
    protected final CredentialsAccessor credentialsAccessor;
    protected BuildContext context;
    protected boolean modifiedFilesForDevVersion;
    protected boolean modifiedFilesForReleaseVersion;

    public AbstractVcsCoordinator(BuildContext context, BuildLogger buildLogger,
                                  CustomVariableContext customVariableContext, CredentialsAccessor credentialsAccessor) {
        this.context = context;
        this.buildLogger = buildLogger;
        this.customVariableContext = customVariableContext;
        this.credentialsAccessor = credentialsAccessor;
    }

    /**
     * Create an VCS coordinator according to the vcs type
     *
     * @param configuration       The build's configuration.
     * @param credentialsAccessor
     * @return SCM coordinator according to the repository type.
     */
    public static VcsCoordinator createVcsCoordinator(BuildContext context,
                                                      Map<? extends String, ? extends String> configuration,
                                                      BuildLogger buildLogger,
                                                      CustomVariableContext customVariableContext,
                                                      CredentialsAccessor credentialsAccessor) {

        Map<String, String> combined = Maps.newHashMap();
        combined.putAll(configuration);
        Map<String, String> customBuildData = context.getBuildResult().getCustomBuildData();
        combined.putAll(customBuildData);
        // Git is optional SCM so we cannot use the class here
        if (VcsTypes.GIT.name().equals(configuration.get("artifactory.vcs.type"))) {
            return new GitCoordinator(context, combined, buildLogger, customVariableContext, credentialsAccessor);
        }
        throw new UnsupportedOperationException("This VCS type is not supported");
    }

    @Override
    public void beforeDevelopmentVersionChange() throws IOException {
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        modifiedFilesForDevVersion = modified;
    }

    @Override
    public String getCheckoutBranch() {
        return "";
    }

    @Override
    public void setCheckoutBranch(String checkoutBranch) {
    }

    @Override
    public String getCurrentWorkingBranch() {
        return "";
    }

    @Override
    public void setCurrentWorkingBranch(String currentWorkingBranch) {
    }

    @Override
    public String getCommitIsh() {
        return "";
    }

    @Override
    public void setCommitIsh(String commitIsh) {
    }

    @Override
    public boolean isReleaseBranchCreated() {
        return false;
    }

    @Override
    public void setReleaseBranchCreated(boolean releaseBranchCreated) {
    }

    @Override
    public void afterReleaseVersionChange(boolean modified) throws IOException {
        modifiedFilesForReleaseVersion = modified;
    }

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }

    @Override
    public void edit(File file) throws IOException, InterruptedException {
    }

    @Override
    public int getCurrentChangeListId() {
        return 0;
    }

    @Override
    public void setCurrentChangeListId(int currentChangeListId) {
    }
}
