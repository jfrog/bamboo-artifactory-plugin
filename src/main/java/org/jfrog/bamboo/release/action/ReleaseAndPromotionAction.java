package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanHelper;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plugin.RemoteAgentSupported;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.repository.svn.SvnRepository;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.security.acegi.acls.BambooPermission;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.trigger.ManualBuildTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opensymphony.xwork.ActionContext;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.promotion.PromotionContext;
import org.jfrog.bamboo.promotion.PromotionThread;
import org.jfrog.bamboo.release.provider.ReleaseProvider;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.version.VersionHelper;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * An action to display when entering the "Artifactory Release & Promotion" tab from within a job.
 * Will display the versions for modules of a Maven build, or read the {@code
 * gradle.properties} file of a Gradle build in accordance to the property keys that were configured in the build.
 *
 * @author Tomer Cohen
 */
@RemoteAgentSupported
public class ReleaseAndPromotionAction extends ViewBuildResults {
    public static final String PROMOTION_PUSH_TO_NEXUS_MODE = "pushToNexusMode";
    public static final String NEXUS_PUSH_PLUGIN_NAME = "bintrayOsoPush";
    public static final String NEXT_INTEG_KEY = "version.nextIntegValue";
    public static final String RELEASE_VALUE_KEY = "version.releaseValue";
    public static final String CURRENT_VALUE_KEY = "version.currentValue";
    public static final String RELEASE_PROP_KEY = "version.releaseProp";
    public static final String MODULE_KEY = "version.key";
    private static final Logger log = Logger.getLogger(ReleaseAndPromotionAction.class);
    private static final String PROMOTION_NORMAL_MODE = "normalMode";
    private static final Map<String, String> MODULE_VERSION_TYPES =
            ImmutableMap.of(ReleaseProvider.CFG_ONE_VERSION, "One version for all modules.",
                    ReleaseProvider.CFG_VERSION_PER_MODULE, "Version per module",
                    ReleaseProvider.CFG_USE_EXISTING_VERSION, "Use existing module versions");
    public static PromotionContext promotionContext = new PromotionContext();
    ServerConfigManager serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
    private String promotionMode = PROMOTION_NORMAL_MODE;
    private boolean promoting = true;
    private String promotionRepo = "";
    private VariableDefinitionManager variableDefinitionManager;
    private String comment = "";
    private String target = "";
    private boolean useCopy;
    private boolean includeDependencies;
    private String artifactoryReleaseManagementUrl = "";
    private String moduleVersionConfiguration = ReleaseProvider.CFG_ONE_VERSION;
    private boolean createVcsTag = true;
    private String tagUrl;
    private String tagComment;
    private String nextDevelopmentComment = "[artifactory-release] Next development version";
    private String releasePublishingRepo;
    private String stagingComment = "";
    private boolean useReleaseBranch = true;
    private CapabilityContext capabilityContext;
    private String releaseBranch;
    private List<ModuleVersionHolder> versions;

    public ReleaseAndPromotionAction() {
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
//        StringBuilder builder = new StringBuilder(
//                summary.getCustomBuildData().get(ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM));
//        if (!builder.toString().endsWith("/")) {
//            builder.append("/");
//        }
//        builder.append("webapp/builds/").append(getImmutableBuild().getName()).append("/").append(getBuildNumber());
//        artifactoryReleaseManagementUrl = builder.toString();

        return INPUT;
    }

