package org.jfrog.bamboo.release.vcs.git;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.struts.TextProvider;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.release.vcs.AbstractVcsManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;

/**
 * Manager that manages the Git repository.
 *
 * @author Tomer Cohen
 */
public class GitManager extends AbstractVcsManager {
    private static final Logger log = Logger.getLogger(GitManager.class);
    private static final String REF_PREFIX = "refs/heads/";
    private static final String REFS_TAGS = "refs/tags/";
    private BuildLogger buildLogger;
    private TextProvider textProvider;
    private String username = "";
    private String password = "";
    private String url = "";
    private String sshKey = "";
    private String sshPassphrase = "";
    private String authenticationType = "";

    public GitManager(BuildContext context, BuildLogger buildLogger) {
        super(context, buildLogger);
        this.buildLogger = buildLogger;
        initVcsConfiguration();
    }

    private void initVcsConfiguration() {
        Map<String, String> confMap = getTaskConfiguration();
        if (confMap != null) {
            url = confMap.get(PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_URL);
            authenticationType = confMap.get(PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_AUTHENTICATION_TYPE);
            username = confMap.get(PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_USERNAME);
            password = confMap.get(PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_PASSWORD);
            sshKey = confMap.get(PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_SSH_KEY);
            sshPassphrase = confMap.get(PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_PASSPHRASE);
        }
    }

    @Override
    public void commitWorkingCopy(String commitMessage) throws IOException, InterruptedException {
        try (Git git = createGitApi()) {
            git.commit().setMessage(commitMessage).setAll(true).setCommitter(new PersonIdent(git.getRepository())).call();
        } catch (Exception e) {
            String message = "An error " + e.getMessage() + " occurred while committing the working copy";
            log.error("[RELEASE] " + message, e);
            buildLogger.addErrorLogEntry("[RELEASE] " + message, e);
            throw new IOException(message, e);
        }
    }


    @Override
    public void createTag(String tagUrl, String commitMessage) throws IOException, InterruptedException {
        try (Git git = createGitApi()) {
            git.tag().setMessage(commitMessage).setName(tagUrl).call();
        } catch (Exception e) {
            String message = "An error " + e.getMessage() + " occurred while creating a tag " + tagUrl;
            log.error("[RELEASE] " + message, e);
            buildLogger.addErrorLogEntry("[RELEASE] " + message, e);
            throw new IOException("An error occurred while creating a tag", e);
        }
    }

    @Override
    public String getRemoteUrl() {
        return url;
    }

    public String getCurrentCommitHash() throws IOException {
        File workingDir = getGitDir();
        FileRepository localRepository = null;
        try {
            localRepository = new FileRepository(workingDir);
            ObjectId objId = localRepository.resolve(Constants.HEAD);
            return (objId != null ? objId.getName() : null);
        } catch (IOException e) {
            String message = textProvider.getText("repository.git.messages.cannotDetermineRevision", Arrays
                    .asList(workingDir)) + " " + e.getMessage();
            log.warn(message, e);
            buildLogger.addErrorLogEntry(message, e);
            return null;
        } finally {
            if (localRepository != null) {
                localRepository.close();
            }
        }
    }

    public String getCurrentBranch() throws IOException {
        File workingDir = getGitDir();
        FileRepository localRepository = null;
        try {
            localRepository = new FileRepository(workingDir);
            return localRepository.getBranch();
        } catch (IOException e) {
            String message = textProvider.getText("repository.git.messages.cannotDetermineRevision", Arrays
                    .asList(workingDir)) + " " + e.getMessage();
            log.warn(message, e);
            buildLogger.addErrorLogEntry(message, e);
            return null;
        } finally {
            if (localRepository != null) {
                localRepository.close();
            }
        }
    }

    public void setBuildLogger(BuildLogger buildLogger) {
        this.buildLogger = buildLogger;
    }

    public void setTextProvider(TextProvider textProvider) {
        this.textProvider = textProvider;
    }

    public void checkoutBranch(final String branch, final boolean create) throws IOException {
        log("Checking out branch: " + branch);
        try (Git git = createGitApi()) {
            git.checkout().setCreateBranch(create).setForce(create).setName(branch).call();
        } catch (Exception e) {
            String message = "An error '" + e.getMessage() + "' occurred while checking out branch: " + branch;
            log.error(message, e);
            buildLogger.addErrorLogEntry(message, e);
            throw new IOException(message, e);
        }
    }

