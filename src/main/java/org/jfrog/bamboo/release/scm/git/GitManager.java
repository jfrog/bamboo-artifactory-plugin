package org.jfrog.bamboo.release.scm.git;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plugins.git.GitAuthenticationType;
import com.atlassian.bamboo.plugins.git.GitHubRepository;
import com.atlassian.bamboo.plugins.git.GitRepository;
import com.atlassian.bamboo.repository.AbstractRepository;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.security.StringEncrypter;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.opensymphony.xwork.TextProvider;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.release.scm.AbstractScmManager;
import org.shaded.eclipse.jgit.api.Git;
import org.shaded.eclipse.jgit.api.PushCommand;
import org.shaded.eclipse.jgit.lib.Constants;
import org.shaded.eclipse.jgit.lib.ObjectId;
import org.shaded.eclipse.jgit.lib.PersonIdent;
import org.shaded.eclipse.jgit.lib.RefUpdate;
import org.shaded.eclipse.jgit.storage.file.FileRepository;
import org.shaded.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.shaded.eclipse.jgit.transport.JschConfigSessionFactory;
import org.shaded.eclipse.jgit.transport.OpenSshConfig;
import org.shaded.eclipse.jgit.transport.PushResult;
import org.shaded.eclipse.jgit.transport.RefSpec;
import org.shaded.eclipse.jgit.transport.SshSessionFactory;
import org.shaded.eclipse.jgit.transport.SshTransport;
import org.shaded.eclipse.jgit.transport.Transport;
import org.shaded.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.shaded.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Manager that manages the {@link GitRepository}
 *
 * @author Tomer Cohen
 */
public class GitManager extends AbstractScmManager<AbstractRepository> {
    private static final Logger log = Logger.getLogger(GitManager.class);
    private static final String REF_PREFIX = "refs/heads/";
    private static final String REFS_TAGS = "refs/tags/";

    private BuildLogger buildLogger;
    private TextProvider textProvider;
    private String username = "";
    private String password = "";

    public GitManager(BuildContext context, Repository repository, BuildLogger buildLogger) {
        super(context, repository, buildLogger);
        this.buildLogger = buildLogger;
        HierarchicalConfiguration configuration = repository.toConfiguration();
        StringEncrypter encrypter = new StringEncrypter();
        if (repository instanceof GitRepository) {
            username = configuration.getString("repository.git.username", "");
            password = encrypter.decrypt(configuration.getString("repository.git.password", ""));
        } else if (repository instanceof GitHubRepository) {
            username = configuration.getString("repository.github.username", "");
            password = encrypter.decrypt(configuration.getString("repository.github.password", ""));
        }
    }

    @Override
    public void commitWorkingCopy(String commitMessage) throws IOException, InterruptedException {
        Git git = createGitApi();
        try {
            git.commit().setMessage(commitMessage).setAll(true).setCommitter(new PersonIdent(git.getRepository()))
                    .call();
        } catch (Exception e) {
            String message = "An error " + e.getMessage() + " occurred while commiting the working copy";
            log.error(buildLogger.addErrorLogEntry("[RELEASE]" + message));
            throw new IOException(message, e);
        }
    }


    @Override
    public void createTag(String tagUrl, String commitMessage) throws IOException, InterruptedException {
        Git git = createGitApi();
        try {
            git.tag().setMessage(commitMessage).setName(tagUrl).call();
        } catch (Exception e) {
            String message = "An error " + e.getMessage() + " occurred while creating a tag " + tagUrl;
            log.error(buildLogger.addErrorLogEntry("[RELEASE]" + message));
            throw new IOException("An error occurred while creating a tag", e);
        }
    }

    @Override
    public String getRemoteUrl() {
        AbstractRepository scm = getBambooScm();
        HierarchicalConfiguration configuration = scm.toConfiguration();
        if (scm instanceof GitRepository) {
            return configuration.getString("repository.git.repositoryUrl");
        } else if (scm instanceof GitHubRepository) {
            String repository = configuration.getString("repository.github.repository");
            return "https://github.com/" + repository + ".git";
        } else {
            return "";
        }
    }

