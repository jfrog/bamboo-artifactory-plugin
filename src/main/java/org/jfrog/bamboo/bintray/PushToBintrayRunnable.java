package org.jfrog.bamboo.bintray;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.apache.velocity.util.StringUtils;
import org.jfrog.build.api.release.BintrayUploadInfoOverride;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.bintrayResponse.BintrayResponse;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.util.List;


/**
 * Push to Bintray Runnable to pass in to a Thread that will preform this task on Bamboo
 *
 * @author Aviad Shikloshi
 */
public class PushToBintrayRunnable implements Runnable {

    public static final String MINIMAL_SUPPORTED_VERSION = "3.6";

    private Logger log = Logger.getLogger(PushToBintrayRunnable.class);
    private PushToBintrayAction action;
    private ArtifactoryBuildInfoClient client;

    public PushToBintrayRunnable(PushToBintrayAction pushToBintrayAction, ArtifactoryBuildInfoClient client) {
        this.action = pushToBintrayAction;
        this.client = client;
    }

    /**
     * Run method to perform Push to Bintray action
     * This method sets the isSuccessfullyDone in the action object to use later in the action view
     */
    @Override
    public void run() {
        logMessage("Starting Push to Bintray action.");
        PushToBintrayAction.context.getLock().lock();
        PushToBintrayAction.context.setDone(false);
        if (!isValidArtifactoryVersion(client)) {
            logError("Push to Bintray supported from Artifactory version " + MINIMAL_SUPPORTED_VERSION);
            PushToBintrayAction.context.setDone(true);
            return;
        }
        performPushToBintray();
        PushToBintrayAction.context.setDone(true);
        PushToBintrayAction.context.getLock().unlock();
        client.shutdown();
    }

    /**
     * Create the relevant objects from input and send it to build info client that will preform the actual push
     * Set the result of the action to true if successful to use in the action view.
     */
    public void performPushToBintray() {

        String buildName = PushToBintrayAction.context.getBuildKey();
        String buildNumber = Integer.toString(PushToBintrayAction.context.getBuildNumber());

        String signMethod = action.getSignMethod();
        String passphrase = action.getGpgPassphrase();

        String subject = action.getSubject(), repoName = action.getRepository(), packageName = action.getPackageName(),
                versionName = action.getVersion(), vcsUrl = action.getVcsUrl();
        List<String> licenses = createLicensesListFromString(action.getLicenses());

        BintrayUploadInfoOverride uploadInfoOverride = new BintrayUploadInfoOverride(
                subject, repoName, packageName, versionName, licenses, vcsUrl
        );
        try {
            BintrayResponse response =
                    client.pushToBintray(buildName, buildNumber, signMethod, passphrase, uploadInfoOverride);
            logMessage(response.toString());
        } catch (Exception e) {
            logError("Push to Bintray Failed with Exception: ", e);
        }
    }

    private boolean isValidArtifactoryVersion(ArtifactoryBuildInfoClient client) {
        boolean validVersion = false;
        try {
            ArtifactoryVersion version = client.verifyCompatibleArtifactoryVersion();
            validVersion = version.isAtLeast(new ArtifactoryVersion(MINIMAL_SUPPORTED_VERSION));
        } catch (Exception e) {
            logError("Error while checking Artifactory version", e);
        }
        return validVersion;
    }

    private List<String> createLicensesListFromString(String licenses) {
        String[] licensesArray = StringUtils.split(licenses, ",");
        for (int i = 0; i < licensesArray.length; i++) {
            licensesArray[i] = licensesArray[i].trim();
        }
        return Lists.newArrayList(licensesArray);
    }

    private void logError(String message, Exception e) {
        if (e != null) {
            message += " " + e.getMessage() + " <br>";
        }
        logError(message);
    }

    private void logError(String message) {
        log.error(message);
        PushToBintrayAction.context.getLog().add(message);
    }

    private void logMessage(String message) {
        log.info(message);
        PushToBintrayAction.context.getLog().add(message);
    }

}