    public void push(String url, String branch) throws IOException {
        try (Git git = createGitApi()) {
            PushCommand pushCommand = buildPushCommand(git);
            pushCommand.setRefSpecs(new RefSpec(REF_PREFIX + branch)).setRemote(url);
            Iterable<PushResult> result;
            log("Pushing branch: " + branch + " to url: " + url);
            try {
                result = pushCommand.call();
            } catch (Exception ire) {
                String message =
                        "An error '" + ire.getMessage() + "' occurred while pushing branch: " + branch + " to url: " + url;
                log.error("[RELEASE] " + message, ire);
                buildLogger.addErrorLogEntry("[RELEASE] " + message, ire);
                throw new IOException(message, ire);
            }
            for (PushResult pushResult : result) {
                if (StringUtils.isNotBlank(pushResult.getMessages())) {
                    log(pushResult.getMessages());
                }
            }
        }
    }

    public void pushTag(String remoteUrl, String tagUrl) throws IOException {
        String escapedTagName = tagUrl.replace(' ', '_');
        Iterable<PushResult> result;
        try (Git git = createGitApi()) {
            PushCommand pushCommand = buildPushCommand(git);
            pushCommand.setRefSpecs(new RefSpec(REFS_TAGS + escapedTagName)).setRemote(remoteUrl);
            log("Pushing tag: " + escapedTagName + " to url " + remoteUrl);
            try {
                result = pushCommand.call();
            } catch (Exception ire) {
                String message =
                        "An error '" + ire.getMessage() + "' occurred while pushing tag: " + tagUrl + " to:" + remoteUrl;
                log.error("[RELEASE] " + message, ire);
                buildLogger.addErrorLogEntry("[RELEASE] " + message, ire);
                throw new IOException(message, ire);
            }
            for (PushResult pushResult : result) {
                if (StringUtils.isNotBlank(pushResult.getMessages())) {
                    log(pushResult.getMessages());
                }
            }
        }
    }

    public void deleteLocalBranch(String branch) throws IOException {
        try (Git git = createGitApi()) {
            log("Deleting local branch: " + branch);
            git.branchDelete().setBranchNames(branch).setForce(true).call();
        } catch (Exception e) {
            String message = "An error '" + e.getMessage() + "' occurred while deleting local branch: " + branch;
            log.error("[RELEASE] " + message, e);
            buildLogger.addErrorLogEntry("[RELEASE] " + message, e);
            throw new IOException(message, e);
        }
    }

    public void deleteRemoteBranch(String repository, String branch) throws IOException {
        Iterable<PushResult> results;
        try (Git git = createGitApi()) {
            PushCommand pushCommand = buildPushCommand(git);
            pushCommand.setRefSpecs(new RefSpec(":" + REF_PREFIX + branch)).setRemote(repository);
            log("Deleting remote branch: " + branch + " from " + repository);
            try {
                results = pushCommand.call();
            } catch (Exception e) {
                String message = "An error '" + e.getMessage() + "' occurred while deleting remote branch: " + branch +
                        " from remote: " + repository;
                log.error("[RELEASE] " + message, e);
                buildLogger.addErrorLogEntry("[RELEASE] " + message, e);
                throw new IOException(message, e);
            }
            for (PushResult result : results) {
                if (StringUtils.isNotBlank(result.getMessages())) {
                    log(result.getMessages());
                }
            }
        }
    }

    public void deleteLocalTag(String tag) throws IOException {
        try (Git git = createGitApi()) {
            DeleteTagCommand deleteTagCommand = new DeleteTagCommand(git.getRepository());
            deleteTagCommand.setName(tag);
            log("Deleting local tag: " + tag);
            try {
                RefUpdate.Result result = deleteTagCommand.call();
                log.debug("Result of deletion of local tag: " + result);
            } catch (Exception e) {
                String message = "An error '" + e.getMessage() + "' occurred when deleting local tag: " + tag;
                log.error("[RELEASE] " + message, e);
                buildLogger.addErrorLogEntry("[RELEASE] " + message, e);
                throw new IOException(message, e);
            }
        }
    }

