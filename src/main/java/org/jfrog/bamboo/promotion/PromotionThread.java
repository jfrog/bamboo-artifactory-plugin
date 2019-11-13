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
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.bamboo.release.action.ReleasePromotionAction;
import org.jfrog.bamboo.util.ActionLog;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.release.action.ReleasePromotionAction.*;

/**
 * Executes the promotion process
 *
 * @author Noam Y. Tenne
 */
public class PromotionThread extends Thread {

    transient Logger log = Logger.getLogger(PromotionThread.class);

    private ReleasePromotionAction action;
    private ArtifactoryBuildInfoClient client;
    private String bambooUsername;
    private ActionLog releaseLog;

    public PromotionThread(ReleasePromotionAction action, ArtifactoryBuildInfoClient client,
                           String bambooUsername) {
        this.action = action;
        this.client = client;
        this.bambooUsername = bambooUsername;
        this.releaseLog = ReleasePromotionAction.promotionContext.getActionLog();
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
            performPromotion();

        } catch (Exception e) {
            String message = "An error occurred: " + e.getMessage();
            releaseLog.logError(message, e);
        } finally {
            try {
                client.close();
            } finally {
                promotionContext.setDone(true);
                promotionContext.getLock().unlock();
            }
        }
    }

    private void performPromotion() throws IOException {
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
                    return;
                }
                return;
            }
            return;
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
        SerializationConfig serializationConfig = mapper.getSerializationConfig()
                .withAnnotationIntrospector(new JacksonAnnotationIntrospector())
                .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.setSerializationConfig(serializationConfig);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    private String entityToString(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();
        return IOUtils.toString(is, "UTF-8");
    }
}
