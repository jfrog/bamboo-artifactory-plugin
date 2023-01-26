package org.jfrog.bamboo.promotion;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfrog.bamboo.release.action.ReleasePromotionAction;
import org.jfrog.bamboo.util.ActionLog;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;

import static org.jfrog.bamboo.release.action.ReleasePromotionAction.promotionContext;

/**
 * Executes the promotion process
 *
 * @author Noam Y. Tenne
 */
public class PromotionThread extends Thread {

    transient Logger log = LogManager.getLogger(PromotionThread.class);

    private ReleasePromotionAction action;
    private ArtifactoryManager client;
    private String bambooUsername;
    private ActionLog releaseLog;
    private String buildName;
    private String buildNumber;

    public PromotionThread(ReleasePromotionAction action, ArtifactoryManager client,
                           String bambooUsername, String buildName, String buildNumber) {
        this.action = action;
        this.client = client;
        this.bambooUsername = bambooUsername;
        this.releaseLog = ReleasePromotionAction.promotionContext.getActionLog();
        releaseLog.setLogger(log);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
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

        // Backward compatibility for non-customizable build name and number.
        if (StringUtils.isBlank(buildName)) {
            buildName = action.getImmutableBuild().getName();
        }
        if (StringUtils.isBlank(buildNumber)) {
            buildNumber = action.getBuildNumber().toString();
        }

        try {
            client.stageBuild(buildName, buildNumber, "", promotionBuilder.build());
            releaseLog.logMessage("Dry run finished successfully. Performing promotion ...");
        } catch (IOException e) {
            releaseLog.logMessage(ExceptionUtils.getRootCauseMessage(e));
            return;
        }
        client.stageBuild(buildName, buildNumber,"", promotionBuilder.dryRun(false).build());
        releaseLog.logMessage("Promotion completed successfully!");
    }
}
