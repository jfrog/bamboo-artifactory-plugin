package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.configuration.GlobalAdminAction;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.util.TaskUtils;

import java.io.IOException;

/**
 * @author Aviad Shikloshi
 */
public class ConfigureBintrayAction extends GlobalAdminAction implements GlobalAdminSecurityAware {

    private static final Logger log = Logger.getLogger(ConfigureBintrayAction.class);


    private String bintrayUsername;
    private String bintrayApiKey;
    private String sonatypeOssUsername;
    private String sonatypeOssPassword;
    private String isSendTest;

    private ServerConfigManager serverConfigManager;

    private BintrayConfiguration bintrayConfig;



    public ConfigureBintrayAction(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
        if (serverConfigManager != null) {
            bintrayConfig = serverConfigManager.getBintrayConfig();
            setBintrayConfig(bintrayConfig);
        }
    }

    public String doDefault() throws Exception {
        getBintrayConfig();
        return INPUT;
    }


    public String execute() throws Exception
    {
        if (isTesting()) {
            bintrayTest();
            return INPUT;
        }
        BintrayConfiguration newBintrayConf = new BintrayConfiguration(
                        bintrayUsername, bintrayApiKey, sonatypeOssUsername, sonatypeOssPassword);
        serverConfigManager.updateBintrayConfiguration(newBintrayConf);
        setBintrayConfig(newBintrayConf);
        return SUCCESS;
    }

    public void bintrayTest() {
        String bintrayUrl = TaskUtils.getBintrayUrl();
        try {
            int bintrayStatus = TaskUtils.testBintrayConnection(bintrayUrl, bintrayUsername, bintrayApiKey);
            if (bintrayStatus == 200) {
                addActionMessage("Connection with Bintray established successfully!");
            } else {
                addActionError("Could not establish connection with Bintray. Server returned status: " + bintrayStatus);
            }
        } catch (IOException e) {
            addActionError("Error while checking connection to Bintray: " + e.getMessage());
        }
    }

    public String doBrowse() throws Exception {
        return super.execute();
    }

    @Override
    public void validate() {
        clearErrorsAndMessages();

        if (StringUtils.isNotEmpty(bintrayUsername)) {
            if (!StringUtils.isNotEmpty(bintrayApiKey)) {
                addFieldError("bintrayApiKey", "Please specify Bintray API key.");
            }
        } else {
            if (StringUtils.isNotEmpty(sonatypeOssUsername)) {
                addFieldError("bintrayUsername", "Bintray Username and API key are mandatory for syncing with Maven Central.");
            }
        }
    }

    public BintrayConfiguration getBintrayConfig() {
        if (this.bintrayConfig == null) {
            if (serverConfigManager != null) {
                bintrayConfig = serverConfigManager.getBintrayConfig();
                setBintrayConfig(bintrayConfig);
                return this.bintrayConfig;
            }
            else {
                addActionError("Server manager not loaded!" + new RuntimeException().getStackTrace());
            }
        }
        return new BintrayConfiguration();
    }

    private void setBintrayConfig(BintrayConfiguration bintrayConfig) {
        if (bintrayConfig != null) {
            this.bintrayUsername = bintrayConfig.getBintrayUsername();
            this.bintrayApiKey = bintrayConfig.getBintrayApiKey();
            this.sonatypeOssUsername = bintrayConfig.getSonatypeOssUsername();
            this.sonatypeOssPassword = bintrayConfig.getSonatypeOssPassword();
            this.bintrayConfig = bintrayConfig;
        }
    }


    public String getSendTest() {
        return isSendTest;
    }

    public void setSendTest(String sendTest) {
        isSendTest = sendTest;
    }

    private boolean isTesting() {
        return StringUtils.isNotBlank(isSendTest);
    }

    public String getBintrayUsername() {
        return bintrayUsername;
    }

    public void setBintrayUsername(String bintrayUsername) {
        this.bintrayUsername = StringUtils.trim(bintrayUsername);
    }

    public String getBintrayApiKey() {
        return bintrayApiKey;
    }

    public void setBintrayApiKey(String bintrayApiKey) {
        this.bintrayApiKey = bintrayApiKey;
    }

    public String getSonatypeOssUsername() {
        return sonatypeOssUsername;
    }

    public void setSonatypeOssUsername(String sonatypeOssUsername) {
        this.sonatypeOssUsername = sonatypeOssUsername;
    }

    public String getSonatypeOssPassword() {
        return sonatypeOssPassword;
    }

    public void setSonatypeOssPassword(String sonatypeOssPassword) {
        this.sonatypeOssPassword = sonatypeOssPassword;
    }
}
