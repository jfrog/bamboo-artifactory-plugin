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

import com.atlassian.bamboo.bandana.BambooBandanaContext;
import com.atlassian.bamboo.bandana.PlanAwareBandanaContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.security.EncryptionHelper;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global Artifactory server configuration manager
 *
 * @author Noam Y. Tenne
 */
public class ServerConfigManager implements Serializable {

    private transient Logger log = Logger.getLogger(ServerConfigManager.class);

    private static final String ARTIFACTORY_CONFIG_KEY = "org.jfrog.bamboo.server.configurations.v2";
    private final List<ServerConfig> configuredServers = new CopyOnWriteArrayList<>();
    private BandanaManager bandanaManager = null;
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
        try {
            persist();
        } catch (IllegalAccessException | UnsupportedEncodingException e) {
            log.error("Could not add Artifactory configuration.", e);
        }
    }

    public void deleteServerConfiguration(final long id) {
        for (ServerConfig configuredServer : configuredServers) {
            if (configuredServer.getId() == id) {
                configuredServers.remove(configuredServer);
                try {
                    persist();
                } catch (IllegalAccessException | UnsupportedEncodingException e) {
                    log.error("Could not delete Artifactory configuration.", e);
                }
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
                try {
                    persist();
                } catch (IllegalAccessException | UnsupportedEncodingException e) {
                    log.error("Could not update Artifactory configuration.", e);
                }
                break;
            }
        }
    }

    public void setBandanaManager(BandanaManager bandanaManager) {
        this.bandanaManager = bandanaManager;
        try {
            setArtifactoryServers(bandanaManager);
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            log.error("Could not load Artifactory configuration.", e);
        }
    }

    public boolean isMissedMigration() {
        Iterator keysIterator = bandanaManager.getKeys(PlanAwareBandanaContext.GLOBAL_CONTEXT).iterator();
        boolean isMissedMigration = false;
        while (keysIterator.hasNext()) {
            String key = (String) keysIterator.next();
            // If the new key exists no migration needed.
            if (key.equals(ARTIFACTORY_CONFIG_KEY)) {
                return false;
            }
            // isMissedMigration will be true only if already found a key from the old plugin
            if (!isMissedMigration) {
                isMissedMigration = key.contains("org.jfrog.bamboo");
            }
        }
        return isMissedMigration;
    }

    private void setArtifactoryServers(BandanaManager bandanaManager)
            throws IOException, InstantiationException, IllegalAccessException {

        String existingArtifactoryConfig = (String) bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, ARTIFACTORY_CONFIG_KEY);
        if (StringUtils.isNotBlank(existingArtifactoryConfig)) {
            List<ServerConfig> serverConfigList = getServersFromXml(existingArtifactoryConfig);
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
                        EncryptionHelper.decrypt(tempServerConfig.getPassword()), tempServerConfig.getTimeout()));
            }
        }
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
        } finally {
            client.close();
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
        } finally {
            client.close();
        }
    }

    private synchronized void persist() throws IllegalAccessException, UnsupportedEncodingException {
        List<ServerConfig> serverConfigs = Lists.newArrayList();

        for (ServerConfig serverConfig : configuredServers) {
            serverConfigs.add(new ServerConfig(serverConfig.getId(), serverConfig.getUrl(), serverConfig.getUsername(),
                    EncryptionHelper.encrypt(serverConfig.getPassword()), serverConfig.getTimeout()));
        }
        String serverConfigsString = toXMLString(serverConfigs);
        bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, ARTIFACTORY_CONFIG_KEY, serverConfigsString);
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    public class BandanaContext extends PlanAwareBandanaContext{

        public BandanaContext(@Nullable BambooBandanaContext parentContext, long planId, long chanId, @Nullable String pluginKey) {
            super(parentContext, planId, chanId, pluginKey);
        }
    }

    private List<ServerConfig> getServersFromXml(String stringXml) throws IllegalAccessException, InstantiationException {
        List<ServerConfig> serverConfigs = Lists.newArrayList();
        List<String> stringServerConfigs = findAllObjects(ServerConfig.class, stringXml);
        for (String stringServerConfig : stringServerConfigs) {
            serverConfigs.add(getObjectFromStringXml(stringServerConfig, ServerConfig.class));
        }
        return serverConfigs;
    }

    private <T> T getObjectFromStringXml(String stringT, Class<T> tClass) throws IllegalAccessException, InstantiationException {
        T object = tClass.newInstance();
        boolean accsessable;
        String value;
        for (Field field : tClass.getDeclaredFields()) {
            accsessable = field.isAccessible();
            field.setAccessible(true);
            value = findFirstObject(field.getName(), stringT, true);
            if (field.getType().equals(long.class)) {
                field.set(object, Long.parseLong(value));
            } else if (field.getType().equals(int.class)) {
                field.set(object, Integer.parseInt(value));
            } else {
                field.set(object, findFirstObject(field.getName(), stringT, true));
            }
            field.setAccessible(accsessable);
        }
        return object;
    }

    private List<String> findAllObjects(Class providedClass, String scannedString) {
        List<String> foundStrings = Lists.newArrayList();
        String foundString = findFirstObject(providedClass.getSimpleName(), scannedString, false);
        while (!"".equals(foundString)) {
            foundStrings.add(foundString);
            scannedString = scannedString.replaceFirst(foundString, "");
            foundString = findFirstObject(providedClass.getSimpleName(), scannedString, false);
        }
        return foundStrings;
    }

    /**
     * Returns the found string or empty string if not found
     * @param objectToFind
     * @param stringToScan
     * @return
     */
    private String findFirstObject(String objectToFind, String stringToScan, boolean dataOnly) {
        String patternString = String.format("<%s>?(.*?)</%s>", objectToFind, objectToFind);
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(stringToScan);
        if (matcher.find()) {
            if (dataOnly) {
                return new String(Base64.getDecoder().decode(matcher.group(1).getBytes()));
            }
            return matcher.group(0);
        }
        return "";
    }

    private String toXMLString(List<ServerConfig> serverConfigs) throws IllegalAccessException, UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        openTag(stringBuilder, "List");
        for (ServerConfig serverConfig : serverConfigs) {
            stringBuilder.append(toXMLString(serverConfig));
        }
        closeTag(stringBuilder, "List");
        return stringBuilder.toString();
    }

    private String toXMLString(Object object) throws IllegalAccessException, UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        openTag(stringBuilder, object.getClass().getSimpleName());
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String value = field.get(object) == null ? "" : field.get(object).toString();
            appendAttribute(stringBuilder, field.getName(), value);
        }
        closeTag(stringBuilder, object.getClass().getSimpleName());
        return stringBuilder.toString();
    }

    private void appendAttribute(StringBuilder stringBuilder, String field, String value) throws UnsupportedEncodingException {
        openTag(stringBuilder, field);
        // Encoding the value to Base64 to prevent saving special chars like % to the database
        stringBuilder.append(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
        closeTag(stringBuilder, field);
    }

    private void openTag(StringBuilder stringBuilder, String fieldName) {
        stringBuilder.append("<");
        stringBuilder.append(fieldName);
        stringBuilder.append(">");
    }

    private void closeTag(StringBuilder stringBuilder, String fieldName) {
        stringBuilder.append("</");
        stringBuilder.append(fieldName);
        stringBuilder.append(">");
    }
}
