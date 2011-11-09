package org.jfrog.bamboo.release.configuration;

import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;

/**
 * @author Tomer Cohen
 */
public class ArtifactoryBuildReleaseConfiguration implements ArtifactoryReleaseConfiguration {

    private final BuildConfiguration buildConfiguration;

    public ArtifactoryBuildReleaseConfiguration(BuildConfiguration buildConfiguration) {
        this.buildConfiguration = buildConfiguration;
    }


    public void setReleaseEnabled(boolean flag) {
        buildConfiguration.setProperty(RELEASE_ENABLED_KEY, flag);
    }

    public boolean isReleaseEnabled() {
        return buildConfiguration.getBoolean(RELEASE_ENABLED_KEY);
    }

    public void setReleaseTaggerEnabled(boolean flag) {
    }

    public boolean isReleaseTaggerEnabled() {
        return false;
    }

    public void setReleaseBrancherEnabled(boolean flag) {
    }

    public boolean isReleaseBrancherEnabled() {
        return false;
    }

    public void setReleaseReleaserEnabled(boolean flag) {
    }

    public boolean isReleaseReleaserEnabled() {
        return false;
    }

    public void setSuccessTaggerEnabled(boolean flag) {
    }

    public boolean isSuccessTaggerEnabled() {
        return false;
    }

    public void setFailedTaggerEnabled(boolean flag) {
    }

    public boolean isFailedTaggerEnabled() {
        return false;
    }

    public void setProdReleasedTaggerEnabled(boolean flag) {
    }

    public boolean isProdReleasedTaggerEnabled() {
        return false;
    }

    public void setProdUnreleasedTaggerEnabled(boolean flag) {
    }

    public boolean isProdUnreleasedTaggerEnabled() {
        return false;
    }

    public String getMoveIssueStrategy() {
        return null;
    }

    public void setMoveIssueStrategy(String s) {
    }

    public String getMoveIssueToVersion() {
        return null;
    }

    public void setMoveIssueToVersion(String s) {
    }

    public String getCodeChangeNullVersionStrategy() {
        return null;
    }

    public void setCodeChangeNullVersionStrategy(String s) {
    }

    public String getDependencyNullVersionStrategy() {
        return null;
    }

    public void setDependencyNullVersionStrategy(String s) {
    }

    public String getInitialNullVersionStrategy() {
        return null;
    }

    public void setInitialNullVersionStrategy(String s) {
    }

    public String getUnkownNullVersionStrategy() {
        return null;
    }

    public void setUnknownNullVersionStrategy(String s) {
    }

    public String getManualNullVersionStrategy() {
        return null;
    }

    public void setManualNullVersionStrategy(String s) {
    }

    public String getScheduledNullVersionStrategy() {
        return null;
    }

    public void setScheduledNullVersionStrategy(String s) {
    }

    public void setCodeChangeNullVersionReleased(boolean flag) {
    }

    public boolean isCodeChangeNullVersionReleased() {
        return false;
    }

    public String getCodeChangeNullVersionName() {
        return null;
    }

    public void setCodeChangeNullVersionName(String s) {
    }

    public void setDependencyNullVersionReleased(boolean flag) {
    }

    public boolean isDependencyNullVersionReleased() {
        return false;
    }

    public String getDependencyNullVersionName() {
        return null;
    }

    public void setDependencyNullVersionName(String s) {
    }

    public void setInitialNullVersionReleased(boolean flag) {
    }

    public boolean isInitialNullVersionReleased() {
        return false;
    }

    public String getInitialNullVersionName() {
        return null;
    }

    public void setInitialVersionName(String s) {
    }

    public void setUnknownNullVersionReleased(boolean flag) {
    }

    public boolean isUnknownNullVersionReleased() {
        return false;
    }

    public String getUnknownNullVersionName() {
        return null;
    }

    public void setUnknownNullVersionName(String s) {
    }

    public void setManualNullVersionReleased(boolean flag) {
    }

    public boolean isManualNullVersionReleased() {
        return false;
    }

    public String getManualNullVersionName() {
        return null;
    }

    public void setManualNullVersionName(String s) {
    }

    public void setScheduledNullVersionReleased(boolean flag) {
    }

    public boolean isScheduledNullVersionReleased() {
        return false;
    }

    public String getScheduledNullVersionName() {
        return null;
    }

    public void setScheduledNullVersionName(String s) {
    }

    public void setRawJiraVersionName(String s) {
    }

    public void setJiraProjectKey(String s) {
    }

    public String getJiraProjectKey() {
        return null;
    }

    public void setJSONVersionTypes(String s) {
    }

    public String getJSONVersionTypes() {
        return null;
    }

    public void setJiraVersionName(String s) {
    }

    public String getJiraVersionName() {
        return null;
    }

    public String getRawJiraVersionName() {
        return null;
    }

    public void setJiraVersionQualifier(String s) {
    }

    public String getJiraVersionQualifier() {
        return null;
    }

    public void setJiraVersionReleased(boolean flag) {
    }

    public boolean isJiraVersionReleased() {
        return false;
    }

    public void setProperty(String s, String s1) {
    }

    public String getProperty(String s) {
        return null;
    }

    public boolean containsKey(String s) {
        return false;
    }

    public void setUnreleasedAppender(String s) {
    }

    public String getUnreleasedAppender() {
        return null;
    }

    public void setReleasedAppender(String s) {
    }

    public String getReleasedAppender() {
        return null;
    }

    public void setSuccessTagDestination(String s) {
    }

    public String getSuccessTagDestination() {
        return null;
    }

    public String getSuccessTagComment() {
        return null;
    }

    public void setFailedTagDestination(String s) {
    }

    public String getFailedTagDestination() {
        return null;
    }

    public String getFailedTagComment() {
        return null;
    }

    public void setExcludesList(String s) {
    }

    public String getExcludesList() {
        return null;
    }

    public void setVersionMask(String s) {
    }

    public String getVersionMask() {
        return null;
    }

    public void setStrictVersions(boolean flag) {
    }

    public boolean isStrictVersions() {
        return false;
    }

    public void setStrictQualifiers(boolean flag) {
    }

    public boolean isStrictQualifiers() {
        return false;
    }

    public void setAllowProduction(boolean flag) {
    }

    public boolean isAllowProduction() {
        return false;
    }

    public void setAllowRelease(boolean flag) {
    }

    public boolean isAllowRelease() {
        return false;
    }

    public void setBrancherDestination(String s) {
    }

    public String getBrancherDestination() {
        return null;
    }

    public void setBrancherComment(String s) {
    }

    public String getBrancherComment() {
        return null;
    }

    public void setBrancherPlan(String s) {
    }

    public String getBrancherPlan() {
        return null;
    }

    public void setTaggedAs(String s) {
    }

    public String getTaggedAs() {
        return null;
    }

    public void setBranchedAs(String s) {
    }

    public String getBranchedAs() {
        return null;
    }

    public void setReleasedInJira(boolean flag) {
    }

    public boolean isReleasedInJira() {
        return false;
    }
}