    public String getCurrentCommitHash() throws IOException {
        File workingDir = getGitDir();
        FileRepository localRepository = null;
        try {
            localRepository = new FileRepository(workingDir);
            ObjectId objId = localRepository.resolve(Constants.HEAD);
            return (objId != null ? objId.getName() : null);
        } catch (IOException e) {
            log.warn(buildLogger
                    .addBuildLogEntry(textProvider.getText("repository.git.messages.cannotDetermineRevision", Arrays
                            .asList(workingDir)) + " " + e.getMessage()), e);
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
            log.warn(buildLogger
                    .addBuildLogEntry(textProvider.getText("repository.git.messages.cannotDetermineRevision", Arrays
                            .asList(workingDir)) + " " + e.getMessage()), e);
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
        Git git = createGitApi();
        log("Checking out branch: " + branch);
        try {
            git.checkout().setCreateBranch(create).setForce(create).setName(branch).call();
        } catch (Exception e) {
            String message = "An error '" + e.getMessage() + "' occurred while checking out branch: " + branch;
            log.error(buildLogger.addErrorLogEntry(message));
            throw new IOException(message, e);
        }
    }

    public void push(String url, String branch) throws IOException {
        Git git = createGitApi();
        PushCommand pushCommand = buildPushCommand(git);
        pushCommand.setRefSpecs(new RefSpec(REF_PREFIX + branch)).setRemote(url);
        Iterable<PushResult> result;
        try {
            log("Pushing branch: " + branch + " to url: " + url);
            result = pushCommand.call();
        } catch (Exception ire) {
            String message =
                    "An error '" + ire.getMessage() + "' occurred while pushing branch: " + branch + " to url: " + url;
            log.error(buildLogger.addErrorLogEntry("[RELEASE] " + message));
            throw new IOException(message, ire);
        }
        for (PushResult pushResult : result) {
            if (StringUtils.isNotBlank(pushResult.getMessages())) {
                log(pushResult.getMessages());

            }
        }
    }

    public void pushTag(String remoteUrl, String tagUrl) throws IOException {
        Git git = createGitApi();
        String escapedTagName = tagUrl.replace(' ', '_');
        Iterable<PushResult> result;
        PushCommand pushCommand = buildPushCommand(git);
        pushCommand.setRefSpecs(new RefSpec(REFS_TAGS + escapedTagName)).setRemote(remoteUrl);
        try {
            log("Pushing tag: " + escapedTagName + " to url " + remoteUrl);
            result = pushCommand.call();
        } catch (Exception ire) {
            String message =
                    "An error '" + ire.getMessage() + "' occurred while pushing tag: " + tagUrl + " to:" + remoteUrl;
            log.error(buildLogger.addErrorLogEntry("[RELEASE] " + message));
            throw new IOException(message, ire);
        }
        for (PushResult pushResult : result) {
            if (StringUtils.isNotBlank(pushResult.getMessages())) {
                log(pushResult.getMessages());
            }
        }
    }

    public void deleteLocalBranch(String branch) throws IOException {
        Git git = createGitApi();
        try {
            log("Deleting local branch: " + branch);
            git.branchDelete().setBranchNames(branch).setForce(true).call();
        } catch (Exception e) {
            String message = "An error '" + e.getMessage() + "' occurred while deleting local branch: " + branch;
            log.error(buildLogger.addErrorLogEntry("[RELEASE] " + message));
            throw new IOException(message, e);
        }
    }

    public void deleteRemoteBranch(String repository, String branch) throws IOException {
        Git git = createGitApi();
        PushCommand pushCommand = buildPushCommand(git);
        pushCommand.setRefSpecs(new RefSpec(":" + REF_PREFIX + branch)).setRemote(repository);
        Iterable<PushResult> results;
        try {
            log("Deleting remote branch: " + branch + " from " + repository);
            results = pushCommand.call();
        } catch (Exception e) {
            String message = "An error '" + e.getMessage() + "' occurred while deleting remote branch: " + branch +
                    " from remote: " + repository;
            log.error(buildLogger.addErrorLogEntry("[RELEASE] " + message));
            throw new IOException(message, e);
        }
        for (PushResult result : results) {
            if (StringUtils.isNotBlank(result.getMessages())) {
                log(result.getMessages());
            }
        }
    }

    public void deleteLocalTag(String tag) throws IOException {
        Git git = createGitApi();
        DeleteTagCommand deleteTagCommand = new DeleteTagCommand(git.getRepository());
        deleteTagCommand.setName(tag);
        try {
            log("Deleting local tag: " + tag);
            RefUpdate.Result result = deleteTagCommand.call();
            log.debug("Result of deletion of local tag: " + result);
        } catch (Exception e) {
            String message = "An error '" + e.getMessage() + "' occurred when deleting local tag: " + tag;
            log.error(buildLogger.addErrorLogEntry("[RELEASE] " + message));
            throw new IOException(message, e);
        }
    }

    public void revertWorkingCopy(String ish) throws IOException {
        Git git = createGitApi();
        log("Reverting local copy to: " + ish);
        org.shaded.eclipse.jgit.lib.Repository repository = git.getRepository();
        ResetCommand resetCommand = new ResetCommand(repository);
        resetCommand.setMode(ResetCommand.ResetType.HARD).setRef(ish).call();
    }

    public void deleteRemoteTag(String repository, String tag) throws IOException {
        Git git = createGitApi();
        PushCommand pushCommand = buildPushCommand(git);
        pushCommand.setRefSpecs(new RefSpec(":" + REFS_TAGS + tag)).setRemote(repository);
        Iterable<PushResult> results;
        try {
            log("Deleting remote tag: " + tag + " from " + repository);
            results = pushCommand.call();
        } catch (Exception e) {
            String message =
                    "An error '" + e.getMessage() + "' occurred when deleting remote tag: " + tag + " from remote: " +
                            repository;
            log.error(buildLogger.addErrorLogEntry("[RELEASE]" + message));
            throw new IOException(message, e);
        }
        for (PushResult result : results) {
            if (StringUtils.isNotBlank(result.getMessages())) {
                log(result.getMessages());
            }
        }
    }

    private PushCommand buildPushCommand(Git git) throws IOException {
        GitAuthenticationType authType = getAuthType();
        PushCommand pushCommand;
        if (authType == GitAuthenticationType.PASSWORD || authType == GitAuthenticationType.NONE) {
            pushCommand = git.push();
            pushCommand.setForce(true);
            UsernamePasswordCredentialsProvider provider = new UsernamePasswordCredentialsProvider(username, password);
            pushCommand.setCredentialsProvider(provider);
        } else {
            Transport transport = createTransport();
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

    private Transport createTransport() throws IOException {
        String url = getRemoteUrl();
        Git git = createGitApi();
        Transport transport = null;
        try {
            GitAuthenticationType authenticationType = getAuthType();
            if (authenticationType.equals(GitAuthenticationType.SSH_KEYPAIR)) {
                transport = Transport.open(git.getRepository(), url);
                if (transport instanceof SshTransport) {
                    AbstractRepository scm = getBambooScm();
                    HierarchicalConfiguration configuration = scm.toConfiguration();
                    StringEncrypter encrypter = new StringEncrypter();
                    String sshKey = encrypter.decrypt(configuration.getString("repository.git.ssh.key", ""));
                    String passphrase = encrypter.decrypt(configuration.getString("repository.git.ssh.passphrase", ""));
                    SshSessionFactory factory = new GitSshSessionFactory(sshKey, passphrase);
                    ((SshTransport) transport).setSshSessionFactory(factory);
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            if (transport != null) {
                transport.close();
            }
        }
        return transport;
    }

    private GitAuthenticationType getAuthType() {
        AbstractRepository scm = getBambooScm();
        if (scm instanceof GitHubRepository) {
            return GitAuthenticationType.PASSWORD;
        }
        HierarchicalConfiguration configuration = scm.toConfiguration();
        String authentication = configuration.getString("repository.git.authenticationType", "");
        return GitAuthenticationType.valueOf(authentication);
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
