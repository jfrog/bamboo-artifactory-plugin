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

import com.atlassian.bamboo.bandana.PlanAwareBandanaContext;
import com.atlassian.bamboo.security.EncryptionException;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Global Artifactory server configuration manager
 *
 * @author Noam Y. Tenne
 */
public class ServerConfigManager implements Serializable {

    private transient Logger log = Logger.getLogger(ServerConfigManager.class);

    private static final String ARTIFACTORY_CONFIG_KEY = "org.jfrog.bamboo.server.configurations";
    private static final String BINTRAY_CONFIG_KEY = "org.jfrog.bamboo.bintray.configurations";
    private static final EncryptionService encryptionService = (EncryptionService) ContainerManager.getComponent("encryptionService");
    private final List<ServerConfig> configuredServers = new CopyOnWriteArrayList<>();
    private BintrayConfiguration bintrayConfig;
    private transient BandanaManager bandanaManager;
    private AtomicLong nextAvailableId = new AtomicLong(0);
    private CustomVariableContext customVariableContext;



    public List<ServerConfig> getAllServerConfigs() {
        return Lists.newArrayList(configuredServers);
    }

    public ServerConfig getServerConfigById(long id) {
        for (ServerConfig configuredServer : configuredServers) {
            if (configuredServer.getId() == id) {
                return configuredServer;
            }
        }

        return null;
    }

    public static ServerConfigManager getInstance() {
        ServerConfigManager serverConfigManager = new ServerConfigManager();
        ContainerManager.autowireComponent(serverConfigManager);
        return serverConfigManager;
    }

    public void addServerConfiguration(ServerConfig serverConfig) {
        serverConfig.setId(nextAvailableId.getAndIncrement());
        configuredServers.add(serverConfig);
        persist();
    }

    public void deleteServerConfiguration(final long id) {
        for (ServerConfig configuredServer : configuredServers) {
            if (configuredServer.getId() == id) {
                configuredServers.remove(configuredServer);
                persist();
                break;
            }
        }
    }

    public void updateServerConfiguration(ServerConfig updated) {
        for (ServerConfig configuredServer : configuredServers) {
            if (configuredServer.getId() == updated.getId()) {
                configuredServer.setUrl(updated.getUrl());
                configuredServer.setUsername(updated.getUsername());
                configuredServer.setPassword(updated.getPassword());
                configuredServer.setTimeout(updated.getTimeout());
                persist();
                break;
            }
        }
    }

    public void updateBintrayConfiguration(BintrayConfiguration bintrayConfig) {
        this.bintrayConfig = bintrayConfig;
        persistBintray();
    }

    public void setBandanaManager(BandanaManager bandanaManager) {
        this.bandanaManager = bandanaManager;
        try {
            setArtifactoryServers(bandanaManager);
        } catch (EncryptionException | IOException e) {
            log.error("Could not load Artifactory configuration. ");
        }
        try {
            setBintrayServers(bandanaManager);
        } catch (EncryptionException | IOException e) {
            log.error("Could not load Bintray configuration. ");
        }
    }

