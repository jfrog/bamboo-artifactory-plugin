package org.jfrog.bamboo.result;

import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.security.acegi.acls.BambooPermission;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.trigger.ManualBuildTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Action for release management builds, for promotion.
 *
 * @author Tomer Cohen
 */
public class ViewReleaseManagementAction extends ViewBuildResults {
    private static final String PROMOTION_NORMAL_MODE = "normalMode";
    static final String PROMOTION_PUSH_TO_NEXUS_MODE = "pushToNexusMode";
    static final String NEXUS_PUSH_PLUGIN_NAME = "nexusPush";
    static final String NEXUS_PUSH_PROPERTY_PREFIX = NEXUS_PUSH_PLUGIN_NAME + ".";
    transient Logger log = Logger.getLogger(ViewReleaseManagementAction.class);

    private String target = "";
    private String comment = "";
    private String artifactoryReleaseManagementUrl = "";
    private String promotionRepo = "";
    static PromotionAction promotionAction = new PromotionAction();
    private boolean promoting = true;
    private boolean includeDependencies;
    private boolean useCopy;
    private String promotionMode = PROMOTION_NORMAL_MODE;
    private VariableDefinitionManager variableDefinitionManager;

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
        builder.append("webapp/builds/").append(getBuild().getName()).append("/").append(getBuildNumber());
        artifactoryReleaseManagementUrl = builder.toString();

