package org.jfrog.bamboo.release.scm.perforce;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.perforce.PerforceRepository;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.release.scm.AbstractScmManager;
import org.jfrog.build.vcs.perforce.PerforceClient;

import java.io.File;
import java.io.IOException;

/**
 * Manager that manages the {@link PerforceRepository}
 *
 * @author Shay Yaakov
 */
public class PerforceManager extends AbstractScmManager<PerforceRepository> {

    private PerforceClient perforce;
    private EncryptionService encryptionService = (EncryptionService) ContainerManager.getComponent("encryptionService");

    public PerforceManager(BuildContext context, Repository repository, BuildLogger buildLogger) {
        super(context, repository, buildLogger);
    }

    public void prepare() throws IOException {
        PerforceClient.Builder builder = new PerforceClient.Builder();
        PerforceRepository perforceRepository = getBambooScm();
        String hostAddress = perforceRepository.getPort();
        if (!hostAddress.contains(":")) {
            hostAddress = "localhost:" + hostAddress;
        }
        builder.hostAddress(hostAddress).client(perforceRepository.getClient());
        String user = perforceRepository.getUser();
        if (!StringUtils.isEmpty(user)) {
            builder.username(user).password(encryptionService.decrypt(perforceRepository.getEncryptedPassword()));
        }
        String charset = System.getenv("P4CHARSET");
        if (!StringUtils.isBlank(charset)) {
            builder.charset(charset);
        }
        perforce = builder.build();
    }

    public void commitWorkingCopy(int changeListId, String commitMessage) throws IOException {
        perforce.commitWorkingCopy(changeListId, commitMessage);
    }

    @Override
    public void commitWorkingCopy(String commitMessage) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Use the overloaded method");
    }

    public void createTag(String label, String commitMessage, String changeListId) throws IOException {
        perforce.createLabel(label, commitMessage, changeListId);
    }

    @Override
    public void createTag(String tagUrl, String commitMessage) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Use the overloaded method");
    }

    @Override
    public String getRemoteUrl() {
        throw new UnsupportedOperationException("Remote URL not supported");
    }

    public void revertWorkingCopy(int changeListId) throws IOException {
        perforce.revertWorkingCopy(changeListId);
    }

    public void deleteLabel(String tagUrl) throws IOException {
        perforce.deleteLabel(tagUrl);
    }

    public void edit(int changeListId, File releaseVersion) throws IOException {
        perforce.editFile(changeListId, releaseVersion);
    }

    /**
     * Creates a new changelist and returns its id number
     *
     * @return The id of the newly created changelist
     * @throws IOException In case of errors communicating with perforce server
     */
    public int createNewChangeList() throws IOException {
        return perforce.createNewChangeList();
    }

    public void deleteChangeList(int changeListId) throws IOException {
        perforce.deleteChangeList(changeListId);
    }

    public int getDefaultChangeListId() throws IOException {
        return perforce.getDefaultChangeListId();
    }

    public void closeConnection() throws IOException {
        perforce.closeConnection();
    }
}
