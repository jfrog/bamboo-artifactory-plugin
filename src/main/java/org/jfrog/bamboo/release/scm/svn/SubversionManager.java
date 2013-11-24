package org.jfrog.bamboo.release.scm.svn;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.repository.svn.SvnRepository;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.release.scm.AbstractScmManager;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.io.IOException;

/**
 * @author Tomer Cohen
 */
public class SubversionManager extends AbstractScmManager<SvnRepository> {
    private static final Logger log = Logger.getLogger(SubversionManager.class);

    private SVNClientManager manager;
    private final BuildLogger buildLogger;
    private CustomVariableContext customVariableContext;

    public SubversionManager(BuildContext context, Repository repository, SVNClientManager manager,
                             BuildLogger buildLogger, CustomVariableContext customVariableContext) {
        super(context, repository, buildLogger);
        this.manager = manager;
        this.buildLogger = buildLogger;
        this.customVariableContext = customVariableContext;
    }

    @Override
    public void commitWorkingCopy(String commitMessage) throws IOException, InterruptedException {
        try {
            File checkoutDir = getAndValidateCheckoutDirectory();
            SVNCommitClient commitClient = manager.getCommitClient();
            log(String.format("Committing working copy: '%s'", checkoutDir));
            log(commitMessage);
            SVNCommitInfo commitInfo = commitClient.doCommit(new File[]{checkoutDir}, true,
                    commitMessage, null, null, true, true, SVNDepth.INFINITY);
            SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
            if (errorMessage != null) {
                throw new IOException("Failed to commit working copy: " + errorMessage.getFullMessage());
            }
        } catch (SVNException e) {
            String message = "[RELEASE] An error " + e.getMessage() + " occurred while committing the working copy";
            log.error(buildLogger.addBuildLogEntry(message));
            throw new IOException(message, e);
        }
    }

    @Override
    public void createTag(String tagUrl, String commitMessage) throws IOException, InterruptedException {
        try {
            tagUrl = customVariableContext.substituteString(tagUrl);
            SVNURL svnUrl = SVNURL.parseURIEncoded(tagUrl);
            SVNCopyClient copyClient = manager.getCopyClient();
            log("Creating subversion tag: " + tagUrl);
            File checkoutDir = getAndValidateCheckoutDirectory();
            SVNCopySource source = new SVNCopySource(SVNRevision.WORKING, SVNRevision.WORKING, checkoutDir);
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
        }
    }

    /**
     * Revert all the working copy changes.
     */
    public void revertWorkingCopy() throws IOException, InterruptedException, RepositoryException {
        File checkoutDir = getAndValidateCheckoutDirectory();
        SVNWCClient wcClient = manager.getWCClient();
        log("Reverting working copy: " + checkoutDir);
        try {
            wcClient.doRevert(new File[]{checkoutDir}, SVNDepth.INFINITY, null);
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
        File checkoutDir = getAndValidateCheckoutDirectory();
        SVNWCClient wcClient = manager.getWCClient();
        try {
            log("Cleanup working copy: " + checkoutDir);
            wcClient.doCleanup(checkoutDir);
        } catch (SVNException e) {
            String message = "[RELEASE] Failed to revert working copy on the 2nd attempt: " + e.getLocalizedMessage();
            log.error(buildLogger.addErrorLogEntry(message));
            throw new IOException(message, e);
        }
    }

    @Override
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
