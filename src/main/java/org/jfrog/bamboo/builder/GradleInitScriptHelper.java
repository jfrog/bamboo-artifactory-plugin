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
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.util.ConfigurationPathHolder;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.version.ScmHelper;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.*;

/**
 * @author Noam Y. Tenne
 */
public class GradleInitScriptHelper extends BaseBuildInfoHelper {

    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(GradleInitScriptHelper.class);

    public ConfigurationPathHolder createAndGetGradleInitScriptPath(String dependenciesDir,
            GradleBuildContext buildContext,
            BuildLogger logger, String scriptTemplate, Map<String, String> taskEnv, Map<String, String> generalEnv) {
        long selectedServerId = buildContext.getArtifactoryServerId();
        if (selectedServerId != -1) {
            //Using "getInstance()" since the field must be transient
            ServerConfig serverConfig = serverConfigManager.getServerConfigById(selectedServerId);
            if (serverConfig == null) {

                String warningMessage =
                        "Found an ID of a selected Artifactory server configuration (" + selectedServerId +
                                ") but could not find a matching configuration. Build info collection is disabled.";
                logger.addBuildLogHeader(warningMessage, true);
                log.warn(warningMessage);
                return null;
            } else {
                String normalizedPath = FilenameUtils.separatorsToUnix(dependenciesDir);
                scriptTemplate = scriptTemplate.replace("${pluginLibDir}", normalizedPath);
                try {
                    File buildProps = File.createTempFile("buildinfo", "properties");
                    ArtifactoryClientConfiguration configuration =
                            createClientConfiguration(buildContext, serverConfig, taskEnv);
                    // Add Bamboo build variables
                    MapDifference<String, String> buildVarDifference = Maps.difference(generalEnv, System.getenv());
                    Map<String, String> filteredBuildVarDifferences = buildVarDifference.entriesOnlyOnLeft();
                    IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                            buildContext.getEnvVarsIncludePatterns(),
                            buildContext.getEnvVarsExcludePatterns());
                    configuration.info.addBuildVariables(filteredBuildVarDifferences, patterns);
                    configuration.setPropertiesFile(buildProps.getAbsolutePath());
                    configuration.persistToPropertiesFile();
                    File tempInitScript = File.createTempFile("artifactory.init.script", "gradle");
                    FileUtils.writeStringToFile(tempInitScript, scriptTemplate, "utf-8");
                    if (buildContext.isPublishBuildInfo()) {
                        this.context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM,
                                "true");
                        this.context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM,
                                serverConfig.getUrl());
                        this.context.getBuildResult().getCustomBuildData().put(BUILD_RESULT_RELEASE_ACTIVATED_PARAM,
                                String.valueOf(buildContext.releaseManagementContext.isActivateReleaseManagement()));
                    }
                    return new ConfigurationPathHolder(tempInitScript.getCanonicalPath(),
                            buildProps.getCanonicalPath());
                } catch (IOException e) {
                    log.warn("An error occurred while creating the gradle build info init script. " +
                            "Build-info task will not be added.", e);
                }
            }
        }

        return null;
    }

    private ArtifactoryClientConfiguration createClientConfiguration(GradleBuildContext buildContext,
            ServerConfig serverConfig, Map<String, String> taskEnv) {
        ArtifactoryClientConfiguration clientConf = new ArtifactoryClientConfiguration(new NullLog());
        String buildName = context.getPlanName();
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

        String buildTimeStampVal = context.getBuildResult().getCustomBuildData().get("buildTimeStamp");
        long buildTimeStamp = System.currentTimeMillis();
        if (StringUtils.isNotBlank(buildTimeStampVal)) {
            buildTimeStamp = new DateTime(buildTimeStampVal).getMillis();
        }
        String buildTimeStampString = String.valueOf(buildTimeStamp);
        clientConf.info.setBuildTimestamp(buildTimeStampString);
        clientConf.publisher.addMatrixParam("build.timestamp", buildTimeStampString);

        StringBuilder summaryUrlBuilder = new StringBuilder(bambooBaseUrl);
        if (!bambooBaseUrl.endsWith("/")) {
            summaryUrlBuilder.append("/");
        }
        String buildUrl = summaryUrlBuilder.append("browse/").
                append(EscapeChars.forURL(context.getBuildResultKey())).toString();
        clientConf.info.setBuildUrl(buildUrl);

        String principal = getTriggeringUserNameRecursively(context);
        if (StringUtils.isBlank(principal)) {
            principal = "auto";
        }
        clientConf.info.setPrincipal(principal);
        addBuildParentProperties(clientConf, context.getTriggerReason());

        clientConf.info.setAgentName("Bamboo");
        clientConf.info.setAgentVersion(BuildUtils.getVersionAndBuild());
        clientConf.info.licenseControl.setRunChecks(buildContext.isRunLicenseChecks());
        clientConf.info.licenseControl.setViolationRecipients(buildContext.getLicenseViolationRecipients());
        clientConf.info.licenseControl.setScopes(buildContext.getScopes());
        clientConf.info.licenseControl.setIncludePublishedArtifacts(buildContext.isIncludePublishedArtifacts());
        clientConf.info.licenseControl.setAutoDiscover(!buildContext.isDisableAutomaticLicenseDiscovery());
        clientConf.info.setReleaseEnabled(buildContext.releaseManagementContext.isActivateReleaseManagement());
        clientConf.info.setReleaseComment(buildContext.releaseManagementContext.getStagingComment());
        addClientProperties(clientConf, serverConfig, buildContext);
        clientConf.setIncludeEnvVars(buildContext.isIncludeEnvVars());
        clientConf.setEnvVarsIncludePatterns(buildContext.getEnvVarsIncludePatterns());
        clientConf.setEnvVarsExcludePatterns(buildContext.getEnvVarsExcludePatterns());

        Map<String, String> globalVars = filterAndGetGlobalVariables();
        globalVars = TaskUtils.getEscapedEnvMap(globalVars);
        globalVars.putAll(TaskUtils.getEscapedEnvMap(taskEnv));
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(buildContext.getEnvVarsIncludePatterns(),
                buildContext.getEnvVarsExcludePatterns());
        clientConf.info.addBuildVariables(globalVars, patterns);
        clientConf.fillFromProperties(globalVars, patterns);
        return clientConf;
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
                clientConf.publisher.addMatrixParam(BuildInfoFields.BUILD_PARENT_NAME, parentBuildName);
                clientConf.info.setParentBuildNumber(triggeringBuildNumber);
                clientConf.publisher.addMatrixParam(BuildInfoFields.BUILD_PARENT_NUMBER, triggeringBuildNumber);
            }
        }
    }

    private void addClientProperties(ArtifactoryClientConfiguration clientConf, ServerConfig serverConfig,
            GradleBuildContext buildContext) {
        clientConf.publisher.setContextUrl(serverConfig.getUrl());
        clientConf.resolver.setContextUrl(serverConfig.getUrl());
        clientConf.setTimeout(serverConfig.getTimeout());
        clientConf.publisher.setRepoKey(buildContext.getPublishingRepo());
        if (StringUtils.isNotBlank(buildContext.releaseManagementContext.getReleaseRepoKey())) {
            clientConf.publisher.setRepoKey(buildContext.releaseManagementContext.getReleaseRepoKey());
        }
        String resolutionRepo = buildContext.getResolutionRepo();
        if (StringUtils.isNotBlank(resolutionRepo) &&
                !GradleBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED.equals(resolutionRepo)) {
            clientConf.resolver.setRepoKey(resolutionRepo);
        }

        String globalServerUsername = serverConfig.getUsername();
        String globalServerPassword = serverConfig.getPassword();
        clientConf.resolver.setUsername(globalServerUsername);
        clientConf.resolver.setPassword(globalServerPassword);

        String deployerUsername = buildContext.getDeployerUsername();
        if (StringUtils.isBlank(deployerUsername)) {
            deployerUsername = globalServerUsername;
        }
        String deployerPassword = buildContext.getDeployerPassword();
        if (StringUtils.isBlank(deployerPassword)) {
            deployerPassword = globalServerPassword;
        }
        if (StringUtils.isNotBlank(deployerUsername)) {
            clientConf.publisher.setUsername(deployerUsername);
            clientConf.publisher.setPassword(deployerPassword);
        }
        boolean publishArtifacts = buildContext.isPublishArtifacts();
        clientConf.publisher.setPublishArtifacts(publishArtifacts);
        clientConf.publisher.setIncludePatterns(buildContext.getIncludePattern());
        clientConf.publisher.setExcludePatterns(buildContext.getExcludePattern());
        if (publishArtifacts) {
            boolean m2Compatible = buildContext.isMaven2Compatible();
            clientConf.publisher.setM2Compatible(m2Compatible);
            if (!m2Compatible) {
                clientConf.publisher.setIvyPattern(buildContext.getIvyPattern());
                clientConf.publisher.setIvyArtifactPattern(buildContext.getArtifactPattern());
            }
        }
        clientConf.publisher.setPublishBuildInfo(buildContext.isPublishBuildInfo());
        clientConf.publisher.setIvy(buildContext.isPublishIvyDescriptors());
        clientConf.publisher.setMaven(buildContext.isPublishMavenDescriptors());
        String artifactSpecs = buildContext.getArtifactSpecs();
        if (StringUtils.isNotBlank(artifactSpecs)) {
            clientConf.publisher.setArtifactSpecs(artifactSpecs);
        }
    }
}
