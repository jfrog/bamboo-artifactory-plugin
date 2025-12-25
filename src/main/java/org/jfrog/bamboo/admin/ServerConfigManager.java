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

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.UnsupportedEncodingException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Global Artifactory server configuration manager
 *
 * @author Noam Y. Tenne
 */
public interface ServerConfigManager {

    List<ServerConfig> getAllServerConfigs();

    ServerConfig getServerConfigById(long id);

    void addServerConfiguration(ServerConfig serverConfig);

    void deleteServerConfiguration(final long id);

    void updateServerConfiguration(ServerConfig updated);

    List<String> getDeployableRepos(long serverId);

    List<String> getDeployableRepos(long serverId, HttpServletRequest req, HttpServletResponse resp);

    String substituteVariables(String s);
    
    List<String> getResolvingRepos(long serverId, HttpServletRequest req, HttpServletResponse resp);

    void persist() throws IllegalAccessException, UnsupportedEncodingException, JsonProcessingException;
}
