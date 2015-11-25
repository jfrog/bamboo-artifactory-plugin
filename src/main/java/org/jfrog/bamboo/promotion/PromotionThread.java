package org.jfrog.bamboo.promotion;

import com.atlassian.bamboo.plan.PlanIdentifier;
import com.atlassian.bamboo.variable.VariableDefinition;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.bamboo.release.action.ReleaseAndPromotionAction;
import org.jfrog.bamboo.util.ActionLog;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.release.action.ReleaseAndPromotionAction.*;

/**
 * Executes the promotion process
 *
 * @author Noam Y. Tenne
 */
public class PromotionThread extends Thread {

    public static final String NEXUS_PUSH_PROPERTY_PREFIX = "bintrayOsoPush.";
    transient Logger log = Logger.getLogger(PromotionThread.class);

    private ReleaseAndPromotionAction action;
    private ArtifactoryBuildInfoClient client;
    private String bambooUsername;
    private ActionLog releaseLog;

    public PromotionThread(ReleaseAndPromotionAction action, ArtifactoryBuildInfoClient client,
                           String bambooUsername) {
        this.action = action;
        this.client = client;
        this.bambooUsername = bambooUsername;
        this.releaseLog = ReleaseAndPromotionAction.promotionContext.getActionLog();
        releaseLog.setLogger(log);

    }

    @Override
    public void run() {
        try {
            promotionContext.getLock().lock();
            promotionContext.setBuildKey(action.getBuildKey());
            promotionContext.setBuildNumber(action.getBuildNumber());
            promotionContext.setDone(false);
            promotionContext.getLog().clear();

            if (performPromotion() && PROMOTION_PUSH_TO_NEXUS_MODE.equals(action.getPromotionMode())) {
                executePushToNexusPlugin();
            }

        } catch (Exception e) {
            String message = "An error occurred: " + e.getMessage();
            releaseLog.logError(message, e);
        } finally {
            try {
                client.shutdown();
            } finally {
                promotionContext.setDone(true);
                promotionContext.getLock().unlock();
            }
        }
    }

    private boolean executePushToNexusPlugin() throws IOException {
        releaseLog.logError("Executing 'Promotion to Bintray and Central' plugin ...");
        VariableDefinitionManager varDefManager = action.getVariableDefinitionManager();
        PlanIdentifier planIdentifier = action.getPlanManager().getPlanIdentifierForPermissionCheckingByKey(action.getPlanKey());
        if (planIdentifier == null) {
            String message = "Plugin execution failed: Couldn't find nexusPush variables.<br/>";
            releaseLog.logError(message);
            return false;
        }

        Map<String, String> executeRequestParams = Maps.newHashMap();
        executeRequestParams.put(BuildInfoFields.BUILD_NAME, action.getImmutableBuild().getName());
        executeRequestParams.put(BuildInfoFields.BUILD_NUMBER, action.getBuildNumber().toString());
        List<VariableDefinition> planVariables = varDefManager.getPlanVariables(planIdentifier);
        for (VariableDefinition planVariable : planVariables) {
            String key = planVariable.getKey();
            if (StringUtils.isNotBlank(key) && key.startsWith(NEXUS_PUSH_PROPERTY_PREFIX)) {
                executeRequestParams.put(StringUtils.removeStart(key, NEXUS_PUSH_PROPERTY_PREFIX),
                        planVariable.getValue());
            }
        }

        HttpResponse nexusPushResponse = null;
        try {
            nexusPushResponse = client.executePromotionUserPlugin(NEXUS_PUSH_PLUGIN_NAME, action.getImmutableBuild().getName(),
                    action.getBuildNumber().toString(), null);
            StatusLine responseStatusLine = nexusPushResponse.getStatusLine();
            if (HttpStatus.SC_OK == responseStatusLine.getStatusCode()) {
                releaseLog.logMessage("Plugin successfully executed!");
                return true;
            } else {
                String responseContent = entityToString(nexusPushResponse);
                String message = "Plugin execution failed: " + responseStatusLine + "<br/>" + responseContent;
                releaseLog.logError(message);
                return false;
            }
        } finally {
            if (nexusPushResponse != null) {
                HttpEntity entity = nexusPushResponse.getEntity();
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
            }
        }
    }

    private boolean performPromotion() throws IOException {
        releaseLog.logMessage("Promoting build ...");
        // do a dry run first
        PromotionBuilder promotionBuilder = new PromotionBuilder().status(action.getTarget())
                .comment(action.getComment()).ciUser(bambooUsername).targetRepo(action.getPromotionRepo())
                .dependencies(action.isIncludeDependencies()).copy(action.isUseCopy())
                .dryRun(true);
        releaseLog.logMessage("Performing dry run promotion (no changes are made during dry run) ...");
        String buildName = action.getImmutableBuild().getName();
        String buildNumber = action.getBuildNumber().toString();
        HttpResponse dryResponse = null;
        HttpResponse wetResponse = null;
        try {
            dryResponse = client.stageBuild(buildName, buildNumber, promotionBuilder.build());
            if (checkSuccess(dryResponse, true)) {
                releaseLog.logMessage("Dry run finished successfully. Performing promotion ...");
                wetResponse = client.stageBuild(buildName, buildNumber, promotionBuilder.dryRun(false).build());
                if (checkSuccess(wetResponse, false)) {
                    releaseLog.logMessage("Promotion completed successfully!");

                    return true;
                }

                return false;
            }

            return false;
        } finally {
            if (dryResponse != null) {
                HttpEntity entity = dryResponse.getEntity();
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
            }
            if (wetResponse != null) {
                HttpEntity entity = wetResponse.getEntity();
                if (entity != null) {
                    EntityUtils.consume(entity);
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
    private boolean checkSuccess(HttpResponse response, boolean dryRun) throws IOException {
        StatusLine status = response.getStatusLine();
        String content = entityToString(response);
        if (status.getStatusCode() != 200) {
            if (dryRun) {
                String message = "Promotion failed during dry run (no change in Artifactory was done): " +
                        status + "<br/>" + content;
                releaseLog.logMessage(message);
            } else {
                String message = "Promotion failed. View Artifactory logs for more details: " + status +
                        "<br/>" + content;
                releaseLog.logError(message);
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
                releaseLog.logError(errorMessage);
                return false;
            }
        }
        return true;
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
}
