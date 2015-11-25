package org.jfrog.bamboo.bintray.client;

import org.apache.commons.lang3.StringUtils;

/**
 * Mapping of Bintray REST call to MavenSync Java Bean
 * Should be sent as payload when making a MavenSync call
 *
 * @author Aviad Shikloshi
 */
public class MavenCentralSyncModel {

    private String username;
    private String password;
    private String close;

    public MavenCentralSyncModel(String username, String password, String close) {
        this.username = username;
        this.password = password;
        if (StringUtils.isNotBlank(close))
            this.close = close;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getClose() {
        return close;
    }
}
