package org.jfrog.bamboo.release.configuration;

public interface ArtifactoryReleaseConfiguration {

    public abstract void setReleaseEnabled(boolean flag);

    public abstract boolean isReleaseEnabled();

    public abstract void setReleaseTaggerEnabled(boolean flag);

    public abstract boolean isReleaseTaggerEnabled();

    public abstract void setReleaseBrancherEnabled(boolean flag);

    public abstract boolean isReleaseBrancherEnabled();

    public abstract void setReleaseReleaserEnabled(boolean flag);

    public abstract boolean isReleaseReleaserEnabled();

    public abstract void setSuccessTaggerEnabled(boolean flag);

    public abstract boolean isSuccessTaggerEnabled();

    public abstract void setFailedTaggerEnabled(boolean flag);

    public abstract boolean isFailedTaggerEnabled();

    public abstract void setProdReleasedTaggerEnabled(boolean flag);

    public abstract boolean isProdReleasedTaggerEnabled();

    public abstract void setProdUnreleasedTaggerEnabled(boolean flag);

    public abstract boolean isProdUnreleasedTaggerEnabled();

    public abstract String getMoveIssueStrategy();

    public abstract void setMoveIssueStrategy(String s);

    public abstract String getMoveIssueToVersion();

    public abstract void setMoveIssueToVersion(String s);

    public abstract String getCodeChangeNullVersionStrategy();

    public abstract void setCodeChangeNullVersionStrategy(String s);

    public abstract String getDependencyNullVersionStrategy();

    public abstract void setDependencyNullVersionStrategy(String s);

    public abstract String getInitialNullVersionStrategy();

    public abstract void setInitialNullVersionStrategy(String s);

    public abstract String getUnkownNullVersionStrategy();

    public abstract void setUnknownNullVersionStrategy(String s);

    public abstract String getManualNullVersionStrategy();

    public abstract void setManualNullVersionStrategy(String s);

    public abstract String getScheduledNullVersionStrategy();

    public abstract void setScheduledNullVersionStrategy(String s);

    public abstract void setCodeChangeNullVersionReleased(boolean flag);

    public abstract boolean isCodeChangeNullVersionReleased();

    public abstract String getCodeChangeNullVersionName();

    public abstract void setCodeChangeNullVersionName(String s);

    public abstract void setDependencyNullVersionReleased(boolean flag);

    public abstract boolean isDependencyNullVersionReleased();

    public abstract String getDependencyNullVersionName();

    public abstract void setDependencyNullVersionName(String s);

    public abstract void setInitialNullVersionReleased(boolean flag);

    public abstract boolean isInitialNullVersionReleased();

    public abstract String getInitialNullVersionName();

    public abstract void setInitialVersionName(String s);

    public abstract void setUnknownNullVersionReleased(boolean flag);

    public abstract boolean isUnknownNullVersionReleased();

    public abstract String getUnknownNullVersionName();

    public abstract void setUnknownNullVersionName(String s);

    public abstract void setManualNullVersionReleased(boolean flag);

    public abstract boolean isManualNullVersionReleased();

    public abstract String getManualNullVersionName();

    public abstract void setManualNullVersionName(String s);

    public abstract void setScheduledNullVersionReleased(boolean flag);

    public abstract boolean isScheduledNullVersionReleased();

    public abstract String getScheduledNullVersionName();

    public abstract void setScheduledNullVersionName(String s);

    public abstract void setRawJiraVersionName(String s);

    public abstract void setJiraProjectKey(String s);

    public abstract String getJiraProjectKey();

    public abstract void setJSONVersionTypes(String s);

    public abstract String getJSONVersionTypes();

    public abstract void setJiraVersionName(String s);

    public abstract String getJiraVersionName();

    public abstract String getRawJiraVersionName();

    public abstract void setJiraVersionQualifier(String s);

    public abstract String getJiraVersionQualifier();

    public abstract void setJiraVersionReleased(boolean flag);

    public abstract boolean isJiraVersionReleased();

    public abstract void setProperty(String s, String s1);

    public abstract String getProperty(String s);

    public abstract boolean containsKey(String s);

    public abstract void setUnreleasedAppender(String s);

    public abstract String getUnreleasedAppender();

    public abstract void setReleasedAppender(String s);

    public abstract String getReleasedAppender();

    public abstract void setSuccessTagDestination(String s);

    public abstract String getSuccessTagDestination();

    public abstract String getSuccessTagComment();

    public abstract void setFailedTagDestination(String s);

    public abstract String getFailedTagDestination();

    public abstract String getFailedTagComment();

    public abstract void setExcludesList(String s);

    public abstract String getExcludesList();

    public abstract void setVersionMask(String s);

    public abstract String getVersionMask();

    public abstract void setStrictVersions(boolean flag);

    public abstract boolean isStrictVersions();

    public abstract void setStrictQualifiers(boolean flag);

    public abstract boolean isStrictQualifiers();

    public abstract void setAllowProduction(boolean flag);

    public abstract boolean isAllowProduction();

    public abstract void setAllowRelease(boolean flag);

    public abstract boolean isAllowRelease();

    public abstract void setBrancherDestination(String s);

    public abstract String getBrancherDestination();

    public abstract void setBrancherComment(String s);

    public abstract String getBrancherComment();

    public abstract void setBrancherPlan(String s);

    public abstract String getBrancherPlan();

    public abstract void setTaggedAs(String s);

    public abstract String getTaggedAs();

    public abstract void setBranchedAs(String s);

    public abstract String getBranchedAs();

    public abstract void setReleasedInJira(boolean flag);

    public abstract boolean isReleasedInJira();