    public void revertWorkingCopy(String ish) throws GitAPIException, IOException {
        log("Reverting local copy to: " + ish);
        try (Git git = createGitApi()) {
            org.eclipse.jgit.lib.Repository repository = git.getRepository();
            ResetCommand resetCommand = new ResetCommand(repository);
            resetCommand.setMode(ResetCommand.ResetType.HARD).setRef(ish).call();
        }
    }

    public void deleteRemoteTag(String repository, String tag) throws IOException {
        try (Git git = createGitApi()) {
            PushCommand pushCommand = buildPushCommand(git);
            pushCommand.setRefSpecs(new RefSpec(":" + REFS_TAGS + tag)).setRemote(repository);
            Iterable<PushResult> results;
            log("Deleting remote tag: " + tag + " from " + repository);
            try {
                results = pushCommand.call();
            } catch (Exception e) {
                String message =
                        "An error '" + e.getMessage() + "' occurred when deleting remote tag: " + tag + " from remote: " +
                                repository;
                log.error("[RELEASE] " + message, e);
                buildLogger.addErrorLogEntry("[RELEASE] " + message, e);
                throw new IOException(message, e);
            }
            for (PushResult result : results) {
                if (StringUtils.isNotBlank(result.getMessages())) {
                    log(result.getMessages());
                }
            }
        }
    }

    private PushCommand buildPushCommand(Git git) throws IOException {
        GitAuthenticationType authType = getAuthType();
        buildLogger.addBuildLogEntry("[RELEASE] Using SCM authentication type: " + authType);
        PushCommand pushCommand;
        if (authType == GitAuthenticationType.PASSWORD || authType == GitAuthenticationType.NONE) {
            pushCommand = git.push();
            pushCommand.setForce(true);
            UsernamePasswordCredentialsProvider provider = new UsernamePasswordCredentialsProvider(username, password);
            pushCommand.setCredentialsProvider(provider);
        } else {
            Transport transport = createSshTransport();
            pushCommand = new SshPushCommand(git.getRepository(), transport);
        }
        return pushCommand;
    }

    private Git createGitApi() throws IOException {
        File workingDir = getGitDir();
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.setGitDir(workingDir);
        return new Git(builder.setup().build());
    }

    private File getGitDir() {
        File gitDirectory = new File(getAndValidateCheckoutDirectory(), Constants.DOT_GIT);
        if (!gitDirectory.exists()) {
            throw new IllegalStateException("Git Dir: " + gitDirectory.getAbsolutePath() + " Does not exist");
        }
        return gitDirectory;
    }

    private Transport createSshTransport() throws IOException {
        String url = getRemoteUrl();
        Transport transport = null;
        try (Git git = createGitApi()) {
                transport = Transport.open(git.getRepository(), url);
                if (transport instanceof SshTransport) {
                    SshSessionFactory factory = new GitSshSessionFactory(sshKey, sshPassphrase);
                    ((SshTransport) transport).setSshSessionFactory(factory);
                }
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
            buildLogger.addErrorLogEntry(e.getMessage(), e);
            throw new IOException(e);
        } finally {
            if (transport != null) {
                transport.close();
            }
        }
        return transport;
    }

    private GitAuthenticationType getAuthType() {
        if (GitAuthenticationType.SSH_KEYPAIR.name().equals(authenticationType)) {
            return GitAuthenticationType.SSH_KEYPAIR;
        }
        if (GitAuthenticationType.PASSWORD.name().equals(authenticationType)) {
            return GitAuthenticationType.PASSWORD;
        }
        return GitAuthenticationType.NONE;
    }

    private static class GitSshSessionFactory extends JschConfigSessionFactory {

        final private String key;
        final private String passphrase;

        GitSshSessionFactory(@Nullable final String key, @Nullable final String passphrase) {
            this.key = key;
            this.passphrase = passphrase;
        }

        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        @Override
        protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
            JSch jsch = super.getJSch(hc, fs);
            jsch.removeAllIdentity();
            if (StringUtils.isNotEmpty(key)) {
                jsch.addIdentity("identityName", key.getBytes(), null, passphrase.getBytes());
            }
            return jsch;
        }
    }
}