        return INPUT;
    }

    public String doGetLog() {
        return SUCCESS;
    }

    public boolean isReleaseBuild() {
        Plan plan = getPlan();
        TaskDefinition mavenOrGradleDefinition =
                TaskDefinitionHelper.findMavenOrGradleDefinition(plan.getBuildDefinition().getTaskDefinitions());
        if (mavenOrGradleDefinition == null) {
            return false;
        }
        ResultsSummary summary = getResultsSummary();
        return summary != null && shouldShow(summary.getCustomBuildData());
    }

    public boolean isPermittedToPromote() {
        return bambooPermissionManager
                .hasPlanPermission(BambooPermission.determineNameFromPermission(BambooPermission.BUILD), getPlanKey());
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
        if (isPushToNexusEnabled()) {
            promotionModes.put(PROMOTION_PUSH_TO_NEXUS_MODE, "Push to Nexus");
        }
        return promotionModes;
    }

    private boolean isPushToNexusEnabled() {
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
        TaskDefinition definition = getMavenOrGradleTaskDefinition();
        if (definition == null) {
            return false;
        }
        String serverId = getSelectedServerId(definition);
        if (StringUtils.isBlank(serverId)) {
            log.error("No special promotion modes enabled: no selected Artifactory server Id.");
            return false;
        }
        ServerConfig serverConfig = component.getServerConfigById(Long.parseLong(serverId));
        if (serverConfig == null) {
            log.error("No special promotion modes enabled: error while retrieving querying for enabled user plugins: " +
                    "could not find Artifactory server configuration by the ID " + serverId);
            return false;
        }
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(definition.getConfiguration());
        ArtifactoryBuildInfoClient client = createClient(serverConfig, context);
        try {
            Map<String, List<Map>> userPluginInfo = client.getUserPluginInfo();
            if (!userPluginInfo.containsKey("executions")) {
                log.debug("No special promotion modes enabled: no 'execute' user plugins could be found.");
            }
            List<Map> executionPlugins = userPluginInfo.get("executions");
            Iterables.find(executionPlugins, new Predicate<Map>() {
                @Override
                public boolean apply(Map pluginInfo) {
                    if ((pluginInfo != null) && pluginInfo.containsKey("name")) {
                        String pluginName = pluginInfo.get("name").toString();
                        return NEXUS_PUSH_PLUGIN_NAME.equals(pluginName);
                    }
                    return false;
                }
            });
            return true;
        } catch (IOException ioe) {
            log.error("No special promotion modes enabled: error while retrieving querying for enabled user plugins: " +
                    ioe.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("No special promotion modes enabled: error while retrieving querying for enabled user " +
                        "plugins.", ioe);
            }
        } catch (NoSuchElementException nsee) {
            log.debug("No special promotion modes enabled: no relevant execute user plugins could be found.");
        }
        return false;
    }

    private boolean shouldShow(Map<String, String> customData) {
        return customData.containsKey(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM) &&
                Boolean.valueOf(customData.get(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) &&
                customData.containsKey(ConstantValues.BUILD_RESULT_RELEASE_ACTIVATED_PARAM) &&
                Boolean.valueOf(customData.get(ConstantValues.BUILD_RESULT_RELEASE_ACTIVATED_PARAM));
    }

    public List<String> getPromotionTargets() {
        return Lists.newArrayList(Promotion.RELEASED, Promotion.ROLLED_BACK);
    }

    public List<String> getPromotionRepos() {
        TaskDefinition definition = getMavenOrGradleTaskDefinition();
        if (definition == null) {
            return Lists.newArrayList();
        }
        String selectedServerId = getSelectedServerId(definition);
        if (StringUtils.isBlank(selectedServerId)) {
            log.warn("No Artifactory server Id found");
            return Lists.newArrayList();
        }
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
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

    private TaskDefinition getMavenOrGradleTaskDefinition() {
        Plan plan = getPlan();
        if (plan == null) {
            return null;
        }
        List<TaskDefinition> definitions = plan.getBuildDefinition().getTaskDefinitions();
        if (definitions.isEmpty()) {
            return null;
        }
        TaskDefinition definition = TaskDefinitionHelper.findMavenOrGradleDefinition(definitions);
        return definition;
    }

    public String doPromote() throws IOException {
        String key = promotionAction.getBuildKey();
        if (StringUtils.isNotBlank(key) && StringUtils.isBlank(getBuildKey())) {
            setBuildKey(key);
        }
        Integer number = promotionAction.getBuildNumber();
        if (number != null && getBuildNumber() == null) {
            setBuildNumber(number);
        }
        if (getPlan() == null) {
            return INPUT;
        }
        if (!isPermittedToPromote()) {
            log.error("You are not permitted to execute build promotion.");
            return ERROR;
        }
        ServerConfigManager component = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
        TaskDefinition definition = getMavenOrGradleTaskDefinition();
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
        ArtifactoryBuildInfoClient client = createClient(serverConfig, context);
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

    private ArtifactoryBuildInfoClient createClient(ServerConfig serverConfig, AbstractBuildContext context) {
        String serverUrl = serverConfig.getUrl();
        String username = context.getDeployerUsername();
        if (StringUtils.isBlank(username)) {
            username = serverConfig.getUsername();
        }
        ArtifactoryBuildInfoClient client;
        BambooBuildInfoLog bambooLog = new BambooBuildInfoLog(log);
        if (StringUtils.isBlank(username)) {
            client = new ArtifactoryBuildInfoClient(serverUrl, bambooLog);
        } else {
            String password = context.getDeployerPassword();
            if (StringUtils.isBlank(password)) {
                password = serverConfig.getPassword();
            }
            client = new ArtifactoryBuildInfoClient(serverUrl, username, password, bambooLog);
        }
        client.setConnectionTimeout(serverConfig.getTimeout());
        return client;
    }

    public String getArtifactoryReleaseManagementUrl() {
        return artifactoryReleaseManagementUrl;
    }

    public void setArtifactoryReleaseManagementUrl(String artifactoryReleaseManagementUrl) {
        this.artifactoryReleaseManagementUrl = artifactoryReleaseManagementUrl;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public boolean isUseCopy() {
        return useCopy;
    }

    public void setUseCopy(boolean useCopy) {
        this.useCopy = useCopy;
    }

    public String getPromotionRepo() {
        return promotionRepo;
    }

    public void setPromotionRepo(String promotionRepo) {
        this.promotionRepo = promotionRepo;
    }

    public List<String> getResult() {
        return promotionAction.getLog();
    }

    public boolean isDone() {
        return promotionAction.isDone();
    }

    public boolean isPromoting() {
        return promoting;
    }

    public void setPromoting(boolean promoting) {
        this.promoting = promoting;
    }

    public void setVariableDefinitionManager(VariableDefinitionManager variableDefinitionManager) {
        this.variableDefinitionManager = variableDefinitionManager;
    }

    public VariableDefinitionManager getVariableDefinitionManager() {
        return variableDefinitionManager;
    }
}
