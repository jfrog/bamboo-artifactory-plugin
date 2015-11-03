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

package org.jfrog.bamboo.util;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public interface ConstantValues extends Serializable {

    String ARTIFACTORY_PLUGIN_KEY = "org.jfrog.bamboo.bamboo-artifactory-plugin";
    String BUILD_RESULT_COLLECTION_ACTIVATED_PARAM = "org.jfrog.bamboo.buildInfo.activated";
    String BUILD_RESULT_RELEASE_ACTIVATED_PARAM = "org.jfrog.bamboo.release.activated";
    String BUILD_RESULT_SELECTED_SERVER_PARAM = "org.jfrog.bamboo.buildInfo.serverUrl";

    String BUILD_SERVLET_CONTEXT_NAME = "artifactoryBuildServlet";
    String BUILD_SERVLET_KEY_PARAM = "buildKey";

    String ADMIN_CONFIG_SERVLET_CONTEXT_NAME = "artifactoryAdminConfigServlet";
    String PLUGIN_CONFIG_MANAGER_KEY = "artifactoryServerConfigManager";
    String ARTIFACTORY_BAMBOO_UTILS_HELPER_KEY = "artifactoryBambooUtilsHelper";
    String PLAN_KEY_PARAM = "planKey";
    String BINTRAY_URL = "https://api.bintray.com/";
}
