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

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.util.BuildUtils;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.trigger.DependencyTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.ManualBuildTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.version.ScmHelper;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.*;

/**
 * @author Noam Y. Tenne
 */
public abstract class ArtifactoryBuildInfoPropertyHelper extends BaseBuildInfoHelper {

    private static final Logger log = Logger.getLogger(ArtifactoryBuildInfoPropertyHelper.class);

    public String createFileAndGetPath(AbstractBuildContext buildContext,
                                       BuildLogger logger, Map<String, String> taskEnv, Map<String, String> generalEnv,
                                       String artifactoryPluginVersion) {
        long selectedServerId = buildContext.getArtifactoryServerId();

        if (selectedServerId != -1) {

            ServerConfig serverConfig = serverConfigManager.getServerConfigById(selectedServerId);
            if (serverConfig == null) {

                String warningMessage =
                        "Found an ID of a selected Artifactory server configuration (" + selectedServerId +
                                ") but could not find a matching configuration. Build info collection is disabled.";
                logger.addBuildLogHeader(warningMessage, true);
                log.warn(warningMessage);
            } else {
                ArtifactoryClientConfiguration clientConf = new ArtifactoryClientConfiguration(new NullLog());
                addBuilderInfoProperties(buildContext, serverConfig, clientConf, taskEnv, generalEnv, artifactoryPluginVersion);
                try {
                    File tempPropertiesFile = File.createTempFile("buildInfo", "properties");
                    clientConf.setPropertiesFile(tempPropertiesFile.getAbsolutePath());
                    clientConf.persistToPropertiesFile();
                    String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());
                    context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
                    context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM, serverUrl);
                    this.context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_RELEASE_ACTIVATED_PARAM,
                            String.valueOf(buildContext.releaseManagementContext.isActivateReleaseManagement()));

                    return tempPropertiesFile.getCanonicalPath();
                } catch (IOException ioe) {
                    log.error("Error occurred while writing build info properties to a temp file. Build info " +
                            "collection is disabled.", ioe);
                }
            }
        }

        return null;
    }

    private void addBuilderInfoProperties(AbstractBuildContext buildContext, ServerConfig serverConfig,
                                          ArtifactoryClientConfiguration clientConf, Map<String, String> environment,
                                          Map<String, String> generalEnv, String pluginVersion) {
        String buildName = context.getPlanName();
        clientConf.info.setArtifactoryPluginVersion(pluginVersion);
        clientConf.info.setBuildName(buildName);
        clientConf.publisher.addMatrixParam("build.name", buildName);

        String buildNumber = String.valueOf(context.getBuildNumber());
        clientConf.info.setBuildNumber(buildNumber);
        clientConf.publisher.addMatrixParam("build.number", buildNumber);

        String vcsRevision = ScmHelper.getRevisionKey(context);
        if (StringUtils.isNotBlank(vcsRevision)) {
            clientConf.info.setVcsRevision(vcsRevision);
            clientConf.publisher.addMatrixParam("vcs.revision", vcsRevision);
        }

        String vcsUrl = ScmHelper.getVcsUrl(context);
        if (StringUtils.isNotBlank(vcsUrl)) {
            clientConf.info.setVcsUrl(vcsUrl);
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
                append(EscapeChars.forFormSubmission(context.getBuildResultKey())).toString();
        clientConf.info.setBuildUrl(buildUrl);

        addBuildParentProperties(clientConf, context.getTriggerReason());

        String principal = getTriggeringUserNameRecursively(context);
        if (StringUtils.isBlank(principal)) {
            principal = "auto";
        }
        clientConf.info.setPrincipal(principal);
        clientConf.info.setAgentName("Bamboo");
        clientConf.info.setAgentVersion(BuildUtils.getVersionAndBuild());
        clientConf.info.licenseControl.setRunChecks(buildContext.isRunLicenseChecks());
        clientConf.info.licenseControl.setViolationRecipients(buildContext.getLicenseViolationRecipients());
        clientConf.info.licenseControl.setScopes(buildContext.getScopes());
        clientConf.info.licenseControl.setIncludePublishedArtifacts(buildContext.isIncludePublishedArtifacts());
        clientConf.info.licenseControl.setAutoDiscover(!buildContext.isDisableAutomaticLicenseDiscovery());
        clientConf.info.setReleaseEnabled(buildContext.releaseManagementContext.isActivateReleaseManagement());
        clientConf.info.setReleaseComment(buildContext.releaseManagementContext.getStagingComment());

        // Blackduck integration
        try {
            BeanUtils.copyProperties(clientConf.info.blackDuckProperties, buildContext.blackDuckProperties);
        } catch (Exception e) {
            throw new RuntimeException("Could not integrate black duck properties", e);
        }

        addClientProperties(buildContext, clientConf, serverConfig, environment);
        addPublisherProperties(buildContext, clientConf, serverConfig, environment);

        Map<String, String> props = filterAndGetGlobalVariables();
        props.putAll(environment);
        props.putAll(generalEnv);
        props = TaskUtils.getEscapedEnvMap(props);
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(buildContext.getEnvVarsIncludePatterns(),
                buildContext.getEnvVarsExcludePatterns());
        clientConf.info.addBuildVariables(props, patterns);
        clientConf.fillFromProperties(props, patterns);
    }

    private String getTriggeringUserNameRecursively(BuildContext context) {
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

    /**
     * Appends properties related to the parent build (if any)
     *
     * @param clientConf    Properties collection
     * @param triggerReason Build trigger reason
     */
    private void addBuildParentProperties(ArtifactoryClientConfiguration clientConf, TriggerReason triggerReason) {
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

    abstract protected void addClientProperties(AbstractBuildContext buildContext,
                                                ArtifactoryClientConfiguration clientConf, ServerConfig serverConfig, Map<String, String> environment);

    private void addPublisherProperties(AbstractBuildContext buildContext,
                                        ArtifactoryClientConfiguration clientConf, ServerConfig serverConfig, Map<String, String> environment) {

        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());
        clientConf.publisher.setContextUrl(serverUrl);
        clientConf.setTimeout(serverConfig.getTimeout());

        String deployerUsername = overrideParam(buildContext.getDeployerUsername(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME);
        if (StringUtils.isBlank(deployerUsername)) {
            deployerUsername = serverConfig.getUsername();
        }
        String password = overrideParam(buildContext.getDeployerPassword(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD);
        if (StringUtils.isBlank(password)) {
            password = serverConfig.getPassword();
        }
        if (StringUtils.isNotBlank(deployerUsername)) {
            clientConf.publisher.setUsername(deployerUsername);
            clientConf.publisher.setPassword(password);
        }
        clientConf.publisher.setRepoKey(getPublishingRepoKey(buildContext, environment));
        clientConf.publisher.setPublishArtifacts(buildContext.isPublishArtifacts());
        clientConf.publisher.setIncludePatterns(buildContext.getIncludePattern());
        clientConf.publisher.setExcludePatterns(buildContext.getExcludePattern());
        clientConf.publisher.setFilterExcludedArtifactsFromBuild(buildContext.isFilterExcludedArtifactsFromBuild());
        clientConf.publisher.setPublishBuildInfo(buildContext.isPublishBuildInfo());
        clientConf.setIncludeEnvVars(buildContext.isIncludeEnvVars());
        clientConf.setEnvVarsIncludePatterns(buildContext.getEnvVarsIncludePatterns());
        clientConf.setEnvVarsExcludePatterns(buildContext.getEnvVarsExcludePatterns());
    }
}
