package org.jfrog.bamboo.release.scm.svn;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.repository.svn.SvnRepository;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.release.scm.AbstractScmManager;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;
import java.io.IOException;

/**
 * @author Tomer Cohen
 */
public class SubversionManager extends AbstractScmManager<SvnRepository> {
    private static final Logger log = Logger.getLogger(SubversionManager.class);

    private SVNClientManager manager;
    private final PlanKey planKey;
    private final BuildLogger buildLogger;

    public SubversionManager(Repository repository, SVNClientManager manager, PlanKey planKey,
            BuildLogger buildLogger) {
        super(repository, buildLogger);
        this.manager = manager;
        this.planKey = planKey;
        this.buildLogger = buildLogger;
    }

    public void commitWorkingCopy(String commitMessage) throws IOException, InterruptedException {
        try {
            File workingCopy = getBambooScm().getSourceCodeDirectory(planKey);
            SVNCommitClient commitClient = manager.getCommitClient();
            log(String.format("Committing working copy: '%s'", workingCopy));
            log(commitMessage);
            SVNCommitInfo commitInfo = commitClient.doCommit(new File[]{workingCopy}, true,
                    commitMessage, null, null, true, true, SVNDepth.INFINITY);
            SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
            if (errorMessage != null) {
                throw new IOException("Failed to commit working copy: " + errorMessage.getFullMessage());
            }
        } catch (SVNException e) {
            String message = "[RELEASE] An error " + e.getMessage() + " occurred while committing the working copy";
            log.error(buildLogger.addBuildLogEntry(message));
            throw new IOException(message, e);
        } catch (RepositoryException e) {
            String message = "[RELEASE] An error " + e.getMessage() + " occurred while committing the working copy";
            log.error(buildLogger.addBuildLogEntry(message));
            throw new IOException(e);
        }
    }

    public void createTag(String tagUrl, String commitMessage) throws IOException, InterruptedException {
        try {
            SVNURL svnUrl = SVNURL.parseURIEncoded(tagUrl);
            File workingCopy = getBambooScm().getSourceCodeDirectory(planKey);
            SVNCopyClient copyClient = manager.getCopyClient();
            log("Creating subversion tag: " + tagUrl);
            SVNCopySource source = new SVNCopySource(SVNRevision.WORKING, SVNRevision.WORKING, workingCopy);
            SVNCommitInfo commitInfo = copyClient.doCopy(new SVNCopySource[]{source},
                    svnUrl, false, true, true, commitMessage, new SVNProperties());
            SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
            if (errorMessage != null) {
                throw new IOException("Failed to create tag: " + errorMessage.getFullMessage());
            }
        } catch (SVNException e) {
            String message = "[RELEASE] An error " + e.getMessage() + " occurred while creating tag: " + tagUrl;
            log.error(buildLogger.addBuildLogEntry(message));
            throw new IOException("Subversion tag creation failed: " + e.getMessage());
        } catch (RepositoryException e) {
            String message = "[RELEASE] An error " + e.getMessage() + " occurred while creating tag: " + tagUrl;
            log.error(buildLogger.addBuildLogEntry(message));
            throw new IOException("Subversion tag creation failed: " + e.getMessage());
        }
    }

    /**
     * Revert all the working copy changes.
     */
    public void revertWorkingCopy() throws IOException, InterruptedException, RepositoryException {
        File workingCopy = getBambooScm().getSourceCodeDirectory(planKey);
        SVNWCClient wcClient = manager.getWCClient();
        log("Reverting working copy: " + workingCopy);
        try {
            wcClient.doRevert(new File[]{workingCopy}, SVNDepth.INFINITY, null);
        } catch (SVNException e) {
            log.error(buildLogger
                    .addErrorLogEntry("[RELEASE] Failed to revert working copy: " + e.getLocalizedMessage()));
            throw new IOException(e);
        }
    }

    /**
     * Attempts to revert the working copy. In case of failure it just logs the error.
     */
    public void safeRevertWorkingCopy() {
        try {
            revertWorkingCopy();
        } catch (Exception e) {
            log.debug("Failed to revert working copy", e);
            log("Failed to revert working copy: " + e.getLocalizedMessage());
            Throwable cause = e.getCause();
            if (!(cause instanceof SVNException)) {
                return;
            }
            SVNException svnException = (SVNException) cause;
            if (svnException.getErrorMessage().getErrorCode() == SVNErrorCode.WC_LOCKED) {
                // work space locked attempt cleanup and try to revert again
                try {
                    cleanupWorkingCopy();
                } catch (Exception unlockException) {
                    log.debug("Failed to cleanup working copy", e);
                    log.error(
                            buildLogger.addErrorLogEntry(
                                    "[RELEASE] Failed to cleanup working copy: " + e.getLocalizedMessage()));
                }
                try {
                    revertWorkingCopy();
                } catch (Exception revertException) {
                    log.error(buildLogger.addErrorLogEntry(
                            "[RELEASE] Failed to revert working copy on the 2nd attempt: " + e.getLocalizedMessage()));
                }
            }
        }
    }

    private void cleanupWorkingCopy() throws IOException, InterruptedException, RepositoryException {
        File workingCopy = getBambooScm().getSourceCodeDirectory(planKey);
        SVNWCClient wcClient = manager.getWCClient();
        try {
            log("Cleanup working copy: " + workingCopy);
            wcClient.doCleanup(workingCopy);
        } catch (SVNException e) {
            String message = "[RELEASE] Failed to revert working copy on the 2nd attempt: " + e.getLocalizedMessage();
            log.error(buildLogger.addErrorLogEntry(message));
            throw new IOException(message, e);
        }
    }

    public String getRemoteUrl() {
        return getBambooScm().getRepositoryUrl();
    }

    public void safeRevertTag(String tagUrl, String comment) {
        try {
            log("Reverting subversion tag: " + tagUrl);
            SVNURL svnUrl = SVNURL.parseURIEncoded(tagUrl);
            SVNCommitClient commitClient = manager.getCommitClient();
            SVNCommitInfo commitInfo = commitClient.doDelete(new SVNURL[]{svnUrl}, COMMENT_PREFIX + comment);
            SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
            if (errorMessage != null) {
                log.error(buildLogger
                        .addBuildLogEntry(
                                "[RELEASE] Failed to revert '" + tagUrl + "': " + errorMessage.getFullMessage()));
            }
        } catch (SVNException e) {
            String message = "[RELEASE] Failed to revert '" + tagUrl + "': " + e.getLocalizedMessage();
            log.error(buildLogger.addErrorLogEntry(message));
        }
    }
}
