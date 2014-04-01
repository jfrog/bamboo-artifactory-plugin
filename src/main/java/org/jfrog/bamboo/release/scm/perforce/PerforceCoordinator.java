package org.jfrog.bamboo.release.scm.perforce;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.release.scm.AbstractScmCoordinator;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Perforce coordinator that will perform SCM operations that are specific to the Perforce SCM.
 *
 * @author Shay Yaakov
 */
public class PerforceCoordinator extends AbstractScmCoordinator {

    private PerforceManager perforce;

    private final Map<String, String> configuration;
    private boolean tagCreated;
    private int currentChangeListId;

    public PerforceCoordinator(BuildContext context, Repository repository, Map<String, String> configuration,
                               BuildLogger buildLogger, CustomVariableContext customVariableContext, CredentialsAccessor credentialsAccessor) {
        super(context, repository, buildLogger, customVariableContext, credentialsAccessor);
        this.configuration = configuration;
    }

    @Override
    public void prepare() throws IOException {
        perforce = new PerforceManager(context, repository, buildLogger);
        perforce.prepare();
    }

    @Override
    public void beforeReleaseVersionChange() throws IOException {
        currentChangeListId = perforce.createNewChangeList();
    }

    @Override
    public void afterSuccessfulReleaseVersionBuild() throws IOException, InterruptedException {
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        AbstractBuildContext.ReleaseManagementContext releaseManagementContext = context.releaseManagementContext;
        String labelChangeListId = configuration.get("repository.revision.number");
        if (modifiedFilesForReleaseVersion) {
            log("Submitting release version changes");
            labelChangeListId = currentChangeListId + "";
            perforce.commitWorkingCopy(currentChangeListId, releaseManagementContext.getTagComment());
        } else {
            safeRevertWorkingCopy();
            currentChangeListId = perforce.getDefaultChangeListId();
        }

        if (releaseManagementContext.isCreateVcsTag()) {
            log("Creating label: '" + releaseManagementContext.getTagUrl() + "' with change list id: " + labelChangeListId);
            perforce.createTag(releaseManagementContext.getTagUrl(), releaseManagementContext.getTagComment(),
                    labelChangeListId);
            tagCreated = true;
        }
    }

    @Override
    public void beforeDevelopmentVersionChange() throws IOException {
        currentChangeListId = perforce.getDefaultChangeListId();
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        super.afterDevelopmentVersionChange(modified);
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        AbstractBuildContext.ReleaseManagementContext releaseManagementContext = context.releaseManagementContext;
        if (modified) {
            log("Submitting next development version changes");
            perforce.commitWorkingCopy(currentChangeListId, releaseManagementContext.getNextDevelopmentComment());
        } else {
            safeRevertWorkingCopy();
            currentChangeListId = perforce.getDefaultChangeListId();
        }
    }

    @Override
    public void edit(File file) throws IOException, InterruptedException {
        log("Opening file: '" + file.getAbsolutePath() + "' for editing");
        perforce.edit(currentChangeListId, file);
    }

    @Override
    public void buildCompleted(BuildContext buildContext) throws IOException, InterruptedException {
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        CurrentBuildResult result = buildContext.getBuildResult();
        if (!BuildState.SUCCESS.equals(result.getBuildState())) {
            safeRevertWorkingCopy();
            if (tagCreated) {
                safeDeleteLabel(context.releaseManagementContext.getTagUrl());
            }
        } else {
            log("Closing connection to perforce server");
            perforce.closeConnection();
        }
    }

    private void safeRevertWorkingCopy() {
        log("Reverting local changes");
        try {
            perforce.revertWorkingCopy(currentChangeListId);
        } catch (Exception e) {
            log("Failed to revert: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteLabel(String label) throws IOException {
        log("Deleting label '" + label + "'");
        try {
            perforce.deleteLabel(label);
        } catch (Exception e) {
            log("Failed to delete label: " + e.getLocalizedMessage());
        }
    }

    @Override
    public String getRemoteUrlForPom() {
        return null;
    }

    @Override
    public boolean isSubversion() {
        return false;
    }

    @Override
    public int getCurrentChangeListId() {
        return currentChangeListId;
    }

    @Override
    public void setCurrentChangeListId(int currentChangeListId) {
        this.currentChangeListId = currentChangeListId;
    }
}