    /**
     * This method is called by reflection via freemarker, it gets a task definition from the job, and gets the module
     * versions.
     *
     * @return A list of Module version holders, that hold the module name / property key, the original value that is
     * there right now, and the new value that it is to be replaced with.
     */
    public List<ModuleVersionHolder> getVersions() throws RepositoryException, IOException {
        if (versions == null) {
            Job job = getPlanJob();
            List<TaskDefinition> taskDefinitions = job.getBuildDefinition().getTaskDefinitions();
            if (taskDefinitions.isEmpty()) {
                log.warn("No task definitions defined");
                return Lists.newArrayList();
            }
            for (TaskDefinition definition : taskDefinitions) {
                AbstractBuildContext context = AbstractBuildContext.createContextFromMap(definition.getConfiguration());
                if (context != null) {
                    VersionHelper versionHelper =
                            VersionHelper.getHelperAccordingToType(context, getCapabilityContext());
                    if (versionHelper != null) {
                        int latestBuildNumberWithBuildInfo = findLatestBuildNumberWithBuildInfo();
                        setVersions(versionHelper.filterPropertiesForRelease(getMutablePlan(), latestBuildNumberWithBuildInfo));
                    }
                }
            }
        }
        return versions;
    }

    public void setVersions(List<ModuleVersionHolder> versions) {
        this.versions = versions;
    }

    private int findLatestBuildNumberWithBuildInfo() {
        List<ResultsSummary> summaries = resultsSummaryManager.getResultSummariesForPlan(getMutablePlan(), 0, 100);
        Collections.sort(summaries, new Comparator<ResultsSummary>() {
            @Override
            public int compare(ResultsSummary o1, ResultsSummary o2) {
                if (o1.getBuildNumber() > o2.getBuildNumber()) {
                    return -1;
                } else if (o2.getBuildNumber() < o2.getBuildNumber()) {
                    return 1;
                }
                return 0;
            }
        });
        for (ResultsSummary summary : summaries) {
            if (summary.getBuildState().equals(BuildState.SUCCESS)) {
                boolean biActive = Boolean.parseBoolean(
                        summary.getCustomBuildData().get(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM));
                if (biActive) {
                    return summary.getBuildNumber();
                }
            }
        }
        return -1;
    }

    /**
     * This method is called by reflection via freemarker, it gets a {@link TaskDefinition} for the job, and determines
     * if it is a Gradle build.
     *
     * @return True if this build is a Gradle build.
     */
    public boolean isGradle() {
        try {
            Job job = getPlanJob();
            List<TaskDefinition> definitions = job.getBuildDefinition().getTaskDefinitions();
            return TaskDefinitionHelper.findGradleDefinition(definitions) != null;
        } catch (Exception e) {
            e.toString();
        }

        return false;
    }

    /**
     * This method is called by reflection via freemarker, it gets a {@link TaskDefinition} and determines if it is a
     * Maven build.
     *
     * @return True if this build is a Maven build.
     */
    public boolean isMaven() {
        Job job = getPlanJob();
        List<TaskDefinition> definitions = job.getBuildDefinition().getTaskDefinitions();
        return TaskDefinitionHelper.findMavenDefinition(definitions) != null;
    }

    /**
     * This method is called by reflection via freemarker, it gets a {@link TaskDefinition} and determines if it uses a
     * Git repository.
     *
     * @return True if this build is using GIT as its SCM.
     */
    public boolean isGit() {
        Repository repository = getRepository();
        if (repository == null) {
            return false;
        }
        String className = repository.getClass().getName();
        return "com.atlassian.bamboo.plugins.git.GitRepository".equals(className) ||
                "com.atlassian.bamboo.plugins.git.GitHubRepository".equals(className) ||
                "com.atlassian.bamboo.plugins.stash.StashRepository".equals(className);
    }

    private Repository getRepository() {
        return PlanHelper.getDefaultRepository(getMutablePlan());
    }

    public boolean isUseShallowClone() {
        if (!isGit()) {
            return false;
        }
        Repository repository = getRepository();
        if (repository == null) {
            return false;
        }
        if ("com.atlassian.bamboo.plugins.git.GitRepository".equals(repository.getClass().getName())) {
            HierarchicalConfiguration configuration = repository.toConfiguration();
            return configuration.getBoolean("repository.git.useShallowClones", false);
        }
        if ("com.atlassian.bamboo.plugins.git.GitHubRepository".equals(repository.getClass().getName())) {
            HierarchicalConfiguration configuration = repository.toConfiguration();
            return configuration.getBoolean("repository.github.useShallowClones", false);
        }
        return false;
    }

