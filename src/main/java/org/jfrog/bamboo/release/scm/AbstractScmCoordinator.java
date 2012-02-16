package org.jfrog.bamboo.release.scm;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.perforce.PerforceRepository;
import com.atlassian.bamboo.repository.svn.SvnRepository;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.release.scm.git.GitCoordinator;
import org.jfrog.bamboo.release.scm.perforce.PerforceCoordinator;
import org.jfrog.bamboo.release.scm.svn.SubversionCoordinator;
import org.jfrog.bamboo.util.version.ScmHelper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Base class for SCM operations that will be performed by the coordinator.
 *
 * @author Tomer Cohen
 */
public abstract class AbstractScmCoordinator implements ScmCoordinator {
    private static final Logger log = Logger.getLogger(AbstractScmCoordinator.class);

    protected BuildContext context;
    protected final Repository repository;
    protected boolean modifiedFilesForDevVersion;
    protected boolean modifiedFilesForReleaseVersion;
    protected final BuildLogger buildLogger;

    public AbstractScmCoordinator(BuildContext context, Repository repository, BuildLogger buildLogger) {
        this.context = context;
        this.repository = repository;
        this.buildLogger = buildLogger;
    }

    /**
     * Create an SCM coordinator according to the {@link Repository} type, either {@link SvnRepository} or a {@link
     * com.atlassian.bamboo.plugins.git.GitRepository}
     *
     * @param configuration The build's configuration.
     * @return SCM coordinator according to the repository type.
     */
    public static ScmCoordinator createScmCoordinator(BuildContext context,
                                                      Map<? extends String, ? extends String> configuration, BuildLogger buildLogger) {

        Repository repository = ScmHelper.getRepository(context);
        Map<String, String> combined = Maps.newHashMap();
        combined.putAll(configuration);
        Map<String, String> customBuildData = context.getBuildResult().getCustomBuildData();
        combined.putAll(customBuildData);
        if (repository instanceof SvnRepository) {
            return new SubversionCoordinator(context, repository, combined, buildLogger);
        }
        // Git is optional SCM so we cannot use the class here
        if (isGitScm(repository)) {
            return new GitCoordinator(context, repository, combined, buildLogger);
        }
        if (repository instanceof PerforceRepository) {
            return new PerforceCoordinator(context, repository, combined, buildLogger);
        }
        throw new UnsupportedOperationException(
                "Scm of type: " + repository.getClass().getName() + " is not supported");
    }

    /**
     * @return Whether this repository is a git repository.
     */
    private static boolean isGitScm(Repository repository) {
        return "com.atlassian.bamboo.plugins.git.GitRepository".equals(repository.getClass().getName()) ||
                "com.atlassian.bamboo.plugins.git.GitHubRepository".equals(repository.getClass().getName());
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
    public void setCommitIsh(String commitIsh) {
    }

    @Override
    public String getCommitIsh() {
        return "";
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
}
