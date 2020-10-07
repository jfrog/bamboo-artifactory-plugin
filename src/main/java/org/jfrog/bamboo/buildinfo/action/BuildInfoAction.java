package org.jfrog.bamboo.buildinfo.action;

import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.PublishedBuildDetails;
import org.jfrog.bamboo.util.PublishedBuilds;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Build info action to display on successfully completed builds that were run with the build info collection activated
 * This is the view displayed when entering the "Artifactory Build Info" tab from within a job.
 *
 * @author Tomer Cohen
 */
public class BuildInfoAction extends ViewBuildResults {
    transient Logger log = Logger.getLogger(BuildInfoAction.class);
    private List<PublishedBuildDetails> publishedBuildsDetails = new ArrayList<>();

    @Override
    public String execute() throws Exception {
        String superResult = super.execute();

        if (ERROR.equals(superResult)) {
            return ERROR;
        }

        ResultsSummary summary = getBuildResultsSummary();
        if (summary == null) {
            log.error("This build has no results summary");
            return ERROR;
        }

        String pbAsString = summary.getCustomBuildData().get(ConstantValues.PUBLISHED_BUILDS_DETAILS);
        if (StringUtils.isNotBlank(pbAsString)) {
            PublishedBuilds pb = BuildInfoExtractorUtils.jsonStringToGeneric(pbAsString, PublishedBuilds.class);
            publishedBuildsDetails = pb.getBuilds();
        }

        // Backward compatibility for non-customizable build name and number.
        if (publishedBuildsDetails.isEmpty()) {
            publishedBuildsDetails.add(createDefaultPublishedBuildDetails(summary));
        }

        // Create and set url for each published build.
        for (PublishedBuildDetails publishedBuildDetails : publishedBuildsDetails) {
            StringBuilder urlStringBuilder = new StringBuilder(publishedBuildDetails.getArtifactoryUrl());
            if (!urlStringBuilder.toString().endsWith("/")) {
                urlStringBuilder.append("/");
            }
            urlStringBuilder.append("webapp/builds/").append(publishedBuildDetails.getBuildName()).append("/").append(publishedBuildDetails.getBuildNumber());
            publishedBuildDetails.setBuildUrl(urlStringBuilder.toString());
        }

        return INPUT;
    }

    /**
     * Create published-build details based on the values used prior to allowing customizable build name and number in tasks.
     * @param summary - Build results summary object.
     * @return The default published-build details to show in the 'Build-Info' summary page.
     */
    private PublishedBuildDetails createDefaultPublishedBuildDetails(ResultsSummary summary) {
        String artifactoryUrl = summary.getCustomBuildData().get(ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM);
        String buildName = getImmutableBuild().getName();
        String buildNumber = String.valueOf(getBuildNumber());
        return new PublishedBuildDetails(artifactoryUrl, buildName, buildNumber);
    }

    public List<PublishedBuildDetails> getPublishedBuildsDetails() {
        return publishedBuildsDetails;
    }

    public void setPublishedBuildsDetails(List<PublishedBuildDetails> publishedBuildsDetails) {
        this.publishedBuildsDetails = publishedBuildsDetails;
    }
}