    public Map<String, String> getModuleVersionTypes() {
        return MODULE_VERSION_TYPES;
    }

    /**
     * Perform a release build, sets the {@link AbstractBuildContext#ACTIVATE_RELEASE_MANAGEMENT} flag to {@code true}
     * and executes a manual build.
     *
     * @return {@code success} if the manual execution finished successfully.
     */
    public String doReleaseBuild() throws RepositoryException, IOException {
        List<TaskDefinition> taskDefinitions = getMutablePlan().getBuildDefinition().getTaskDefinitions();
        if (taskDefinitions.isEmpty()) {
            log.warn("No task definitions defined, cannot execute release build");
            return ERROR;
        }
        User user = getUser();
        PlanKey planKey = getMutablePlan().getPlanKey();
        if (user == null || planKey == null) {
            return ERROR;
        }
        setBuildKey(planKey.getKey());
        Map<String, String> configuration = Maps.newHashMap();
        Map parameters = ActionContext.getContext().getParameters();
        configuration.put(AbstractBuildContext.ACTIVATE_RELEASE_MANAGEMENT, String.valueOf(true));
        configuration.put(AbstractBuildContext.ReleaseManagementContext.TAG_URL, getTagUrl());
        configuration.put(AbstractBuildContext.ReleaseManagementContext.NEXT_DEVELOPMENT_COMMENT,
                getNextDevelopmentComment());
        configuration.put(AbstractBuildContext.ReleaseManagementContext.STAGING_COMMENT,
                getStagingComment());
        configuration.put(AbstractBuildContext.ReleaseManagementContext.RELEASE_REPO_KEY, getReleasePublishingRepo());
        configuration.put(AbstractBuildContext.ReleaseManagementContext.TAG_COMMENT, getTagComment());
        configuration.put(AbstractBuildContext.ReleaseManagementContext.RELEASE_BRANCH, getReleaseBranch());
        String[] useReleaseBranchParam = (String[]) parameters.get("useReleaseBranch");
        String useReleaseBranch = useReleaseBranchParam != null ? useReleaseBranchParam[0] : "false";
        configuration.put(AbstractBuildContext.ReleaseManagementContext.USE_RELEASE_BRANCH, useReleaseBranch);
        String[] createVcsTagParam = (String[]) parameters.get("createVcsTag");
        String createVcsTag = createVcsTagParam != null ? createVcsTagParam[0] : "false";
        configuration.put(AbstractBuildContext.ReleaseManagementContext.CREATE_VCS_TAG, createVcsTag);
        configuration.put(ReleaseProvider.MODULE_VERSION_CONFIGURATION, getModuleVersionConfiguration());
        TaskDefinition definition = TaskDefinitionHelper.findMavenOrGradleDefinition(taskDefinitions);
        if (definition == null) {
            log.error("No Maven or Gradle task found in job");
            return ERROR;
        }
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(definition.getConfiguration());
        VersionHelper helper = VersionHelper.getHelperAccordingToType(context, getCapabilityContext());

        helper.addVersionFieldsToConfiguration(parameters, configuration, getModuleVersionConfiguration(),
                definition.getConfiguration());
            planExecutionManager
                .startManualExecution(getPlanJob().getParent(), user, configuration, Maps.<String, String>newHashMap());
        return SUCCESS;
    }

