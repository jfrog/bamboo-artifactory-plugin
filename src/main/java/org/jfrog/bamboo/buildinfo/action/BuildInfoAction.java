package org.jfrog.bamboo.buildinfo.action;

import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.util.ConstantValues;

/**
 * Build info action to display on successfully completed builds that were run with the build info collection activated
 * This is the view displayed when entering the "Artifactory Build Info" tab from within a job.
 *
 * @author Tomer Cohen
 */
public class BuildInfoAction extends ViewBuildResults {
    transient Logger log = Logger.getLogger(BuildInfoAction.class);
    private String artifactoryReleaseManagementUrl = "";

    @Override
    public String doDefault() throws Exception {
        super.doExecute(); // to populate all the stuff
        if (getUser() == null) {
            return ERROR;
        }
        return INPUT;
    }

    @Override
    public String doExecute() throws Exception {
        String superResult = super.doExecute();

        if (ERROR.equals(superResult)) {
            return ERROR;
        }

        ResultsSummary summary = getBuildResultsSummary();
        if (summary == null) {
            log.error("This build has no results summary");
            return ERROR;
        }

        StringBuilder builder = new StringBuilder(
                summary.getCustomBuildData().get(ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM));
        if (!builder.toString().endsWith("/")) {
            builder.append("/");
        }
        builder.append("webapp/builds/").append(getImmutableBuild().getName()).append("/").append(getBuildNumber());
        artifactoryReleaseManagementUrl = builder.toString();

        return INPUT;
    }

    public String getArtifactoryReleaseManagementUrl() {
        return artifactoryReleaseManagementUrl;
    }

    public void setArtifactoryReleaseManagementUrl(String artifactoryReleaseManagementUrl) {
        this.artifactoryReleaseManagementUrl = artifactoryReleaseManagementUrl;
    }


}
