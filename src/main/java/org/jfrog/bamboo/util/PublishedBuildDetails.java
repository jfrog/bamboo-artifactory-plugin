package org.jfrog.bamboo.util;

import java.io.Serializable;

/**
 * Created by Bar Belity on 05/10/2020.
 */
public class PublishedBuildDetails implements Serializable {

    private String artifactoryUrl;
    private String buildName;
    private String buildNumber;
    private String buildUrl;

    @SuppressWarnings({"UnusedDeclaration"})
    public PublishedBuildDetails() {} // Empty constructor for serialization.

    public PublishedBuildDetails(String artifactoryUrl, String buildName, String buildNumber) {
        this.artifactoryUrl = artifactoryUrl;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
    }

    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }
}