    public String getSelectedServerId() {
        List<TaskDefinition> taskDefinitions = getPlanJob().getBuildDefinition().getTaskDefinitions();
        if (taskDefinitions.isEmpty()) {
            return "";
        }
        TaskDefinition definition = TaskDefinitionHelper.findMavenOrGradleDefinition(taskDefinitions);
        if (definition == null) {
            return "";
        }
        Map<String, String> configuration = definition.getConfiguration();
        Map<String, String> filtered = Maps.filterKeys(configuration, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return StringUtils.endsWith(input, AbstractBuildContext.SERVER_ID_PARAM);
            }
        });
        return filtered.values().iterator().next();
    }

    /**
     * @return Gets the current job.
     */
    private Job getPlanJob() {
        return (Job) getMutablePlan();
    }

    public String getModuleVersionConfiguration() {
        return moduleVersionConfiguration;
    }

    public void setModuleVersionConfiguration(String moduleVersionConfiguration) {
        this.moduleVersionConfiguration = moduleVersionConfiguration;
    }

    public boolean isCreateVcsTag() {
        return createVcsTag;
    }

    public void setCreateVcsTag(boolean createVcsTag) {
        this.createVcsTag = createVcsTag;
    }

    public String getTagUrl() throws RepositoryException, IOException {
        if (tagUrl != null) {
            return tagUrl;
        }
        String url = getDefaultTagUrl();
        List<ModuleVersionHolder> moduleVersionHolders = getVersions();
        if (moduleVersionHolders.isEmpty()) {
            return url;
        }
        url += moduleVersionHolders.get(0).getReleaseValue();
        return url;
    }

    public void setTagUrl(String tagUrl) {
        this.tagUrl = tagUrl;
    }

    public String getTagComment() throws RepositoryException, IOException {
        if (tagComment == null) {
            List<ModuleVersionHolder> versions1 = getVersions();
            String releaseValue;
            if (!versions1.isEmpty()) {
                releaseValue = versions1.get(0).getReleaseValue();
            } else {
                releaseValue = "1.0.0";
            }
            return "[artifactory-release] Release version " + releaseValue;
        }
        return tagComment;
    }

    public void setTagComment(String tagComment) {
        this.tagComment = tagComment;
    }

    public String getNextDevelopmentComment() {
        return nextDevelopmentComment != null ? nextDevelopmentComment : "";
    }

    public void setNextDevelopmentComment(String nextDevelopmentComment) {
        this.nextDevelopmentComment = nextDevelopmentComment;
    }

    public List<String> getPublishingRepos() {
        String serverId = getSelectedServerId();
        if (StringUtils.isBlank(serverId)) {
            return Lists.newArrayList();
        }
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
        return component.getDeployableRepos(Long.parseLong(serverId));
    }

    public String getReleasePublishingRepo() {
        if (StringUtils.isBlank(releasePublishingRepo)) {
            List<TaskDefinition> definitions = getPlanJob().getBuildDefinition().getTaskDefinitions();
            TaskDefinition definition = TaskDefinitionHelper.findMavenOrGradleDefinition(definitions);
            if (definition == null) {
                return "";
            }
            Map<String, String> configuration = definition.getConfiguration();
            Map<String, String> filtered = Maps.filterKeys(configuration, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    return StringUtils.endsWith(input, AbstractBuildContext.PUBLISHING_REPO_PARAM) ||
                            StringUtils.endsWith(input, Maven3BuildContext.DEPLOYABLE_REPO_KEY);
                }
            });
            return filtered.values().iterator().next();
        }
        return releasePublishingRepo;
    }

    public void setReleasePublishingRepo(String releasePublishingRepo) {
        this.releasePublishingRepo = releasePublishingRepo;
    }

    private String getDefaultTagUrl() {
        Job job = getPlanJob();
        List<TaskDefinition> definitions = job.getBuildDefinition().getTaskDefinitions();
        TaskDefinition definition = TaskDefinitionHelper.findMavenOrGradleDefinition(definitions);
        if (definition == null) {
            return "";
        }
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(definition.getConfiguration());
        String tagUrl = StringUtils.trimToEmpty(context.releaseManagementContext.getVcsTagBase());
        StringBuilder sb = new StringBuilder(getBaseTagUrlAccordingToScm(tagUrl));
        return sb.toString();
    }

    private String getBaseTagUrlAccordingToScm(String baseTagUrl) {
        Repository repository = getRepository();
        if (repository == null) {
            return baseTagUrl;
        }
        if (repository instanceof SvnRepository && !baseTagUrl.endsWith("/")) {
            return baseTagUrl + "/";
        }
        return baseTagUrl;
    }

    public boolean isUseReleaseBranch() {
        return useReleaseBranch;
    }

    public void setUseReleaseBranch(boolean useReleaseBranch) {
        this.useReleaseBranch = useReleaseBranch;
    }

    public String getReleaseBranch() throws RepositoryException, IOException {
        if (releaseBranch == null) {
            List<TaskDefinition> taskDefinitions = getPlanJob().getBuildDefinition().getTaskDefinitions();
            if (taskDefinitions.isEmpty()) {
                return "";
            }
            TaskDefinition definition = TaskDefinitionHelper.findMavenOrGradleDefinition(taskDefinitions);
            if (definition == null) {
                return "";
            }
            Map<String, String> configuration = definition.getConfiguration();
            AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
            String url = context.releaseManagementContext.getGitReleaseBranch();
            List<ModuleVersionHolder> moduleVersionHolders = getVersions();
            if (moduleVersionHolders.isEmpty()) {
                return url;
            }
            return url + moduleVersionHolders.get(0).getReleaseValue();
        }
        return releaseBranch;
    }

    public void setReleaseBranch(String releaseBranch) {
        this.releaseBranch = releaseBranch;
    }

    public String getStagingComment() {
        return stagingComment;
    }

    public void setStagingComment(String stagingComment) {
        this.stagingComment = stagingComment;
    }

    public CapabilityContext getCapabilityContext() {
        return capabilityContext;
    }

    public void setCapabilityContext(CapabilityContext capabilityContext) {
        this.capabilityContext = capabilityContext;
    }

    public String getReleaseValue() throws RepositoryException, IOException {
        List<ModuleVersionHolder> versions = getVersions();
        if (versions == null || versions.isEmpty()) {
            return "";
        }
        return versions.get(0).getReleaseValue();
    }

    public String getNextIntegValue() throws RepositoryException, IOException {
        List<ModuleVersionHolder> versions = getVersions();
        if (versions == null || versions.isEmpty()) {
            return "";
        }
        return versions.get(0).getNextIntegValue();
    }


    /**
     * ******************************************************************************
     */

    public boolean isReleaseBuild() {
        Plan plan = getMutablePlan();
        TaskDefinition mavenOrGradleDefinition =
                TaskDefinitionHelper.findMavenOrGradleDefinition(plan.getBuildDefinition().getTaskDefinitions());
        if (mavenOrGradleDefinition == null) {
            return false;
        }
        ResultsSummary summary = getResultsSummary();
        return summary != null && shouldShow(summary.getCustomBuildData());
    }

    private boolean shouldShow(Map<String, String> customData) {
        return customData.containsKey(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM) &&
                Boolean.valueOf(customData.get(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) &&
                customData.containsKey(ConstantValues.BUILD_RESULT_RELEASE_ACTIVATED_PARAM) &&
                Boolean.valueOf(customData.get(ConstantValues.BUILD_RESULT_RELEASE_ACTIVATED_PARAM));
    }

    public boolean isPermittedToPromote() {
        return bambooPermissionManager.hasPlanPermission(BambooPermission.BUILD, PlanKeys.getPlanKey(getPlanKey()));
    }

    public String doPromote() throws IOException {
        String key = promotionContext.getBuildKey();
        if (StringUtils.isNotBlank(key) && StringUtils.isBlank(getBuildKey())) {
            setBuildKey(key);
        }
        Integer number = promotionContext.getBuildNumber();
        if (number != null && getBuildNumber() == null) {
            setBuildNumber(number);
        }
        if (getMutablePlan() == null) {
            return INPUT;
        }
        if (!isPermittedToPromote()) {
            log.error("You are not permitted to execute build promotion.");
            return ERROR;
        }
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
        TaskDefinition definition = TaskUtils.getMavenOrGradleTaskDefinition(getMutablePlan());
        if (definition == null) {
            return ERROR;
        }
        String serverId = getSelectedServerId(definition);
        if (StringUtils.isBlank(serverId)) {
            log.error("No selected Artifactory server Id");
            return ERROR;
        }
        ServerConfig serverConfig = component.getServerConfigById(Long.parseLong(serverId));
        if (serverConfig == null) {
            log.error("Error while retrieving target repository list: Could not find Artifactory server " +
                    "configuration by the ID " + serverId);
            return ERROR;
        }

        Map<String, String> taskConfiguration = definition.getConfiguration();
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(taskConfiguration);
        ArtifactoryBuildInfoClient client = TaskUtils.createClient(serverConfigManager, serverConfig, context, log);
        ResultsSummary summary = getResultsSummary();
        TriggerReason reason = summary.getTriggerReason();
        String username = "";
        if (reason instanceof ManualBuildTriggerReason) {
            username = ((ManualBuildTriggerReason) reason).getUserName();
        }
        new PromotionThread(this, client, username).start();
        promoting = false;
        return SUCCESS;
    }

    public List<String> getResult() {
        return promotionContext.getLog();
    }

    public boolean isDone() {
        return promotionContext.isDone();
    }

    public String getPromotionMode() {
        return promotionMode;
    }

    public void setPromotionMode(String promotionMode) {
        this.promotionMode = promotionMode;
    }

    public Map<String, String> getSupportedPromotionModes() {
        Map<String, String> promotionModes = Maps.newHashMap();
        promotionModes.put(PROMOTION_NORMAL_MODE, "Normal");
        TaskDefinition definition = TaskUtils.getMavenOrGradleTaskDefinition(getMutablePlan());
        if (MavenSyncUtils.isPushToNexusEnabled(serverConfigManager, definition, getSelectedServerId(definition))) {
            promotionModes.put(PROMOTION_PUSH_TO_NEXUS_MODE, "Promote to Bintray and Central");
        }
        return promotionModes;
    }

    public List<String> getPromotionTargets() {
        return Lists.newArrayList(Promotion.RELEASED, Promotion.ROLLED_BACK);
    }

    public List<String> getPromotionRepos() {
        TaskDefinition definition = TaskUtils.getMavenOrGradleTaskDefinition(getMutablePlan());
        if (definition == null) {
            return Lists.newArrayList();
        }
        String selectedServerId = getSelectedServerId(definition);
        if (StringUtils.isBlank(selectedServerId)) {
            log.warn("No Artifactory server Id found");
            return Lists.newArrayList();
        }
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
        return component.getDeployableRepos(Long.parseLong(selectedServerId));
    }

    public String getSelectedServerId(TaskDefinition definition) {
        if (definition == null) {
            return "";
        }
        Map<String, String> configuration = definition.getConfiguration();
        Map<String, String> filtered = Maps.filterKeys(configuration, new Predicate<String>() {
            public boolean apply(String input) {
                return StringUtils.endsWith(input, AbstractBuildContext.SERVER_ID_PARAM);
            }
        });
        return filtered.values().iterator().next();
    }

    public String getPromotionRepo() {
        return promotionRepo;
    }

    public void setPromotionRepo(String promotionRepo) {
        this.promotionRepo = promotionRepo;
    }

    public boolean isPromoting() {
        return promoting;
    }

    public void setPromoting(boolean promoting) {
        this.promoting = promoting;
    }

    public VariableDefinitionManager getVariableDefinitionManager() {
        return variableDefinitionManager;
    }

    public void setVariableDefinitionManager(VariableDefinitionManager variableDefinitionManager) {
        this.variableDefinitionManager = variableDefinitionManager;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isUseCopy() {
        return useCopy;
    }

    public void setUseCopy(boolean useCopy) {
        this.useCopy = useCopy;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public String getArtifactoryReleaseManagementUrl() {
        return artifactoryReleaseManagementUrl;
    }

    public void setArtifactoryReleaseManagementUrl(String artifactoryReleaseManagementUrl) {
        this.artifactoryReleaseManagementUrl = artifactoryReleaseManagementUrl;
    }

    public String doGetLog() {
        return SUCCESS;
    }
}
