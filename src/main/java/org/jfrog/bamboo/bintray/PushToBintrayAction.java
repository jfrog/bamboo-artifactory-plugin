package org.jfrog.bamboo.bintray;

import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.plugin.RemoteAgentSupported;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.BintrayConfig;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.bintray.client.BintrayClient;
import org.jfrog.bamboo.promotion.PromotionContext;
import org.jfrog.bamboo.util.TaskUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Displaying the Push to Bintray action inside a Job tab
 *
 * @author Aviad Shikloshi
 */
@RemoteAgentSupported
public class PushToBintrayAction extends ViewBuildResults {

    public static Logger log = Logger.getLogger(PushToBintrayAction.class);
    public static PromotionContext context = new PromotionContext();

    private static final String BINTRAY_CONFIG_PREFIX = "bintray.";
    private static Map<String, String> signMethodList = ImmutableMap.of(
            "false", "Don't Sign", "true", "Sign", "", "According to descriptor file");
    private static BintrayClient bintrayClient;

    private boolean pushing = true;
    private boolean overrideDescriptorFile;
    private String subject;
    private String repository;
    private String packageName;
    private String version;
    private String licenses;
    private String vcsUrl;
    private String gpgPassphrase;
    private String signMethod;
    private boolean mavenSync;

    @Override
    public String doExecute() throws Exception {
        context.clearLog();
        String result = super.doExecute();
        if (ERROR.equals(result)) {
            return ERROR;
        }
        try {
            context.setBuildNumber(this.getBuildNumber());
            context.setBuildKey(this.getImmutableBuild().getName());
            BintrayConfig bintrayConfig = TaskUtils.getBintrayConfig();
            bintrayClient = new BintrayClient(bintrayConfig);
            Map<String, String> buildTaskConfiguration = TaskUtils.findConfigurationForBuildTask(this);
            addDefaultValuesForInput(buildTaskConfiguration);
            return INPUT;
        } catch (Exception e) {
            log.error("Error occurred while loading Push to Bintray configuration page.", e);
            return ERROR;
        }
    }

    public String doPush() {
        String result;
        ServerConfig serverConfig = TaskUtils.getArtifactoryServerConfig(getImmutableBuild());
        try {
            new Thread(new PushToBintrayRunnable(this, serverConfig, bintrayClient)).start();
            pushing = false;
            result = SUCCESS;
        } catch (Exception e) {
            result = ERROR;
        }
        return result;
    }

    public String doGetPushToBintrayLog() {
        return SUCCESS;
    }

    // If package name already in the Bintray configuration page we shouldn't generate it again
    private boolean validPushToBintrayFields() {
        return StringUtils.isNotBlank(this.packageName);
    }

    public Map<String, String> getSignMethodList() {
        return signMethodList;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGpgPassphrase() {
        return gpgPassphrase;
    }

    public void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
    }

    public String getLicenses() {
        return licenses;
    }

    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    public String getVcsUrl() {
        return vcsUrl;
    }

    public void setVcsUrl(String vcsUrl) {
        this.vcsUrl = vcsUrl;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getSignMethod() {
        return signMethod;
    }

    public void setSignMethod(String signMethod) {
        this.signMethod = signMethod;
    }

    public String isOverrideDescriptorFile() {
        return String.valueOf(overrideDescriptorFile);
    }

    public void setOverrideDescriptorFile(boolean overrideDescriptorFile) {
        this.overrideDescriptorFile = overrideDescriptorFile;
    }

    public boolean isMavenSync() {
        return mavenSync;
    }

    public void setMavenSync(boolean mavenSync) {
        this.mavenSync = mavenSync;
    }

    public boolean isPushing() {
        return pushing;
    }

    public void setPushing(boolean pushing) {
        this.pushing = pushing;
    }

    public List<String> getResult() {
        return context.getLog();
    }

    public boolean isDone() {
        return context.isDone();
    }

    /**
     * Populate the Bintray configuration from the build task configuration to the "Push to Bintray" task
     *
     * @param buildTaskConfiguration Artifactory build task configuration
     */
    private void addDefaultValuesForInput(Map<String, String> buildTaskConfiguration) throws IllegalAccessException, NoSuchFieldException {
        for (String bintrayFieldKey : buildTaskConfiguration.keySet()) {
            String bintrayValue = buildTaskConfiguration.get(bintrayFieldKey);
            if (StringUtils.startsWith(bintrayFieldKey, BINTRAY_CONFIG_PREFIX) && StringUtils.isNotBlank(bintrayValue)) {
                String valueKey = bintrayFieldKey.split("\\.")[1];
                Field field = this.getClass().getDeclaredField(valueKey);
                field.set(this, buildTaskConfiguration.get(bintrayFieldKey));
            }
        }
        setOverrideDescriptorFile(Boolean.valueOf(buildTaskConfiguration.get("bintrayConfiguration")));
    }
}
