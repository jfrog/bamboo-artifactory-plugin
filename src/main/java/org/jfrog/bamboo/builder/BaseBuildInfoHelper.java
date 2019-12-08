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

package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.trigger.ManualBuildTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.Utils;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_SERVLET_CONTEXT_NAME;
import static org.jfrog.bamboo.util.ConstantValues.BUILD_SERVLET_KEY_PARAM;

/**
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildInfoHelper {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = Logger.getLogger(BaseBuildInfoHelper.class);

    protected BuildContext context;
    protected ServerConfigManager serverConfigManager;
    protected AdministrationConfiguration administrationConfiguration;
    protected AdministrationConfigurationAccessor administrationConfigurationAccessor;
    private HttpClient httpClient;
    protected String bambooBaseUrl;
    protected BuildParamsOverrideManager buildParamsOverrideManager;

    public void init(BuildParamsOverrideManager buildParamsOverrideManager, BuildContext context) {
        this.context = context;
        serverConfigManager = ServerConfigManager.getInstance();
        ContainerManager.autowireComponent(this);
        httpClient = new HttpClient();
        bambooBaseUrl = determineBambooBaseUrl();
        this.buildParamsOverrideManager = buildParamsOverrideManager;
    }

    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }

    public void setAdministrationConfigurationAccessor(
            AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }

    /**
     * Returns the full name of the build with the given key
     *
     * @param triggeringBuildKey Key of build to retrieve
     * @return Build full name
     */
    protected String getBuildName(String triggeringBuildKey) {
        try {
            Map<String, String> params = Maps.newHashMap();
            params.put(BUILD_SERVLET_KEY_PARAM, triggeringBuildKey);
            return getStringResource(BUILD_SERVLET_CONTEXT_NAME, params);
        } catch (IOException ioe) {
            log.error("Unable to determine triggering build name.", ioe);
            return null;
        }
    }

    protected String getPublishingRepoKey(AbstractBuildContext buildContext, Map<String, String> environment) {
        // In case this is a Release Staging build, return the publishing repo configured
        // in the Release Staging configuration page:
        String releaseManagementPublishRepo = environment.get("bamboo_release_management_repoKey");
        if (StringUtils.isNotBlank(releaseManagementPublishRepo)) {
            return releaseManagementPublishRepo;
        }
        // Take the publishing repo defined as a Bamboo variable or, if not defined, take the value
        // configured in the task configuration page:
        return overrideParam(
            buildContext.getPublishingRepo(),
            BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOY_REPO);
    }

    /**
     * Returns the String value of the remote resource
     *
     * @param servletName Name of servlet to query
     * @param params      Parameters to pass in the request
     * @return String value if found, Null if not
     */
    private String getStringResource(String servletName, Map<String, String> params) throws IOException {
        String requestUrl = prepareRequestUrl(servletName, params);

        GetMethod getMethod = new GetMethod(requestUrl);
        try {
            executeMethod(requestUrl, getMethod);
            return getMethod.getResponseBodyAsString();
        } finally {
            getMethod.releaseConnection();
        }
    }

    /**
     * Get parameters from buildInfoConfig.propertiesFile
     * @param propFilePath Path to buildInfoConfig.propertiesFile
     * @return Map of the parameters
     */
    public Map<String, String> getBuildInfoConfigPropertiesFileParams(String propFilePath) {
        Map<String, String> variablesToReturn = Maps.newHashMap();
        if (StringUtils.isNotBlank(propFilePath)) {
            File propFile = new File(propFilePath);
            if (propFile.isFile()) {
                Properties fileProperties = new Properties();

                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(propFile);
                    fileProperties.load(inputStream);
                } catch (IOException ioe) {
                    log.error("Error occurred while trying to resolve build info properties from: " + propFilePath,
                            ioe);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }

                variablesToReturn.putAll(Utils.filterPropertiesKeysByPrefix(fileProperties, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX));
                variablesToReturn.putAll(Utils.filterPropertiesKeysByPrefix(fileProperties, BuildInfoProperties.BUILD_INFO_PROP_PREFIX));
            }
        }
        return TaskUtils.getEscapedEnvMap(variablesToReturn);
    }

    private String prepareRequestUrl(String servletName, Map<String, String> params) {
        StringBuilder builder = new StringBuilder(bambooBaseUrl);
        if (!bambooBaseUrl.endsWith("/")) {
            builder.append("/");
        }
        StringBuilder requestUrlBuilder = builder.append("plugins/servlet/").append(servletName);
        if (params.size() != 0) {
            requestUrlBuilder.append("?");

            for (Map.Entry<String, String> param : params.entrySet()) {
                if (!requestUrlBuilder.toString().endsWith("?")) {
                    requestUrlBuilder.append("&");
                }
                requestUrlBuilder.append(param.getKey()).append("=").append(EscapeChars.forFormSubmission(param.getValue()));
            }
        }

        return requestUrlBuilder.toString();
    }

    /**
     * Executes the given HTTP method
     *
     * @param requestUrl Full request URL
     * @param getMethod  HTTP GET method
     */
    private void executeMethod(String requestUrl, GetMethod getMethod) throws IOException {
        int responseCode = httpClient.executeMethod(getMethod);
        if (responseCode == HttpStatus.SC_NOT_FOUND) {
            throw new IOException("Unable to find requested resource: " + requestUrl);
        } else if (responseCode != HttpStatus.SC_OK) {
            throw new IOException("Failed to retrieve requested resource: " + requestUrl + ". Response code: " +
                    responseCode + ", Message: " + getMethod.getStatusText());
        }
    }

    /**
     * Determines the base URL of this Bamboo instance.<br> This method is needed since we query the plugin's servlets
     * for build information that isn't accessible to a remote agent.<br> The URL can generally be found in {@link
     * com.atlassian.bamboo.configuration.AdministrationConfiguration}
     *
     * @return Bamboo base URL if found. Null if running in an un-recognized type of agent.
     */
    protected String determineBambooBaseUrl() {
        if (administrationConfiguration != null) {
            return administrationConfiguration.getBaseUrl();
        } else if (administrationConfigurationAccessor != null) {
            return administrationConfigurationAccessor.getAdministrationConfiguration().getBaseUrl();
        }
        return null;
    }

    /**
     * Checks if a Bamboo variable was defined to override the task configured value.
     * If so then returns the value defined for the variable, else return the original value
     *
     * @param originalValue Value from the task configuration.
     * @param overrideKey   Bamboo variable name.
     * @return              The Bamboo variable if defined. If not, the configured value.
     */
    public String overrideParam(String originalValue, String overrideKey) {
        String overriddenValue = buildParamsOverrideManager.getOverrideValue(overrideKey);
        return overriddenValue.isEmpty() ? originalValue : overriddenValue;
    }

    protected String getTriggeringUserNameRecursively(BuildContext context) {
        String principal = null;
        TriggerReason triggerReason = context.getTriggerReason();
        if (triggerReason instanceof ManualBuildTriggerReason) {
            principal = ((ManualBuildTriggerReason) triggerReason).getUserName();

            if (StringUtils.isBlank(principal)) {
                BuildContext parentContext = context.getParentBuildContext();
                if (parentContext != null) {
                    principal = getTriggeringUserNameRecursively(parentContext);
                }
            }
        }
        return principal;
    }

    protected ServerConfig getConfiguredServer(TaskContext context, long selectedServerId) {
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(selectedServerId);
        if (serverConfig == null) {
            String warning =
                    "Found an ID of a selected Artifactory server configuration (" + selectedServerId +
                            ") but could not find a matching configuration. Build info collection is disabled.";
            context.getBuildLogger().addErrorLogEntry(warning);
            log.warn(warning);
            return null;
        }
        return serverConfig;
    }
}
