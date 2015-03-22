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
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.spring.ComponentAccessor;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Global Artifactory server configuration manager
 *
 * @author Noam Y. Tenne
 */
public class ServerConfigManager implements Serializable {

    private EncryptionService encryptionService = ComponentAccessor.ENCRYPTION_SERVICE.get();
    private static final String CONFIG_KEY = "org.jfrog.bamboo.server.configurations";
    private final List<ServerConfig> configuredServers = new CopyOnWriteArrayList<ServerConfig>();
    private transient Logger log = Logger.getLogger(ServerConfigManager.class);
    private transient BandanaManager bandanaManager;
    private AtomicLong nextAvailableId = new AtomicLong(0);
    private CustomVariableContext customVariableContext;

    public static ServerConfigManager getInstance() {
        ServerConfigManager serverConfigManager = new ServerConfigManager();
        ContainerManager.autowireComponent(serverConfigManager);
        return serverConfigManager;
    }

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

    public void addServerConfiguration(ServerConfig serverConfig) {
        serverConfig.setId(nextAvailableId.getAndIncrement());
        configuredServers.add(serverConfig);
        persist();
    }

    public void deleteServerConfiguration(final long id) {
        Iterator<ServerConfig> configIterator = configuredServers.iterator();
        while (configIterator.hasNext()) {
            ServerConfig serverConfig = configIterator.next();
            if (serverConfig.getId() == id) {
                configuredServers.remove(serverConfig);
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

    public void setBandanaManager(BandanaManager bandanaManager) {
        this.bandanaManager = bandanaManager;

        Object existingConfigs = bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, CONFIG_KEY);
        if (existingConfigs != null) {
            List<ServerConfig> serverConfigList = (List<ServerConfig>) existingConfigs;

            for (ServerConfig serverConfig : serverConfigList) {
                if (nextAvailableId.get() <= serverConfig.getId()) {
                    nextAvailableId.set(serverConfig.getId() + 1);
                }

                configuredServers.add(new ServerConfig(serverConfig.getId(), serverConfig.getUrl(),
                        serverConfig.getUsername(), encryptionService.decrypt(serverConfig.getPassword()),
                        serverConfig.getTimeout()));
            }
        }
    }

    public ArtifactoryBuildInfoClient createClient(long serverId) {
        ServerConfig serverConfig = getServerConfigById(serverId);
        if (serverConfig == null) {
            log.error("Error while retrieving target repository list: Could not find Artifactory server " +
                    "configuration by the ID " + serverId);
            return null;
        }
        ArtifactoryBuildInfoClient client;

        String serverUrl = substituteVariables(serverConfig.getUrl());
        String username = substituteVariables(serverConfig.getUsername());
        if (StringUtils.isBlank(username)) {
            client = new ArtifactoryBuildInfoClient(serverUrl, new BambooBuildInfoLog(log));
        } else {
            String password = substituteVariables(serverConfig.getPassword());
            client = new ArtifactoryBuildInfoClient(serverUrl, username, password, new BambooBuildInfoLog(log));
        }
        client.setConnectionTimeout(serverConfig.getTimeout());
        return client;
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
        String username;
        String password;
        if (req != null && StringUtils.isNotBlank(req.getParameter("user")) && StringUtils.isNotBlank(req.getParameter("password"))) {
            username = substituteVariables(req.getParameter("user"));
            password = substituteVariables(req.getParameter("password"));
        } else {
            username = substituteVariables(serverConfig.getUsername());
            password = substituteVariables(serverConfig.getPassword());
        }

        if (StringUtils.isBlank(username)) {
            client = new ArtifactoryBuildInfoClient(serverUrl, new BambooBuildInfoLog(log));
        } else {
            client = new ArtifactoryBuildInfoClient(serverUrl, username, password,
                    new BambooBuildInfoLog(log));
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
            password = substituteVariables(req.getParameter("password"));
        } else {
            username = substituteVariables(serverConfig.getUsername());
            password = substituteVariables(serverConfig.getPassword());
        }

        if (StringUtils.isBlank(username)) {
            client = new ArtifactoryBuildInfoClient(serverUrl, new BambooBuildInfoLog(log));
        } else {
            client = new ArtifactoryBuildInfoClient(serverUrl, username, password,
                    new BambooBuildInfoLog(log));
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

        bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, CONFIG_KEY, serverConfigs);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
