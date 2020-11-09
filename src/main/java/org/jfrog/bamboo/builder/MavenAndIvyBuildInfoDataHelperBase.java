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

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.util.BuildUtils;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.trigger.DependencyTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.ProxyUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.version.VcsHelper;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.*;

/**
 * @author Noam Y. Tenne
 */
public abstract class MavenAndIvyBuildInfoDataHelperBase extends BaseBuildInfoHelper {
    protected ArtifactoryClientConfiguration clientConf;
    protected ServerConfig selectedServerConfig;
    protected String deployerUsername;
    protected String deployerPassword;
    protected String deployerUrl;

    public MavenAndIvyBuildInfoDataHelperBase(BuildParamsOverrideManager buildParamsOverrideManager, TaskContext context,
                                              PackageManagersContext abstractBuildContext,
                                              EnvironmentVariableAccessor envVarAccessor, String artifactoryPluginVersion,
                                              boolean aggregateBuildInfo) {
        BuildContext buildContext = context.getBuildContext();
        super.init(buildParamsOverrideManager, context.getBuildContext(), context.getBuildLogger());
        long selectedServerId = abstractBuildContext.getArtifactoryServerId();
        clientConf = new ArtifactoryClientConfiguration(new NullLog());
        if (selectedServerId != -1 && isServerConfigured(selectedServerId)) {
            setDeployerProperties(context, abstractBuildContext);
            setBuilderData(context, abstractBuildContext, selectedServerConfig, clientConf, envVarAccessor.getEnvironment(context),
                    envVarAccessor.getEnvironment(), artifactoryPluginVersion);
            setDataToContext(buildContext, abstractBuildContext);
        } else if (aggregateBuildInfo) {
            // In case of no deployment configured, but build-info collection is activated.
            setBuilderData(context, abstractBuildContext, selectedServerConfig, clientConf, envVarAccessor.getEnvironment(context),
                    envVarAccessor.getEnvironment(), artifactoryPluginVersion);
            setDataToContext(buildContext, abstractBuildContext);
        }
    }

    protected boolean isServerConfigured(long selectedServerId) {
        selectedServerConfig = getConfiguredServer(buildInfoLog, selectedServerId);
        return selectedServerConfig != null;
    }

