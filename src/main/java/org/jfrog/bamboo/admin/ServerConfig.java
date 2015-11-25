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

import java.io.Serializable;

/**
 * Global Artifactory server configuration bean
 *
 * @author Noam Y. Tenne
 */
public class ServerConfig implements Serializable {

    private long id;
    private String url;
    private String username;
    private String password;
    private int timeout = 300;


    public ServerConfig() {
    }

    public ServerConfig(long id, String url, String username, String password, int timeout) {
        this.id = id;
        this.url = url;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
    }

    public ServerConfig(ServerConfig serverConfig) {
        this.id = serverConfig.id;
        this.url = serverConfig.url;
        this.username = serverConfig.username;
        this.password = serverConfig.password;
        this.timeout = serverConfig.timeout;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
}
