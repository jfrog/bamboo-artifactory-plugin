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

    @Override
    public void setReleaseEnabled(boolean flag) {
        buildConfiguration.setProperty(RELEASE_ENABLED_KEY, flag);
    }

    @Override
    public boolean isReleaseEnabled() {
        return buildConfiguration.getBoolean(RELEASE_ENABLED_KEY);
    }

    @Override
    public void setReleaseTaggerEnabled(boolean flag) {
    }

    @Override
    public boolean isReleaseTaggerEnabled() {
        return false;
    }

    @Override
    public void setReleaseBrancherEnabled(boolean flag) {
    }

    @Override
    public boolean isReleaseBrancherEnabled() {
        return false;
    }

    @Override
    public void setReleaseReleaserEnabled(boolean flag) {
    }

    @Override
    public boolean isReleaseReleaserEnabled() {
        return false;
    }

    @Override
    public void setSuccessTaggerEnabled(boolean flag) {
    }

    @Override
    public boolean isSuccessTaggerEnabled() {
        return false;
    }

    @Override
    public void setFailedTaggerEnabled(boolean flag) {
    }

    @Override
    public boolean isFailedTaggerEnabled() {
        return false;
    }

    @Override
    public void setProdReleasedTaggerEnabled(boolean flag) {
    }

    @Override
    public boolean isProdReleasedTaggerEnabled() {
        return false;
    }

    @Override
    public void setProdUnreleasedTaggerEnabled(boolean flag) {
    }

    @Override
    public boolean isProdUnreleasedTaggerEnabled() {
        return false;
    }

    @Override
    public String getMoveIssueStrategy() {
        return null;
    }

    @Override
    public void setMoveIssueStrategy(String s) {
    }

    @Override
    public String getMoveIssueToVersion() {
        return null;
    }

    @Override
    public void setMoveIssueToVersion(String s) {
    }

    @Override
    public String getCodeChangeNullVersionStrategy() {
        return null;
    }

    @Override
    public void setCodeChangeNullVersionStrategy(String s) {
    }

    @Override
    public String getDependencyNullVersionStrategy() {
        return null;
    }

    @Override
    public void setDependencyNullVersionStrategy(String s) {
    }

    @Override
    public String getInitialNullVersionStrategy() {
        return null;
    }

    @Override
    public void setInitialNullVersionStrategy(String s) {
    }

    @Override
    public String getUnkownNullVersionStrategy() {
        return null;
    }

    @Override
    public void setUnknownNullVersionStrategy(String s) {
    }

    @Override
    public String getManualNullVersionStrategy() {
        return null;
    }

    @Override
    public void setManualNullVersionStrategy(String s) {
    }

    @Override
    public String getScheduledNullVersionStrategy() {
        return null;
    }

    @Override
    public void setScheduledNullVersionStrategy(String s) {
    }

    @Override
    public void setCodeChangeNullVersionReleased(boolean flag) {
    }

    @Override
    public boolean isCodeChangeNullVersionReleased() {
        return false;
    }

    @Override
    public String getCodeChangeNullVersionName() {
        return null;
    }

    @Override
    public void setCodeChangeNullVersionName(String s) {
    }

    @Override
    public void setDependencyNullVersionReleased(boolean flag) {
    }

    @Override
    public boolean isDependencyNullVersionReleased() {
        return false;
    }

    @Override
    public String getDependencyNullVersionName() {
        return null;
    }

    @Override
    public void setDependencyNullVersionName(String s) {
    }

    @Override
    public void setInitialNullVersionReleased(boolean flag) {
    }

    @Override
    public boolean isInitialNullVersionReleased() {
        return false;
    }

    @Override
    public String getInitialNullVersionName() {
        return null;
    }

    @Override
    public void setInitialVersionName(String s) {
    }

    @Override
    public void setUnknownNullVersionReleased(boolean flag) {
    }

    @Override
    public boolean isUnknownNullVersionReleased() {
        return false;
    }

    @Override
    public String getUnknownNullVersionName() {
        return null;
    }

    @Override
    public void setUnknownNullVersionName(String s) {
    }

    @Override
    public void setManualNullVersionReleased(boolean flag) {
    }

    @Override
    public boolean isManualNullVersionReleased() {
        return false;
    }

    @Override
    public String getManualNullVersionName() {
        return null;
    }

    @Override
    public void setManualNullVersionName(String s) {
    }

    @Override
    public void setScheduledNullVersionReleased(boolean flag) {
    }

    @Override
    public boolean isScheduledNullVersionReleased() {
        return false;
    }

    @Override
    public String getScheduledNullVersionName() {
        return null;
    }

    @Override
    public void setScheduledNullVersionName(String s) {
    }

    @Override
    public void setRawJiraVersionName(String s) {
    }

    @Override
    public void setJiraProjectKey(String s) {
    }

    @Override
    public String getJiraProjectKey() {
        return null;
    }

    @Override
    public void setJSONVersionTypes(String s) {
    }

    @Override
    public String getJSONVersionTypes() {
        return null;
    }

    @Override
    public void setJiraVersionName(String s) {
    }

    @Override
    public String getJiraVersionName() {
        return null;
    }

    @Override
    public String getRawJiraVersionName() {
        return null;
    }

    @Override
    public void setJiraVersionQualifier(String s) {
    }

    @Override
    public String getJiraVersionQualifier() {
        return null;
    }

    @Override
    public void setJiraVersionReleased(boolean flag) {
    }

    @Override
    public boolean isJiraVersionReleased() {
        return false;
    }

    @Override
    public void setProperty(String s, String s1) {
    }

    @Override
    public String getProperty(String s) {
        return null;
    }

    @Override
    public boolean containsKey(String s) {
        return false;
    }

    @Override
    public void setUnreleasedAppender(String s) {
    }

    @Override
    public String getUnreleasedAppender() {
        return null;
    }

    @Override
    public void setReleasedAppender(String s) {
    }

    @Override
    public String getReleasedAppender() {
        return null;
    }

    @Override
    public void setSuccessTagDestination(String s) {
    }

    @Override
    public String getSuccessTagDestination() {
        return null;
    }

    @Override
    public String getSuccessTagComment() {
        return null;
    }

    @Override
    public void setFailedTagDestination(String s) {
    }

    @Override
    public String getFailedTagDestination() {
        return null;
    }

    @Override
    public String getFailedTagComment() {
        return null;
    }

    @Override
    public void setExcludesList(String s) {
    }

    @Override
    public String getExcludesList() {
        return null;
    }

    @Override
    public void setVersionMask(String s) {
    }

    @Override
    public String getVersionMask() {
        return null;
    }

    @Override
    public void setStrictVersions(boolean flag) {
    }

    @Override
    public boolean isStrictVersions() {
        return false;
    }

    @Override
    public void setStrictQualifiers(boolean flag) {
    }

    @Override
    public boolean isStrictQualifiers() {
        return false;
    }

    @Override
    public void setAllowProduction(boolean flag) {
    }

    @Override
    public boolean isAllowProduction() {
        return false;
    }

    @Override
    public void setAllowRelease(boolean flag) {
    }

    @Override
    public boolean isAllowRelease() {
        return false;
    }

    @Override
    public void setBrancherDestination(String s) {
    }

    @Override
    public String getBrancherDestination() {
        return null;
    }

    @Override
    public void setBrancherComment(String s) {
    }

    @Override
    public String getBrancherComment() {
        return null;
    }

    @Override
    public void setBrancherPlan(String s) {
    }

    @Override
    public String getBrancherPlan() {
        return null;
    }

    @Override
    public void setTaggedAs(String s) {
    }

    @Override
    public String getTaggedAs() {
        return null;
    }

    @Override
    public void setBranchedAs(String s) {
    }

    @Override
    public String getBranchedAs() {
        return null;
    }

    @Override
    public void setReleasedInJira(boolean flag) {
    }

    @Override
    public boolean isReleasedInJira() {
        return false;
    }
}
