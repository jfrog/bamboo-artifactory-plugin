package org.jfrog.bamboo.release.scm.perforce;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
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

    private PerforceManager perforceManager;
    private final Map<String, String> configuration;
    private boolean tagCreated;

    public PerforceCoordinator(BuildContext context, Repository repository, Map<String, String> configuration, BuildLogger buildLogger) {
        super(context, repository, buildLogger);
        this.configuration = configuration;
    }

    @Override
    public void prepare() throws IOException {
        perforceManager = new PerforceManager(context, repository, buildLogger);
    }

    @Override
    public void beforeReleaseVersionChange() throws IOException {

    }

    @Override
    public void afterSuccessfulReleaseVersionBuild() throws IOException, InterruptedException {
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        AbstractBuildContext.ReleaseManagementContext releaseManagementContext = context.releaseManagementContext;
        if (modifiedFilesForReleaseVersion) {
            log("Submitting release version changes");
            perforceManager.commitWorkingCopy(releaseManagementContext.getTagComment());

            /*log("Shelving release version");
            Depot perforceDepot = perforceManager.getBambooScm().getPerforceDepot();
            perforceManager.shelveWorkingCopy(releaseManagementContext.getReleaseProps());
            perforceManager.revertWorkingCopy();
            //perforceManager.commitWorkingCopy(releaseManagementContext.getNextDevelopmentComment());
            try {
                List<Integer> changeNumbers = perforceDepot.getChanges().getChangeNumbers(perforceManager.getBambooScm().getDepot(), -1, 1);
                releaseShelveChangeList = changeNumbers.get(0);
            } catch (PerforceException e) {
                log("Failed to retrieve latest change number: " + e.getLocalizedMessage());
            }*/
        }

        if (releaseManagementContext.isCreateVcsTag()) {
            log("Creating label: '" + releaseManagementContext.getTagUrl() + "'");
            perforceManager.createTag(releaseManagementContext.getTagUrl(), releaseManagementContext.getTagComment());
            tagCreated = true;
        }
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        super.afterDevelopmentVersionChange(modified);
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        AbstractBuildContext.ReleaseManagementContext releaseManagementContext = context.releaseManagementContext;
        if (modified) {
            // submit the next development version
            log("Submitting next development version changes");
            perforceManager.commitWorkingCopy(context.releaseManagementContext.getNextDevelopmentComment());

            /*log("Shelving next development version");
            perforceManager.shelveWorkingCopy(releaseManagementContext.getNextIntegProps());
            perforceManager.revertWorkingCopy();
            Depot perforceDepot = perforceManager.getBambooScm().getPerforceDepot();
            try {
                List<Integer> changeNumbers = perforceDepot.getChanges().getChangeNumbers(perforceManager.getBambooScm().getDepot(), -1, 1);
                devShelveChangeList = changeNumbers.get(0);
            } catch (PerforceException e) {
                log("Failed to retrieve latest change number: " + e.getLocalizedMessage());
            }*/
        }
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
        }
    }

    private void safeRevertWorkingCopy() {
        log("Reverting local changes");
        try {
            perforceManager.revertWorkingCopy();
        } catch (Exception e) {
            log("Failed to revert: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteLabel(String tagUrl) throws IOException {
        log("Deleting label '" + tagUrl + "'");
        try {
            perforceManager.deleteLabel(tagUrl);
        } catch (Exception e) {
            log("Failed to delete label: " + e.getLocalizedMessage());
        }
    }

    @Override
    public String getRemoteUrlForPom() {
        return null;
    }

    @Override
    public boolean isGit() {
        return false;
    }

    @Override
    public void edit(File file) throws IOException, InterruptedException {
        perforceManager.edit(file);
    }
}
