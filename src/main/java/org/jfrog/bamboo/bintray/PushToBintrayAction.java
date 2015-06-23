package org.jfrog.bamboo.bintray;

import com.atlassian.bamboo.build.ViewBuildResults;
import com.atlassian.bamboo.plugin.RemoteAgentSupported;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.promotion.PromotionContext;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

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

    public static final String BINTRAY_CONFIG_PREFIX = "bintray.";
    public static PromotionContext context = new PromotionContext();
    private static Map<String, String> signMethodList = ImmutableMap.of(
            "false", "Don't Sign", "true", "Sign", "", "According to descriptor file");

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

    @Override
    public String doExecute() throws Exception {
        String result = super.doExecute();
        if (ERROR.equals(result)) {
            return ERROR;
        }

        Map<String, String> buildTaskConfiguration = TaskUtils.findConfigurationForBuildTask(this);
        addDefaultValuesForInput(buildTaskConfiguration);
        context.setBuildNumber(this.getBuildNumber());
        context.setBuildKey(this.getImmutableBuild().getName());
        context.getLog().clear();
        return INPUT;
    }


    public String doPush() {
        String result;
        ArtifactoryBuildInfoClient client = getArtifactoryBuildInfoClient();
        if (client != null) {
            try {
                new Thread(new PushToBintrayRunnable(this, client)).start();
                pushing = false;
                result = SUCCESS;
            } catch (Exception e) {
                result = ERROR;
            }
        } else {
            result = ERROR;
        }
        return result;
    }

    public String doGetPushToBintrayLog() {
        return SUCCESS;
    }

    private ServerConfig getServerConfig() {
        List<TaskDefinition> taskDefinitionList = getImmutableBuild().getBuildDefinition().getTaskDefinitions();
        TaskDefinition relevantTaskDef = taskDefinitionList.get(taskDefinitionList.size() - 1);
        String serverIdStr = TaskUtils.getSelectedServerId(relevantTaskDef);
        if (StringUtils.isNotEmpty(serverIdStr)) {
            long serverId = Long.parseLong(serverIdStr);
            return ((ServerConfigManager) ContainerManager.getComponent(
                    ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY)).getServerConfigById(serverId);
        }
        return null;
    }

    private ArtifactoryBuildInfoClient getArtifactoryBuildInfoClient() {
        ServerConfig serverConfig = getServerConfig();
        if (serverConfig != null) {
            String username = serverConfig.getUsername();
            String password = serverConfig.getPassword();
            String artifactoryUrl = serverConfig.getUrl();
            NullLog log = new NullLog();
            return new ArtifactoryBuildInfoClient(artifactoryUrl, username, password, log);
        }
        return null;
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

    public boolean isOverrideDescriptorFile() {
        return overrideDescriptorFile;
    }

    public void setOverrideDescriptorFile(boolean overrideDescriptorFile) {
        this.overrideDescriptorFile = overrideDescriptorFile;
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
        boolean shouldOverrideDescriptor = false;
        for (String bintrayFieldKey : buildTaskConfiguration.keySet()) {
            String bintrayValue = buildTaskConfiguration.get(bintrayFieldKey);
            if (StringUtils.startsWith(bintrayFieldKey, BINTRAY_CONFIG_PREFIX) && StringUtils.isNotBlank(bintrayValue)) {
                String valueKey = bintrayFieldKey.split("\\.")[1];
                Field field = this.getClass().getDeclaredField(valueKey);
                field.set(this, buildTaskConfiguration.get(bintrayFieldKey));
                shouldOverrideDescriptor = true;
            }
        }
        setOverrideDescriptorFile(shouldOverrideDescriptor);
    }
}
