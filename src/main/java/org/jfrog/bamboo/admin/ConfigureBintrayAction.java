package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import com.atlassian.spring.container.ContainerManager;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.TaskUtils;

import java.io.IOException;

/**
 * @author Aviad Shikloshi
 */
public class ConfigureBintrayAction extends BambooActionSupport implements GlobalAdminSecurityAware {

    private final ServerConfigManager serverConfigManager;

    private String bintrayUsername;
    private String bintrayApiKey;
    private String sonatypeOssUsername;
    private String sonatypeOssPassword;
    private String bintrayTest;

    public ConfigureBintrayAction() {
        serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
        if (serverConfigManager != null) {
            BintrayConfig bintrayConfig = serverConfigManager.getBintrayConfig();
            if (bintrayConfig != null) {
                this.bintrayUsername = bintrayConfig.getBintrayUsername();
                this.bintrayApiKey = bintrayConfig.getBintrayApiKey();
                this.sonatypeOssUsername = bintrayConfig.getSonatypeOssUsername();
                this.sonatypeOssPassword = bintrayConfig.getSonatypeOssPassword();
            }
        }
    }

    public String doUpdateBintray() {
        if (isBintrayTesting()) {
            bintrayTest();
            return INPUT;
        }
        serverConfigManager.updateBintrayConfiguration(new BintrayConfig(
                bintrayUsername, bintrayApiKey, sonatypeOssUsername, sonatypeOssPassword
        ));
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

    public String getBintrayUsername() {
        return bintrayUsername;
    }

    public void setBintrayUsername(String bintrayUsername) {
        this.bintrayUsername = bintrayUsername;
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

    public void setBintrayTest(String bintrayTest) {
        this.bintrayTest = bintrayTest;
    }

    public boolean isBintrayTesting() {
        return "Test Bintray".equals(this.bintrayTest);
    }
}