    private void setDataToContext(BuildContext context, PackageManagersContext buildContext) {
        if (selectedServerConfig != null) {
            String serverUrl = serverConfigManager.substituteVariables(selectedServerConfig.getUrl());
            context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM, serverUrl);
        }
        context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_RELEASE_ACTIVATED_PARAM,
                String.valueOf(buildContext.releaseManagementContext.isActivateReleaseManagement()));
    }

    public String createBuildInfoPropsFileAndGetItsPath(boolean shouldCaptureBuildInfo, File bambooTmp) throws IOException {
        if (selectedServerConfig == null && !shouldCaptureBuildInfo) {
            return null;
        }
        try {
            File tempPropertiesFile = File.createTempFile("buildInfo", ".properties", bambooTmp);
            clientConf.setPropertiesFile(tempPropertiesFile.getAbsolutePath());
            clientConf.persistToPropertiesFile();
            return tempPropertiesFile.getCanonicalPath();
        } catch (IOException e) {
            log.error("Error occurred while writing build info properties to a temp file. Build info " +
                    "collection is disabled.", e);
            throw e;
        }
    }

    public String createBuildInfoJSonFileAndGetItsPath(File bambooTmp) throws IOException {
        try {
            File buildInfoJsonTempFile = File.createTempFile(BuildInfoFields.GENERATED_BUILD_INFO, ".json", bambooTmp);
            clientConf.info.setGeneratedBuildInfoFilePath(buildInfoJsonTempFile.getAbsolutePath());
            return buildInfoJsonTempFile.getCanonicalPath();
        } catch (IOException e) {
            log.error("Error occurred while creating temp build info JSON file.", e);
            throw e;
        }
    }

    public void addPasswordsSystemProps(List<String> command, PackageManagersContext buildContext, @NotNull TaskContext context) {
        if (deployerPassword == null) {
            return;
        }

        command.add("-D" + clientConf.publisher.getPrefix() + "password=" + deployerPassword);
        // Adding the passwords as a variable with key that contains the word "password" will mask every instance of the password in bamboo logs.
        context.getBuildContext().getVariableContext().addLocalVariable("artifactory.password.mask.a", deployerPassword);
    }

    private void setBuilderData(TaskContext taskContext, PackageManagersContext buildContext, ServerConfig serverConfig,
                                ArtifactoryClientConfiguration clientConf, Map<String, String> environment,
                                Map<String, String> generalEnv, String pluginVersion) {
        String buildName = buildContext.getBuildName(context);
        clientConf.info.setArtifactoryPluginVersion(pluginVersion);
        clientConf.info.setBuildName(buildName);
        clientConf.publisher.addMatrixParam("build.name", buildName);

        String buildNumber = buildContext.getBuildNumber(context);
        clientConf.info.setBuildNumber(buildNumber);
        clientConf.publisher.addMatrixParam("build.number", buildNumber);

        String vcsRevision = VcsHelper.getRevisionKey(context);
        if (StringUtils.isNotBlank(vcsRevision)) {
            clientConf.info.setVcsRevision(vcsRevision);
            clientConf.publisher.addMatrixParam("vcs.revision", vcsRevision);
        }

        String[] vcsUrls = VcsHelper.getVcsUrls(context);
        if (!ArrayUtils.isEmpty(vcsUrls)) {
            clientConf.info.setVcsUrl(vcsUrls[0]);
        }

        String buildTimeStampVal = context.getBuildResult().getCustomBuildData().get("buildTimeStamp");
        long buildTimeStamp = System.currentTimeMillis();
        if (StringUtils.isNotBlank(buildTimeStampVal)) {
            buildTimeStamp = new DateTime(buildTimeStampVal).getMillis();
        }
        String buildTimeStampString = String.valueOf(buildTimeStamp);
        clientConf.info.setBuildTimestamp(buildTimeStampString);
        clientConf.publisher.addMatrixParam("build.timestamp", buildTimeStampString);

        clientConf.setActivateRecorder(true);

        StringBuilder summaryUrlBuilder = new StringBuilder(bambooBaseUrl);
        if (!bambooBaseUrl.endsWith("/")) {
            summaryUrlBuilder.append("/");
        }
        String buildUrl = summaryUrlBuilder.append("browse/").
                append(EscapeChars.forFormSubmission(context.getPlanResultKey().getKey())).toString();
        clientConf.info.setBuildUrl(buildUrl);

        setBuildParentData(clientConf, context.getTriggerReason());

        String principal = getTriggeringUserNameRecursively(context);
        if (StringUtils.isBlank(principal)) {
            principal = "auto";
        }
        clientConf.info.setPrincipal(principal);
        clientConf.info.setAgentName("Bamboo");
        clientConf.info.setAgentVersion(BuildUtils.getVersionAndBuild());
        clientConf.info.setReleaseEnabled(buildContext.releaseManagementContext.isActivateReleaseManagement());
        clientConf.info.setReleaseComment(buildContext.releaseManagementContext.getStagingComment());

        setClientData(taskContext, buildContext, clientConf, serverConfig, environment);
        if (selectedServerConfig != null) {
            setPublisherData(buildContext, clientConf, serverConfig, environment);
        } else {
            setEmptyPublisherData(clientConf);
        }

        setIncludeEnvVars(buildContext, clientConf);
        Map<String, String> props = Maps.newHashMap(environment);
        props.putAll(generalEnv);
        props = TaskUtils.getEscapedEnvMap(props);
        props.putAll(getBuildInfoConfigPropertiesFileParams(props.get(BuildInfoConfigProperties.PROP_PROPS_FILE)));
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(buildContext.getEnvVarsIncludePatterns(),
                buildContext.getEnvVarsExcludePatterns());
        clientConf.info.addBuildVariables(props, patterns);
        clientConf.fillFromProperties(props, patterns);
    }

    /**
     * Appends properties related to the parent build (if any)
     *
     * @param clientConf    Properties collection
     * @param triggerReason Build trigger reason
     */
    private void setBuildParentData(ArtifactoryClientConfiguration clientConf, TriggerReason triggerReason) {
        if (triggerReason instanceof DependencyTriggerReason) {
            String triggeringBuildResultKey = ((DependencyTriggerReason) triggerReason).getTriggeringBuildResultKey();
            if (StringUtils.isNotBlank(triggeringBuildResultKey) &&
                    (StringUtils.split(triggeringBuildResultKey, "-").length == 3)) {
                String triggeringBuildKey =
                        triggeringBuildResultKey.substring(0, triggeringBuildResultKey.lastIndexOf("-"));
                String triggeringBuildNumber =
                        triggeringBuildResultKey.substring(triggeringBuildResultKey.lastIndexOf("-") + 1);
                String parentBuildName = getBuildName(triggeringBuildKey);
                if (StringUtils.isBlank(parentBuildName)) {
                    log.error("Received a null build parent name.");
                }
                clientConf.info.setParentBuildName(parentBuildName);
                clientConf.publisher.addMatrixParam("build.parentName", parentBuildName);

                clientConf.info.setParentBuildNumber(triggeringBuildNumber);
                clientConf.publisher.addMatrixParam("build.parentNumber", triggeringBuildNumber);
            }
        }
    }

    abstract protected void setClientData(TaskContext taskContext, PackageManagersContext buildContext,
                                          ArtifactoryClientConfiguration clientConf, ServerConfig serverConfig, Map<String, String> environment);

    private void setPublisherData(PackageManagersContext buildContext,
                                  ArtifactoryClientConfiguration clientConf, ServerConfig serverConfig, Map<String, String> environment) {
        clientConf.publisher.setContextUrl(deployerUrl);
        clientConf.setTimeout(serverConfig.getTimeout());
        clientConf.publisher.setUsername(deployerUsername);
        clientConf.publisher.setRepoKey(getPublishingRepoKey(buildContext, environment));
        clientConf.publisher.setPublishArtifacts(buildContext.isPublishArtifacts());
        clientConf.publisher.setIncludePatterns(buildContext.getIncludePattern());
        clientConf.publisher.setExcludePatterns(buildContext.getExcludePattern());
        clientConf.publisher.setFilterExcludedArtifactsFromBuild(buildContext.isFilterExcludedArtifactsFromBuild());
        if (buildContext.isCaptureBuildInfo()) {
            clientConf.publisher.setPublishBuildInfo(false);
        } else {
            clientConf.publisher.setPublishBuildInfo(buildContext.isPublishBuildInfo());
        }

        // Set proxy configurations.
        ProxyUtils.setProxyConfigurationToArtifactoryClientConfig(deployerUrl, clientConf);
    }

    /**
     * Publisher data is required by build-info-maven-extractor.
     * Set empty publisher data when publish repository is not configured.
     */
    private void setEmptyPublisherData(ArtifactoryClientConfiguration clientConf) {
        clientConf.publisher.setContextUrl("http://empty_url");
        clientConf.publisher.setRepoKey("empty_repo");
        clientConf.publisher.setPublishArtifacts(false);
        clientConf.publisher.setPublishBuildInfo(false);
    }

    private void setIncludeEnvVars(PackageManagersContext buildContext, ArtifactoryClientConfiguration clientConf) {
        clientConf.setIncludeEnvVars(buildContext.isIncludeEnvVars());
        clientConf.setEnvVarsIncludePatterns(buildContext.getEnvVarsIncludePatterns());
        clientConf.setEnvVarsExcludePatterns(buildContext.getEnvVarsExcludePatterns());
    }

    private void setDeployerProperties(TaskContext taskContext, PackageManagersContext buildContext) {
        Map<String, String> runtimeContext = taskContext.getRuntimeTaskContext();
        Log buildInfoLog = new BuildInfoLog(log, taskContext.getBuildLogger());

        ServerConfig overriderServerConfig = TaskUtils.getResolutionServerConfig(
                buildContext.getOverriddenUsername(runtimeContext, buildInfoLog, true),
                buildContext.getOverriddenPassword(runtimeContext, buildInfoLog, true),
                serverConfigManager, selectedServerConfig, buildParamsOverrideManager);
        deployerUrl = overriderServerConfig.getUrl();
        deployerUsername = overriderServerConfig.getUsername();
        deployerPassword = overriderServerConfig.getPassword();
    }

    public ServerConfig getDeployServer() {
        if (deployerUrl == null) {
            return null;
        }
        return new ServerConfig(selectedServerConfig.getId(), deployerUrl, deployerUsername, deployerPassword, selectedServerConfig.getTimeout());
    }
}
