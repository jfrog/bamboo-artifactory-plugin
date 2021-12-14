/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.security.EncryptionHelper;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.util.VersionException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Global Artifactory server configuration form action
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryServerConfigAction extends BambooActionSupport implements GlobalAdminSecurityAware {

    private transient Logger log = LogManager.getLogger(ArtifactoryServerConfigAction.class);

    private String mode;
    private long serverId;
    private String url;
    private String username;
    private String password;
    private int timeout;
    private String isSendTest;


    private transient ServerConfigManager serverConfigManager;

    public ArtifactoryServerConfigAction(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
        mode = "add";
        timeout = 300;
        }

    @Override
    public void validate() {
        clearErrorsAndMessages();
        if (StringUtils.isBlank(url)) {
            addFieldError("url", "Please specify a URL of an Artifactory server.");
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException mue) {
                addFieldError("url", "Please specify a valid URL of an Artifactory server.");
            }
        }

        if (timeout <= 0) {
            addFieldError("timeout", "Please specify a positive integer.");
        }
    }

    public String doAdd() throws Exception {
        return INPUT;
    }

    public String doCreate() throws Exception {
        if (isTesting()) {
            testConnection();
            return INPUT;
        }

        serverConfigManager.addServerConfiguration(
                new ServerConfig(-1, getUrl(), getUsername(), getPassword(), getTimeout()));
        return SUCCESS;
    }

    public String doEdit() throws Exception {
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server configuration by the ID " + serverId);
        }
        updateFieldsFromServerConfig(serverConfig);
        return INPUT;
   }


    public String doUpdate() throws Exception {
        // Decrypt password from UI, if encrypted.
        password = EncryptionHelper.decryptIfNeeded(password);
        if (isTesting()) {
            testConnection();
            // Encrypt password when returning it to UI.
            password = EncryptionHelper.encryptForUi(password);
            return INPUT;
        }
        serverConfigManager.updateServerConfiguration(createServerConfig());
        return SUCCESS;
    }

    public String doDelete() throws Exception {
        serverConfigManager.deleteServerConfiguration(getServerId());
        return SUCCESS;
    }

    public String doBrowse() throws Exception {
        return super.execute();
    }

    public String browse() throws Exception {
        return super.execute();
    }

    public String confirm() throws Exception {
        return SUCCESS;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    private boolean isTesting() {
        return StringUtils.isNotBlank(isSendTest);
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getIsSendTest() {
        return isSendTest;
    }

    public void setSendTest(String sendTest) {
        isSendTest = sendTest;
    }

    private void testConnection() {
        ArtifactoryBuildInfoClient testClient;
        if (StringUtils.isNotBlank(username)) {
            testClient = TaskUtils.getArtifactoryBuildInfoClient(new ServerConfig(serverId, url, username,
                    password, timeout), new BuildInfoLog(log));
        } else {
            testClient = TaskUtils.getArtifactoryBuildInfoClient(new ServerConfig(serverId, url,
                    "", "", timeout), new BuildInfoLog(log));
        }
        try {
            testClient.verifyCompatibleArtifactoryVersion();
            addActionMessage("Connection successful!");
        } catch (VersionException ve) {
            handleConnectionException(ve);
        } catch (IllegalArgumentException iae) {
            handleConnectionException(iae);
        }
    }

    private void handleConnectionException(Exception e) {
        Throwable throwable = e.getCause();
        String errorMessage;
        if (throwable != null) {
            errorMessage = e.getMessage() + " (" + throwable.getClass().getCanonicalName() + ")";
        } else {
            errorMessage = e.getClass().getCanonicalName() + ": " + e.getMessage();
        }
        addActionError("Connection failed " + errorMessage);
        log.error("Error while testing the connection to Artifactory server " + url, e);
    }

    /**
     * Update fields to show in the server update page in the UI.
     * Encrypting password so it won't show in the UI inspection.
     *
     * @param serverConfig - Server being updated.
     */
    private void updateFieldsFromServerConfig(ServerConfig serverConfig) {
        setUrl(serverConfig.getUrl());
        setUsername(serverConfig.getUsername());
        setPassword(EncryptionHelper.encryptForUi(serverConfig.getPassword()));
        setTimeout(serverConfig.getTimeout());
    }

    @NotNull
    private ServerConfig createServerConfig() {
        return new ServerConfig(serverId, url, username, password, timeout);
    }
}
