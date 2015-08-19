/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in complia * nce with the License.
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

import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfrog.bamboo.util.ConstantValues;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Exposes features of the Artifactory Server Configuration Manager service to serve plugin modules which can't reach
 * it.
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryConfigServlet extends HttpServlet {

    private Logger log = Logger.getLogger(ArtifactoryConfigServlet.class);

    private ServerConfigManager serverConfigManager;

    public ArtifactoryConfigServlet() {
        serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
    }

    /**
     * Requires to be provided with a server ID (param name is serverId).<br> If given with the parameter
     * "deployableRepos=true", it will return the list of deployable repositories for the server with the given ID.<br>
     * If no other parameter is provided, the server configuration of the given ID will be returned.<br> All successful
     * responses are returned in JSON format.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String serverIdValue = req.getParameter("serverId");
        if (StringUtils.isBlank(serverIdValue)) {
            resp.sendError(HttpStatus.SC_BAD_REQUEST, "Please provide a server ID.");
            log.error("Unable to retrieve server configuration information. No server ID was provided.");
            return;
        }

        long serverId;
        try {
            serverId = Long.parseLong(serverIdValue);
        } catch (NumberFormatException e) {
            resp.sendError(HttpStatus.SC_BAD_REQUEST, "Please provide a valid long-type server ID.");
            log.error("Unable to retrieve server configuration information. An invalid server ID was provided (" +
                    serverIdValue + ").");
            return;
        }

        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        if (serverConfig == null) {
            resp.sendError(HttpStatus.SC_NOT_FOUND, "Could not find an Artifactory server configuration with the ID " +
                    serverId + ".");
            log.error("Unable to retrieve server configuration. No configuration was found with the ID " + serverId +
                    ".");
            return;
        }

        String deployableReposValue = req.getParameter("deployableRepos");
        String resolvingReposValue = req.getParameter("resolvingRepos");
        if (StringUtils.isNotBlank(deployableReposValue) && Boolean.valueOf(deployableReposValue)) {
            List<String> deployableRepoList = serverConfigManager.getDeployableRepos(serverId, req, resp);
            returnJsonObject(resp, deployableRepoList);
        } else if (StringUtils.isNotBlank(resolvingReposValue) && Boolean.valueOf(resolvingReposValue)) {
            List<String> resolvingRepoList = serverConfigManager.getResolvingRepos(serverId, req, resp);
            returnJsonObject(resp, resolvingRepoList);
        } else {
            returnJsonObject(resp, serverConfig);
        }
    }

    /**
     * Sends the given object as JSON to the response
     *
     * @param resp     Response to send to
     * @param toReturn Object to send
     */
    private void returnJsonObject(HttpServletResponse resp, Object toReturn) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        jsonFactory.setCodec(mapper);

        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
            JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
            jsonGenerator.writeObject(toReturn);
            writer.flush();
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
