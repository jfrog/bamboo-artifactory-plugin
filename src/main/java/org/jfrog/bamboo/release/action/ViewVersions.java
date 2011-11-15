package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanHelper;
import com.atlassian.bamboo.plugin.RemoteAgentSupported;
import com.atlassian.bamboo.plugins.git.GitHubRepository;
import com.atlassian.bamboo.plugins.git.GitRepository;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.repository.svn.SvnRepository;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.ww2.actions.BuildActionSupport;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.util.OgnlValueStack;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.release.provider.ReleaseProvider;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.TaskHelper;
import org.jfrog.bamboo.util.version.VersionHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for displaying the versions for modules for a Maven build, or read the {@code
 * gradle.properties} file of a Gradle build in accordance to the property keys that were configured in the build.
 *
 * @author Tomer Cohen
 */
@RemoteAgentSupported
public class ViewVersions extends BuildActionSupport {
    private static final Logger log = Logger.getLogger(ViewVersions.class);
    private static final Map<String, String> MODULE_VERSION_TYPES =
            ImmutableMap.of(ReleaseProvider.CFG_ONE_VERSION, "One version for all modules.",
                    ReleaseProvider.CFG_VERSION_PER_MODULE, "Version per module",
                    ReleaseProvider.CFG_USE_EXISTING_VERSION, "Use existing module versions");
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
    public static final String NEXT_INTEG_KEY = "version.nextIntegValue";
    public static final String RELEASE_VALUE_KEY = "version.releaseValue";
    public static final String CURRENT_VALUE_KEY = "version.currentValue";
    public static final String RELEASE_PROP_KEY = "version.releaseProp";
    public static final String MODULE_KEY = "version.key";

    public ViewVersions() {
    }

    @Override
    public String doExecute() throws Exception {
        return INPUT;
    }

    /**
     * This method is called by reflection via freemarker, it gets a task definition from the job, and gets the module
     * versions.
     *
     * @return A list of Module version holders, that hold the module name / property key, the original value that is
     *         there right now, and the new value that it is to be replaced with.
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
                        setVersions(
                                versionHelper.filterPropertiesForRelease(getPlan(), latestBuildNumberWithBuildInfo));
                    }
                }
            }
        }
        OgnlValueStack stack = ActionContext.getContext().getValueStack();
        stack.push(versions);
        return versions;
    }

    private int findLatestBuildNumberWithBuildInfo() {
        List<ResultsSummary> summaries = resultsSummaryManager.getResultSummariesForPlan(getPlan(), 0, 100);
        Collections.sort(summaries, new Comparator<ResultsSummary>() {
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
        Job job = getPlanJob();
        List<TaskDefinition> definitions = job.getBuildDefinition().getTaskDefinitions();
        return TaskHelper.findGradleBuild(definitions) != null;
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
        return TaskHelper.findMavenBuild(definitions) != null;
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
                "com.atlassian.bamboo.plugins.git.GitHubRepository".equals(className);
    }

    private Repository getRepository() {
        return PlanHelper.getDefaultRepository(getPlan()).getRepository();
    }

    public boolean isUseShallowClone() {
        if (!isGit()) {
            return false;
        }
        Repository repository = getRepository();
        if (repository == null) {
            return false;
        }
        if (repository instanceof GitRepository) {
            HierarchicalConfiguration configuration = repository.toConfiguration();
            return configuration.getBoolean("repository.git.useShallowClones", false);
        }
        if (repository instanceof GitHubRepository) {
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
        List<TaskDefinition> taskDefinitions = getPlan().getBuildDefinition().getTaskDefinitions();
        if (taskDefinitions.isEmpty()) {
            log.warn("No task definitions defined, cannot execute release build");
            return ERROR;
        }
        User user = getUser();
        if (user == null) {
            return ERROR;
        }
        setBuildKey(getPlan().getPlanKey().getKey());
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
        TaskDefinition definition = TaskHelper.findMavenOrGradleTask(taskDefinitions);
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
        TaskDefinition definition = TaskHelper.findMavenOrGradleTask(taskDefinitions);
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

    /**
     * @return Gets the current job.
     */
    private Job getPlanJob() {
        return (Job) getPlan();
    }


    public void setVersions(List<ModuleVersionHolder> versions) {
        this.versions = versions;
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
            return "[artifactory-release] Release version " + getVersions().get(0).getReleaseValue();
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

    @Override
    public void setPlan(Plan plan) {
        super.setPlan(plan);
    }

    public List<String> getPublishingRepos() {
        String serverId = getSelectedServerId();
        if (StringUtils.isBlank(serverId)) {
            return Lists.newArrayList();
        }
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
        return component.getDeployableRepos(Long.parseLong(serverId));
    }

    public String getReleasePublishingRepo() {
        if (StringUtils.isBlank(releasePublishingRepo)) {
            List<TaskDefinition> definitions = getPlanJob().getBuildDefinition().getTaskDefinitions();
            TaskDefinition definition = TaskHelper.findMavenOrGradleTask(definitions);
            if (definition == null) {
                return "";
            }
            Map<String, String> configuration = definition.getConfiguration();
            Map<String, String> filtered = Maps.filterKeys(configuration, new Predicate<String>() {
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
        TaskDefinition definition = TaskHelper.findMavenOrGradleTask(definitions);
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
            TaskDefinition definition = TaskHelper.findMavenOrGradleTask(taskDefinitions);
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
}
