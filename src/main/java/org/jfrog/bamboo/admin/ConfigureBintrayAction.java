package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import com.atlassian.spring.container.ContainerManager;
import org.jfrog.bamboo.util.ConstantValues;

/**
 * @author Aviad Shikloshi
 */
public class ConfigureBintrayAction extends BambooActionSupport implements GlobalAdminSecurityAware {

    private final ServerConfigManager serverConfigManager;

    private String bintrayUsername;
    private String bintrayApiKey;
    private String sonatypeOssUsername;
    private String sonatypeOssPassword;

    public ConfigureBintrayAction() {
        serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
    }

    public String doUpdateBintray() {
        serverConfigManager.updateBintrayConfiguration(new BintrayConfig(
                bintrayUsername, bintrayApiKey, sonatypeOssUsername, sonatypeOssPassword
        ));
        return SUCCESS;
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
}
