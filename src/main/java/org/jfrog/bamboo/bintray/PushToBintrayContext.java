package org.jfrog.bamboo.bintray;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Push to Bintray context to pass values from the build task configuration to the Push to Bintray configuration
 *
 * @author Aviad Shikloshi
 */
public class PushToBintrayContext {

    public static final String SUBJECT = "bintray.subject";
    public static final String REPO_NAME = "bintray.repository";
    public static final String PACKAGE_NAME = "bintray.packageName";
    public static final String SIGN = "bintray.signMethod";
    public static final String PASSPHRASE = "bintray.gpgPassphrase";
    public static final String LICENSES = "bintray.licenses";
    public static final String VCS_URL = "bintray.vcsUrl";
    public static final String MAVEN_CENTRAL_SYNC = "bintray.mavenSync";

    public static final Set<String> bintrayFields = ImmutableSet.of(SUBJECT, REPO_NAME, PACKAGE_NAME, SIGN, PASSPHRASE,
            LICENSES, VCS_URL, MAVEN_CENTRAL_SYNC);

    private boolean bintrayConfiguration;

    private String subject;
    private String repoName;
    private String packageName;
    private String signMethod;
    private String licenses;
    private String gpgPassphrase;
    private String vcsUrl;
    private boolean mavenCentralSync;

    public PushToBintrayContext() {
    }

    private String createLicensesList(String licenses) {
        return licenses;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getSignMethod() {
        return signMethod;
    }

    public void setSignMethod(String signMethod) {
        this.signMethod = signMethod;
    }

    public String getLicenses() {
        return licenses;
    }

    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    public String getGpgPassphrase() {
        return gpgPassphrase;
    }

    public void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
    }

    public String getVcsUrl() {
        return vcsUrl;
    }

    public void setVcsUrl(String vcsUrl) {
        this.vcsUrl = vcsUrl;
    }

    public boolean isMavenCentralSync() {
        return mavenCentralSync;
    }

    public void setMavenCentralSync(boolean mavenCentralSync) {
        this.mavenCentralSync = mavenCentralSync;
    }

    public boolean isBintrayConfiguration() {
        return bintrayConfiguration;
    }

    public void setBintrayConfiguration(boolean bintrayConfiguration) {
        this.bintrayConfiguration = bintrayConfiguration;
    }

}
