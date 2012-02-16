package org.jfrog.bamboo.release.scm.perforce;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.perforce.PerforceRepository;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import com.tek42.perforce.Depot;
import com.tek42.perforce.PerforceException;
import com.tek42.perforce.model.Label;
import org.jfrog.bamboo.release.scm.AbstractScmManager;
import org.jfrog.bamboo.release.scm.perforce.command.*;

import java.io.File;
import java.io.IOException;

/**
 * Manager that manages the {@link PerforceRepository}
 *
 * @author Shay Yaakov
 */
public class PerforceManager extends AbstractScmManager<PerforceRepository> {

    public PerforceManager(BuildContext context, Repository repository, BuildLogger buildLogger) {
        super(context, repository, buildLogger);
    }

    @Override
    public void commitWorkingCopy(String commitMessage) throws IOException, InterruptedException {
        submit(commitMessage);
    }

    @Override
    public void createTag(String tagUrl, String commitMessage) throws IOException, InterruptedException {
        try {
            Depot depot = getBambooScm().getPerforceDepot();
            Label label = new Label();
            label.setName(tagUrl);
            label.setDescription(commitMessage);
            label.setOwner(depot.getWorkspaces().getWorkspace(depot.getClient()).getOwner());
            Iterable<BuildRepositoryChanges> repositoryChanges = getContext().getBuildChanges().getRepositoryChanges();
            //starting Bamboo 3.3 multiple repositories could be defined for plan.
            //Every entry in the collection has own revisionId.
            //Which should we use for the tag?
            //https://answers.atlassian.com/questions/24077/how-to-get-revision-number-associated-with-build-result-via-the-plugin-sdk
            for (BuildRepositoryChanges repositoryChange : repositoryChanges) {
                label.setRevision("@" + repositoryChange.getVcsRevisionKey());
                break;
            }
            String workspaceViews = depot.getWorkspaces().getWorkspace(depot.getClient()).getViewsAsString();
            String[] viewsStrArr = workspaceViews.split(" ");
            label.addView(viewsStrArr[0]);
            depot.getLabels().saveLabel(label);
        } catch (PerforceException e) {
            throw new IOException("Failed to revert changelist: " + e.getMessage());
        }
    }

    @Override
    public String getRemoteUrl() {
        throw new UnsupportedOperationException("Remote URL not supported");
    }

    public void edit(File file) throws IOException {
        Depot depot = getBambooScm().getPerforceDepot();
        try {
            new Edit(depot, file).editFile();
        } catch (PerforceException e) {
            throw new IOException("Failed to edit file: " + e.getMessage());
        }
    }

    private void submit(String message) throws IOException {
        Depot depot = getBambooScm().getPerforceDepot();
        try {
            new Submit(depot).submit(message);
        } catch (PerforceException e) {
            throw new IOException("Failed to submit changelist: " + e.getMessage());
        }
    }

    public void revertWorkingCopy() throws IOException {
        try {
            Depot depot = getBambooScm().getPerforceDepot();
            new Revert(depot).revert();
        } catch (PerforceException e) {
            throw new IOException("Failed to revert changelist: " + e.getMessage());
        }
    }

    public void deleteLabel(String labelName) throws IOException {
        try {
            Depot depot = getBambooScm().getPerforceDepot();
            new DeleteLabel(depot, labelName).deleteLabel();
        } catch (PerforceException e) {
            throw new IOException("Failed to delete label: " + e.getMessage());
        }
    }

    public void shelveWorkingCopy() throws IOException {
        try {
            Depot depot = getBambooScm().getPerforceDepot();
            new Shelve(depot).shelve();
        } catch (PerforceException e) {
            throw new IOException("Failed to shelve working copy: " + e.getMessage());
        }
    }

    public void unshelveToWorkingCopy(int changeListNum) throws IOException {
        try {
            Depot depot = getBambooScm().getPerforceDepot();
            new Unshelve(depot).unshelve(changeListNum);
        } catch (PerforceException e) {
            throw new IOException("Failed to unshelve to working copy: " + e.getMessage());
        }
    }
}
