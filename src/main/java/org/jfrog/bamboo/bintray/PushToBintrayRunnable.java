package org.jfrog.bamboo.bintray;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.bintray.client.BintrayClient;
import org.jfrog.bamboo.bintray.client.MavenSyncResponse;
import org.jfrog.bamboo.util.ActionLog;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
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

    private static final String MINIMAL_SUPPORTED_VERSION = "3.6";
    private Logger log = Logger.getLogger(PushToBintrayRunnable.class);

    private BintrayClient bintrayClient;
    private ServerConfig serverConfig;
    private PushToBintrayAction action;
    private ActionLog bintrayLog;

    public PushToBintrayRunnable(PushToBintrayAction pushToBintrayAction, ServerConfig serverConfig, BintrayClient bintrayClient) {
        this.action = pushToBintrayAction;
        this.serverConfig = serverConfig;
        this.bintrayClient = bintrayClient;
        this.bintrayLog = PushToBintrayAction.context.getActionLog();
    }

    /**
     * Run method to perform Push to Bintray action
     * This method sets the isSuccessfullyDone in the action object to use later in the action view
     */
    @Override
    public void run() {
        ArtifactoryBuildInfoClient artifactoryClient = null;
        try {
            bintrayLog.logMessage("Starting Push to Bintray action.");
            PushToBintrayAction.context.getLock().lock();
            PushToBintrayAction.context.setDone(false);
            artifactoryClient = getArtifactoryBuildInfoClient(serverConfig);
            if (!isValidArtifactoryVersion(artifactoryClient)) {
                bintrayLog.logError("Push to Bintray supported from Artifactory version " + MINIMAL_SUPPORTED_VERSION);
                PushToBintrayAction.context.setDone(true);
                return;
            }
            boolean successfulPush = performPushToBintray(artifactoryClient);
            if (successfulPush && action.isMavenSync()) {
                bintrayLog.logMessage("Starting MavenSync.");
                mavenCentralSync();
            }
        } catch (Exception e) {
            bintrayLog.logError("Error while trying to Push build to Bintray.", e);
        } finally {
            if (artifactoryClient != null) {
                artifactoryClient.shutdown();
            }
            PushToBintrayAction.context.setDone(true);
            PushToBintrayAction.context.getLock().unlock();
        }
    }

    /**
     * Create the relevant objects from input and send it to build info artifactoryClient that will preform the actual push
     * Set the result of the action to true if successful to use in the action view.
     */
    private boolean performPushToBintray(ArtifactoryBuildInfoClient artifactoryClient) {

        String buildName = PushToBintrayAction.context.getBuildKey();
        String buildNumber = Integer.toString(PushToBintrayAction.context.getBuildNumber());

        String subject = action.getSubject(),
                repoName = action.getRepository(),
                packageName = action.getPackageName(),
                versionName = action.getVersion(),
                vcsUrl = action.getVcsUrl(),
                signMethod = action.getSignMethod(),
                passphrase = action.getGpgPassphrase();

        List<String> licenses = createLicensesListFromString(action.getLicenses());

        BintrayUploadInfoOverride uploadInfoOverride = new BintrayUploadInfoOverride(subject, repoName, packageName,
                versionName, licenses, vcsUrl);

        try {
            BintrayResponse response =
                    artifactoryClient.pushToBintray(buildName, buildNumber, signMethod, passphrase, uploadInfoOverride);
            bintrayLog.logMessage(response.toString());
            log.info("Push to Bintray finished: " + response.toString());
            return response.isSuccessful();
        } catch (Exception e) {
            bintrayLog.logError("Push to Bintray Failed with Exception.", e);
        }
        return false;
    }

    /**
     * Trigger's Bintray MavenCentralSync API
     */
    private void mavenCentralSync() {
        try {
            bintrayLog.logMessage("Syncing build with Sonatype OSS.");
            MavenSyncResponse mavenSyncResponse = bintrayClient.mavenCentralSync(action.getSubject(), action.getRepository(),
                    action.getPackageName(), action.getVersion());
            bintrayLog.logMessage("Bintray response status: " + mavenSyncResponse.getStatus() + ".");
            bintrayLog.logMessage(mavenSyncResponse.getMessages());
        } catch (Exception e) {
            bintrayLog.logError("Error while trying to sync with Maven Central", e);
        }
    }

    private boolean isValidArtifactoryVersion(ArtifactoryBuildInfoClient client) {
        boolean validVersion = false;
        try {
            ArtifactoryVersion version = client.verifyCompatibleArtifactoryVersion();
            validVersion = version.isAtLeast(new ArtifactoryVersion(MINIMAL_SUPPORTED_VERSION));
        } catch (Exception e) {
            bintrayLog.logError("Error while checking Artifactory version", e);
        }
        return validVersion;
    }

    private ArtifactoryBuildInfoClient getArtifactoryBuildInfoClient(ServerConfig serverConfig) {
        String username = serverConfig.getUsername();
        String password = serverConfig.getPassword();
        String artifactoryUrl = serverConfig.getUrl();
        return new ArtifactoryBuildInfoClient(artifactoryUrl, username, password, new BambooBuildInfoLog(log));
    }

    private List<String> createLicensesListFromString(String licenses) {
        String[] licensesArray = StringUtils.split(licenses, ",");
        for (int i = 0; i < licensesArray.length; i++) {
            licensesArray[i] = licensesArray[i].trim();
        }
        return Lists.newArrayList(licensesArray);
    }
}
