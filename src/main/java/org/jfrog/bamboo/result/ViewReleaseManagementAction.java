package org.jfrog.bamboo.result;

import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.security.BambooPermissionManager;
import com.atlassian.bamboo.security.acegi.acls.BambooPermission;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.trigger.ManualBuildTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Action for release management builds, for promotion.
 *
 * @author Tomer Cohen
 */
public class ViewReleaseManagementAction extends ViewBuildResults {
    private transient Logger log = Logger.getLogger(ViewReleaseManagementAction.class);

    private String target = "";
    private String comment = "";
    private String artifactoryReleaseManagementUrl = "";
    private String promotionRepo = "";
    private static PromotionAction promotionAction = new PromotionAction();
    private boolean promoting = true;
    private boolean includeDependencies;
    private boolean useCopy;

    private BambooPermissionManager bambooPermissionManager;

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
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(definition.getConfiguration());
        ArtifactoryBuildInfoClient client = createClient(serverConfig, context);
        ResultsSummary summary = getResultsSummary();
        TriggerReason reason = summary.getTriggerReason();
        String username = "";
        if (reason instanceof ManualBuildTriggerReason) {
            username = ((ManualBuildTriggerReason) reason).getUserName();
        }
        new PromoteWorkerThread(client, username).start();
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

    public void setBambooPermissionManager(BambooPermissionManager bambooPermissionManager) {
        this.bambooPermissionManager = bambooPermissionManager;
    }

    /**
     * The thread that performs the promotion asynchronously.
     */
    public final class PromoteWorkerThread extends Thread {
        private final ArtifactoryBuildInfoClient client;
        private final String username;

        public PromoteWorkerThread(ArtifactoryBuildInfoClient client, String username) {
            this.client = client;
            this.username = username;
        }

        @Override
        public void run() {
            try {
                promotionAction.getLock().lock();
                promotionAction.setBuildKey(getBuildKey());
                promotionAction.setBuildNumber(getBuildNumber());
                promotionAction.setDone(false);
                promotionAction.getLog().clear();
                String message = "Promoting build ....";
                log.info(message);
                promotionAction.getLog().add(message + "<br/>");
                // do a dry run first
                PromotionBuilder promotionBuilder = new PromotionBuilder().status(getTarget()).comment(getComment())
                        .ciUser(username).targetRepo(getPromotionRepo()).dependencies(includeDependencies).copy(useCopy)
                        .dryRun(true);
                message = "Performing dry run promotion (no changes are made during dry run) ...";
                promotionAction.getLog().add(message + "<br/>");
                log.info(message);
                HttpResponse dryResponse =
                        client.stageBuild(getBuild().getName(), getBuildNumber().toString(), promotionBuilder.build());
                if (checkSuccess(dryResponse, true)) {
                    message = "Dry run finished successfully.Performing promotion ...";
                    promotionAction.getLog().add(message + "<br/>");
                    log.info(message);
                    HttpResponse wetResponse = client.stageBuild(getBuild().getName(), getBuildNumber().toString(),
                            promotionBuilder.dryRun(false).build());
                    if (checkSuccess(wetResponse, false)) {
                        message = "Promotion completed successfully!";
                        promotionAction.getLog().add(message);
                        log.info(message);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    client.shutdown();
                } finally {
                    promotionAction.setDone(true);
                    promotionAction.getLock().unlock();
                }
            }
        }
    }

    /**
     * Checks the status and return true on success
     *
     * @param response
     * @param dryRun
     * @return
     */
    private boolean checkSuccess(HttpResponse response, boolean dryRun) {
        StatusLine status = response.getStatusLine();
        try {
            String content = entityToString(response);
            if (status.getStatusCode() != 200) {
                if (dryRun) {
                    String message = "Promotion failed during dry run (no change in Artifactory was done): " + status +
                            "<br/>" + content;
                    promotionAction.getLog().add(message);
                    log.error(message);
                } else {
                    String message =
                            "Promotion failed. View Artifactory logs for more details: " + status + "<br/>" + content;
                    promotionAction.getLog().add(message);
                    log.error(message);
                }
                return false;
            }

            JsonFactory factory = createJsonFactory();
            JsonParser parser = factory.createJsonParser(content);
            JsonNode root = parser.readValueAsTree();
            JsonNode messagesNode = root.get("messages");
            for (JsonNode node : messagesNode) {
                String level = node.get("level").getTextValue();
                String message = node.get("message").getTextValue();
                if (("WARNING".equals(level) || "ERROR".equals(level)) && !message.startsWith("No items were")) {
                    String errorMessage = "Received " + level + ": " + message;
                    promotionAction.getLog().add(errorMessage);
                    log.error(errorMessage);
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    private String entityToString(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();
        return IOUtils.toString(is, "UTF-8");
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
}
