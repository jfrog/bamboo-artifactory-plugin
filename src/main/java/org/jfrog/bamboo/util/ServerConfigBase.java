package org.jfrog.bamboo.util;

/**
 * Created by Bar Belity on 03/12/2019.
 */
public class ServerConfigBase {
    protected String url;
    protected String username;
    protected String password;

    public ServerConfigBase() {
    }

    public ServerConfigBase(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