    public static final String CUSTOM_PREFIX = "custom.Release.";
    public static final String VERSION_PREFIX = "custom.Release.";
    public static final String DO_RELEASE_FLAG_KEY = "releaseDoReleaseBuild";
    public static final String FAIL_CHAIN = "failChainNOW";
    public static final String RELEASE_ENABLED_KEY = "custom.release.enabled";
    public static final String TAGGER_ENABLED_KEY = "custom.release.tagger.enabled";
    public static final String BRANCHER_ENABLED_KEY = "custom.release.brancher.enabled";
    public static final String RELEASER_ENABLED_KEY = "custom.release.releaser.enabled";
    public static final String RELEASER_MOVE_ISSUE_STRATEGY_KEY = "custom.release.release.moveissuestrategy";
    public static final String RELEASER_MOVE_ISSUE_TO_VERSION_KEY = "custom.release.release.moveissue.toversion";
    public static final String CODE_NULL_STRATEGY_KEY = "custom.release.code.nullversion.strategy";
    public static final String CODE_NULL_VERSION_NAME = "custom.release.code.nullversion.name";
    public static final String CODE_NULL_VERSION_RELEASED = "custom.release.code.nullversion.released";
    public static final String DEPEND_NULL_STRATEGY_KEY = "custom.release.depend.nullversion.strategy";
    public static final String DEPEND_NULL_VERSION_NAME = "custom.release.depend.nullversion.name";
    public static final String DEPEND_NULL_VERSION_RELEASED = "custom.release.depend.nullversion.released";
    public static final String INITIAL_NULL_STRATEGY_KEY = "custom.release.initial.nullversion.strategy";
    public static final String INITIAL_NULL_VERSION_NAME = "custom.release.initial.nullversion.name";
    public static final String INITIAL_NULL_VERSION_RELEASED = "custom.release.initial.nullversion.released";
    public static final String MANUAL_NULL_STRATEGY_KEY = "custom.release.manual.nullversion.strategy";
    public static final String MANUAL_NULL_VERSION_NAME = "custom.release.manual.nullversion.name";
    public static final String MANUAL_NULL_VERSION_RELEASED = "custom.release.manual.nullversion.released";
    public static final String SCHEDULED_NULL_STRATEGY_KEY = "custom.release.scheduled.nullversion.strategy";
    public static final String SCHEDULED_NULL_VERSION_NAME = "custom.release.scheduled.nullversion.name";
    public static final String SCHEDULED_NULL_VERSION_RELEASED = "custom.release.scheduled.nullversion.released";
    public static final String EDITED_NULL_STRATEGY_KEY = "custom.release.edited.nullversion.strategy";
    public static final String EDITED_NULL_VERSION_NAME = "custom.release.edited.nullversion.name";
    public static final String EDITED_NULL_VERSION_RELEASED = "custom.release.edited.nullversion.released";
    public static final String JIRA_PROJECTKEY_KEY = "custom.release.jira.projectkey";
    public static final String JSON_KEY = "custom.release.versiontypes.json";
    public static final String TAG_RELEASED_PROD_KEY = "custom.release.tagger.tag.released.prod";
    public static final String TAG_UNRELEASED_PROD_KEY = "custom.release.tagger.tag.unreleased.prod";
    public static final String TAG_SUCCESS_ENABLED_KEY = "custom.release.tagger.success.enabled";
    public static final String TAG_SUCCESS_DESTINATION_KEY = "custom.release.tagger.success.destination";
    public static final String TAG_SUCCESS_COMMENT_KEY = "custom.release.tagger.success.comment";
    public static final String TAG_FAILED_ENABLED_KEY = "custom.release.tagger.failed.enabled";
    public static final String TAG_FAILED_DESTINATION_KEY = "custom.release.tagger.failed.destination";
    public static final String TAG_FAILED_COMMENT_KEY = "custom.release.tagger.failed.comment";
    public static final String UNRELEASED_APPENDER = "custom.release.unreleased.appender";
    public static final String RELEASED_APPENDER = "custom.release.released.appender";
    public static final String EXCLUDES_LIST = "custom.release.exclude.list";
    public static final String VERSION_MASK = "custom.release.version.mask";
    public static final String STRICT_VERSIONS = "custom.release.strict.versions";
    public static final String STRICT_QUALIFIERS = "custom.release.strict.qualifiers";
    public static final String ALLOW_PRODUCTION = "custom.release.allow.production";
    public static final String ALLOW_RELEASE = "custom.release.allow.release";
    public static final String VERSION_NAME_KEY = "custom.release.name";
    public static final String VERSION_RAWNAME_KEY = "custom.release.rawname";
    public static final String VERSION_KEY = "custom.release.version";
    public static final String RAWVERSION_KEY = "custom.release.rawversion";
    public static final String VERSION_RELEASED_KEY = "custom.release.released";
    public static final String VERSION_TYPE_KEY = "custom.release.type";
    public static final String VERSION_QUALIFIER_KEY = "custom.release.qualifier";
    public static final String BRANCHER_PLAN = "custom.release.brancher.plan";
    public static final String BRANCHER_DESTINATION = "custom.release.brancher.destination";
    public static final String BRANCHER_COMMENT = "custom.release.brancher.comment";
    public static final String RELEASE_TAGGED_AS = "custom.release.tagged.as";
    public static final String RELEASE_BRANCHED_AS = "custom.release.branched.as";
    public static final String RELEASE_RELEASED_IN_JIRA = "custom.release.released.in.jira";
    public static final String SELECTED_REPOSITORY_KEY = "selectedRepository";
    public static final String SVN_REPO_ID = "com.atlassian.bamboo.plugin.system.repository:svn";
    public static final String RELEASE_MY_PLAN = "release-MY-PLAN";
}