    private void setBintrayServers(BandanaManager bandanaManager) throws IOException, EncryptionException {
        Object existingBintrayConfig = bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, BINTRAY_CONFIG_KEY);
        if (existingBintrayConfig != null) {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(existingBintrayConfig);
            BintrayConfiguration tempBintrayConfig = new ObjectMapper().readValue(json, BintrayConfiguration.class);
            bintrayConfig = decryptExistingBintrayConfig(tempBintrayConfig);
        }
    }

    private void setArtifactoryServers(BandanaManager bandanaManager) throws IOException, EncryptionException {
        Object existingArtifactoryConfig = bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, ARTIFACTORY_CONFIG_KEY);
        if (existingArtifactoryConfig != null) {
            List<ServerConfig> serverConfigList = (List<ServerConfig>) existingArtifactoryConfig;
            for (Object serverConfig : serverConfigList) {
                // Because of some class loader issues we had to get a workaround,
                // we serialize and deserialize the serverConfig object.
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(serverConfig);
                ServerConfig tempServerConfig = new ObjectMapper().readValue(json, ServerConfig.class);

                if (nextAvailableId.get() <= tempServerConfig.getId()) {
                    nextAvailableId.set(tempServerConfig.getId() + 1);
                }

                configuredServers.add(new ServerConfig(tempServerConfig.getId(), tempServerConfig.getUrl(), tempServerConfig.getUsername(),
                        encryptionService.decrypt(tempServerConfig.getPassword()), tempServerConfig.getTimeout()));
            }
        }
    }

    public BandanaManager getBandanaManager() {
        return this.bandanaManager;
    }

    private BintrayConfiguration decryptExistingBintrayConfig(BintrayConfiguration bintrayConfig) throws EncryptionException {
        String bintrayApi = bintrayConfig.getBintrayApiKey();
        String sonatypeOssPassword = bintrayConfig.getSonatypeOssPassword();
        bintrayApi = TaskUtils.decryptIfNeeded(bintrayApi);
        sonatypeOssPassword = TaskUtils.decryptIfNeeded(sonatypeOssPassword);
        return new BintrayConfiguration(bintrayConfig.getBintrayUsername(), bintrayApi,
                bintrayConfig.getSonatypeOssUsername(), sonatypeOssPassword);
    }

    public void setBintrayConfig(BintrayConfiguration bintrayConfig) {
        this.bintrayConfig = bintrayConfig;
    }

    public BintrayConfiguration getBintrayConfig() {
        return bintrayConfig;
    }

    public List<String> getDeployableRepos(long serverId) {
        return getDeployableRepos(serverId, null, null);
    }

    public List<String> getDeployableRepos(long serverId, HttpServletRequest req, HttpServletResponse resp) {
        ServerConfig serverConfig = getServerConfigById(serverId);
        if (serverConfig == null) {
            log.error("Error while retrieving target repository list: Could not find Artifactory server " +
                    "configuration by the ID " + serverId);
            return Lists.newArrayList();
        }
        ArtifactoryBuildInfoClient client;

        String serverUrl = substituteVariables(serverConfig.getUrl());
        String username = null;
        String password = null;
        if (req != null) {
            username = req.getParameter("user");
            password = req.getParameter("password");
        }
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            password = TaskUtils.decryptIfNeeded(password);
        } else {
            username = serverConfig.getUsername();
            password = serverConfig.getPassword();
        }
        username = substituteVariables(username);
        password = substituteVariables(password);

        if (StringUtils.isBlank(username)) {
            client = new ArtifactoryBuildInfoClient(serverUrl, new BuildInfoLog(log));
        } else {
            client = new ArtifactoryBuildInfoClient(serverUrl, username, password,
                    new BuildInfoLog(log));
        }

        client.setConnectionTimeout(serverConfig.getTimeout());

        try {
            return client.getLocalRepositoriesKeys();

        } catch (IOException ioe) {
            log.error("Error while retrieving target repository list from: " + serverUrl, ioe);
            try {
                if (resp != null && ioe.getMessage().contains("401"))
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                if (resp != null && ioe.getMessage().contains("404"))
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                log.error("Error while sending error to response", e);
            }

            return Lists.newArrayList();
        }

    }

    /**
     * Substitute (replace) Bamboo variable names with their defined values
     */
    public String substituteVariables(String s) {
        return s != null ? customVariableContext.substituteString(s) : null;
    }

    public List<String> getResolvingRepos(long serverId, HttpServletRequest req, HttpServletResponse resp) {
        ServerConfig serverConfig = getServerConfigById(serverId);
        if (serverConfig == null) {
            log.error("Error while retrieving resolving repository list: Could not find Artifactory server " +
                    "configuration by the ID " + serverId);
            return Lists.newArrayList();
        }
        ArtifactoryBuildInfoClient client;

        String serverUrl = substituteVariables(serverConfig.getUrl());
        String username;
        String password;
        if (StringUtils.isNotBlank(req.getParameter("user")) && StringUtils.isNotBlank(req.getParameter("password"))) {
            username = substituteVariables(req.getParameter("user"));
            password = substituteVariables(TaskUtils.decryptIfNeeded(req.getParameter("password")));
        } else {
            username = substituteVariables(serverConfig.getUsername());
            password = substituteVariables(serverConfig.getPassword());
        }

        if (StringUtils.isBlank(username)) {
            client = new ArtifactoryBuildInfoClient(serverUrl, new BuildInfoLog(log));
        } else {
            client = new ArtifactoryBuildInfoClient(serverUrl, username, password,
                    new BuildInfoLog(log));
        }

        client.setConnectionTimeout(serverConfig.getTimeout());

        try {
            return client.getVirtualRepositoryKeys();
        } catch (IOException ioe) {
            log.error("Error while retrieving resolving repository list from: " + serverUrl, ioe);
            try {
                if (ioe.getMessage().contains("401"))
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                if (ioe.getMessage().contains("404"))
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                log.error("Error while sending error to response", e);
            }
            return Lists.newArrayList();
        }
    }

    private synchronized void persist() {
        List<ServerConfig> serverConfigs = Lists.newArrayList();

        for (ServerConfig serverConfig : configuredServers) {
            serverConfigs.add(new ServerConfig(serverConfig.getId(), serverConfig.getUrl(), serverConfig.getUsername(),
                    encryptionService.encrypt(serverConfig.getPassword()), serverConfig.getTimeout()));
        }
        bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, ARTIFACTORY_CONFIG_KEY, serverConfigs);
    }

    private synchronized void persistBintray() {
        if (bintrayConfig != null) {
            bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, BINTRAY_CONFIG_KEY, createEncryptedBintrayConfig(bintrayConfig));
        }
    }

    private BintrayConfiguration createEncryptedBintrayConfig(BintrayConfiguration bintrayConfig) {
        BintrayConfiguration encConfig = new BintrayConfiguration();
        String apiKey = encryptionService.encrypt(bintrayConfig.getBintrayApiKey());
        String sonatypeOssPassword = bintrayConfig.getSonatypeOssPassword();
        encConfig.setBintrayApiKey(apiKey);
        encConfig.setSonatypeOssPassword(encryptionService.encrypt(sonatypeOssPassword));
        encConfig.setBintrayUsername(bintrayConfig.getBintrayUsername());
        encConfig.setSonatypeOssUsername(bintrayConfig.getSonatypeOssUsername());
        return encConfig;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
