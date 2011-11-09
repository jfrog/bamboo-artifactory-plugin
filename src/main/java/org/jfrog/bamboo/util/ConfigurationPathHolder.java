package org.jfrog.bamboo.util;

/**
 * @author Tomer Cohen
 */
public class ConfigurationPathHolder {

    private final String initScriptPath;
    private final String clientConfPath;

    public ConfigurationPathHolder(String initScriptPath, String clientConfPath) {
        this.initScriptPath = initScriptPath;
        this.clientConfPath = clientConfPath;
    }

    public String getInitScriptPath() {
        return initScriptPath;
    }

    public String getClientConfPath() {
        return clientConfPath;
    }
}
