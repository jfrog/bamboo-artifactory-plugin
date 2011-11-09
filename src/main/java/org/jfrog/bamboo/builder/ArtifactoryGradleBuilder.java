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

import com.atlassian.bamboo.builder.AbstractBuilder;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.LabelPathMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plugin.descriptor.BuilderModuleDescriptor;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.error.SimpleErrorCollection;
import com.atlassian.bamboo.utils.map.FilteredMap;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.spring.container.ComponentNotFoundException;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.util.ConfigurationPathHolder;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.PluginUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoTask;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Noam Y. Tenne
 * @deprecated Bamboo does not use builders anymore. Replaced by {@link org.jfrog.bamboo.task.ArtifactoryGradleTask}
 */
@Deprecated
public class ArtifactoryGradleBuilder extends AbstractBuilder {

    private static final Logger log = Logger.getLogger(ArtifactoryGradleBuilder.class);

    private static final String SWITCHES_PARAM = "switches";
    private static final String TASKS_PARAM = "tasks";
    private static final String BUILD_SCRIPT_PARAM = "buildScript";
    private static final String BUILD_FILE_PARAM = "buildFile";
    private static final String SERVER_ID_PARAM = "artifactoryServerId";
    private static final String RESOLUTION_REPO_PARAM = "resolutionRepo";
    private static final String PUBLISHING_REPO_PARAM = "publishingRepo";
    private static final String DEPLOYER_USERNAME_PARAM = "deployerUsername";
    private static final String DEPLOYER_PASSWORD_PARAM = "deployerPassword";
    private static final String PUBLISH_BUILD_INFO_PARAM = "publishBuildInfo";
    private static final String INCLUDE_ENV_VARS_PARAM = "includeEnvVars";
    private static final String RUN_LICENSE_CHECKS = "runLicenseChecks";
    private static final String LICENSE_VIOLATION_RECIPIENTS = "licenseViolationRecipients";
    private static final String LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES = "limitChecksToScopes";
    private static final String INCLUDE_PUBLISHED_ARTIFACTS = "includePublishedArtifacts";
    private static final String DISABLE_AUTOMATIC_LICENSE_DISCOVERY = "disableAutoLicenseDiscovery";

    private static final String PUBLISH_ARTIFACTS_PARAM = "publishArtifacts";
    private static final String PUBLISH_MAVEN_DESCRIPTORS_PARAM = "publishMavenDescriptors";
    private static final String PUBLISH_IVY_DESCRIPTORS_PARAM = "publishIvyDescriptors";
    private static final String USE_M2_COMPATIBLE_PATTERNS_PARAM = "useM2CompatiblePatterns";
    private static final String IVY_PATTERN_PARAM = "ivyPattern";
    private static final String ARTIFACT_PATTERN_PARAM = "artifactPattern";
    private static final String PUBLISH_INCLUDE_PATTERNS_PARAM = "publishIncludePatterns";
    private static final String PUBLISH_EXCLUDE_PATTERNS_PARAM = "publishExcludePatterns";

    public static final String NO_RESOLUTION_REPO_KEY_CONFIGURED = "noResolutionRepoKeyConfigured";

    private boolean activateBuildInfoRecording = false;
    private String gradleDependenciesDir = null;
    private ConfigurationPathHolder configurationPathHolder = null;

    private String switches;
    private String tasks;
    private String buildScript;
    private String buildFile;
    private long artifactoryServerId;
    private String resolutionRepo;
    private String publishingRepo;
    private String deployerUsername;
    private String deployerPassword;
    private boolean publishBuildInfo;
    private boolean includeEnvVars;
    private boolean runLicenseChecks;
    private String licenseViolationRecipients;
    private String limitChecksToScopes;
    private boolean includePublishedArtifacts;
    private boolean disableAutoLicenseDiscovery;

    private boolean publishArtifacts;
    private boolean publishMavenDescriptors;
    private boolean publishIvyDescriptors;
    private boolean useM2CompatiblePatterns;
    private String ivyPattern;
    private String artifactPattern;
    private String publishIncludePatterns;
    private String publishExcludePatterns;

    private transient BuilderModuleDescriptor descriptor;
    private AdministrationConfiguration administrationConfiguration;
    private transient ServerConfigManager serverConfigManager;
    private BuilderDependencyHelper dependencyHelper;

    public ArtifactoryGradleBuilder() {
        try {
            if (ContainerManager.isContainerSetup()) {
                serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                        ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
            }
        } catch (ComponentNotFoundException cnfe) {
            System.out.println(ArtifactoryGradleBuilder.class.getName() + " - " + new Date() +
                    " - Warning: could not find component 'Artifactory Server Configuration Manager' (Can be ignored " +
                    "when running on a remote agent).");
        }
    }

    @Override
    public void init(@NotNull ModuleDescriptor moduleDescriptor) {
        super.init(moduleDescriptor);
        descriptor = ((BuilderModuleDescriptor) moduleDescriptor);
        dependencyHelper = new BuilderDependencyHelper(getKey());
        ContainerManager.autowireComponent(dependencyHelper);
    }

    @NotNull
    @Override
    public String getKey() {
        return "artifactoryGradleBuilder";
    }

    @NotNull
    @Override
    public String getEditHtml(@NotNull BuildConfiguration buildConfiguration, @NotNull Plan plan) {
        String editTemplateLocation = descriptor.getFreemarkerEditTemplate();
        Map context = getTemplateContext();
        context.put(getKey(), this);
        context.put("builderType", this);
        context.put("builder", this);
        context.put("adminConfig", administrationConfiguration);
        context.put("baseUrl", administrationConfiguration.getBaseUrl());
        context.put("build", plan);
        context.put("plan", plan);
        context.put("buildConfiguration", buildConfiguration);
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);

        String serverIdParamKey = getParamMapKey(SERVER_ID_PARAM);
        long serverId;
        try {
            serverId = buildConfiguration.containsKey(serverIdParamKey) ?
                    buildConfiguration.getLong(serverIdParamKey) : -1;
        } catch (ConversionException ce) {
            serverId = -1;
        }
        context.put("selectedServerId", serverId);

        String resolvingRepoParamKey = getParamMapKey(RESOLUTION_REPO_PARAM);
        String resolving = buildConfiguration.containsKey(resolvingRepoParamKey) ?
                buildConfiguration.getString(resolvingRepoParamKey) : NO_RESOLUTION_REPO_KEY_CONFIGURED;
        context.put("selectedResolutionRepoKey", resolving);

        String publishingRepoParamKey = getParamMapKey(PUBLISHING_REPO_PARAM);
        String publishingRepo = buildConfiguration.containsKey(publishingRepoParamKey) ?
                buildConfiguration.getString(publishingRepoParamKey) : "noPublishingRepoKeyConfigured";
        context.put("selectedPublishingRepoKey", publishingRepo);

        return templateRenderer.render(editTemplateLocation, context);
    }

    @NotNull
    @Override
    public String getViewHtml(@NotNull Plan plan) {
        String templatePath = descriptor.getViewTemplate();

        String selectedServerUrl = "";
        if (getArtifactoryServerId() != -1) {
            ServerConfig serverConfig = serverConfigManager.getServerConfigById(getArtifactoryServerId());
            if (serverConfig != null) {
                selectedServerUrl = serverConfig.getUrl();
            } else {
                selectedServerUrl = "Unable to find server configuration";
            }
        }

        Map map = EasyMap.build("builder", this, "build", plan, "plan", plan, "selectedServerUrl", selectedServerUrl);
        return templateRenderer.render(templatePath, map);
    }

    @NotNull
    public String getName() {
        return "Artifactory Gradle";
    }

    @Override
    public void setParams(@NotNull FilteredMap filteredBuilderParams) {
        super.setParams(filteredBuilderParams);

        if (filteredBuilderParams.containsKey(SWITCHES_PARAM)) {
            setSwitches((String) filteredBuilderParams.get(SWITCHES_PARAM));
        }
        if (filteredBuilderParams.containsKey(TASKS_PARAM)) {
            setTasks((String) filteredBuilderParams.get(TASKS_PARAM));
        }
        if (filteredBuilderParams.containsKey(BUILD_SCRIPT_PARAM)) {
            setBuildScript((String) filteredBuilderParams.get(BUILD_SCRIPT_PARAM));
        }
        if (filteredBuilderParams.containsKey(BUILD_FILE_PARAM)) {
            setBuildFile((String) filteredBuilderParams.get(BUILD_FILE_PARAM));
        }

        if (filteredBuilderParams.containsKey(SERVER_ID_PARAM) &&
                StringUtils.isNotBlank(((String) filteredBuilderParams.get(SERVER_ID_PARAM)))) {
            long serverId = Long.valueOf(((String) filteredBuilderParams.get(SERVER_ID_PARAM)));
            setArtifactoryServerId(serverId);
            if (serverId != -1) {
                if (filteredBuilderParams.containsKey(RESOLUTION_REPO_PARAM)) {
                    setResolutionRepo((String) filteredBuilderParams.get(RESOLUTION_REPO_PARAM));
                }
                if (filteredBuilderParams.containsKey(PUBLISHING_REPO_PARAM)) {
                    setPublishingRepo((String) filteredBuilderParams.get(PUBLISHING_REPO_PARAM));
                }
                if (filteredBuilderParams.containsKey(DEPLOYER_USERNAME_PARAM)) {
                    setDeployerUsername((String) filteredBuilderParams.get(DEPLOYER_USERNAME_PARAM));
                }
                if (filteredBuilderParams.containsKey(DEPLOYER_PASSWORD_PARAM)) {
                    setDeployerPassword((String) filteredBuilderParams.get(DEPLOYER_PASSWORD_PARAM));
                }


                if (filteredBuilderParams.containsKey(PUBLISH_BUILD_INFO_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(PUBLISH_BUILD_INFO_PARAM)))) {
                    setPublishBuildInfo(
                            Boolean.valueOf(((String) filteredBuilderParams.get(PUBLISH_BUILD_INFO_PARAM))));
                }
                if (filteredBuilderParams.containsKey(INCLUDE_ENV_VARS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(INCLUDE_ENV_VARS_PARAM)))) {
                    setIncludeEnvVars(Boolean.valueOf(((String) filteredBuilderParams.get(INCLUDE_ENV_VARS_PARAM))));
                }
                if (filteredBuilderParams.containsKey(RUN_LICENSE_CHECKS) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(RUN_LICENSE_CHECKS)))) {
                    setRunLicenseChecks(Boolean.valueOf(((String) filteredBuilderParams.get(RUN_LICENSE_CHECKS))));
                }
                if (filteredBuilderParams.containsKey(LICENSE_VIOLATION_RECIPIENTS)) {
                    setLicenseViolationRecipients(((String) filteredBuilderParams.get(LICENSE_VIOLATION_RECIPIENTS)));
                }
                if (filteredBuilderParams.containsKey(LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES)) {
                    setLimitChecksToScopes(((String) filteredBuilderParams.get(LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES)));
                }
                if (filteredBuilderParams.containsKey(INCLUDE_PUBLISHED_ARTIFACTS) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(INCLUDE_PUBLISHED_ARTIFACTS)))) {
                    setIncludePublishedArtifacts(
                            Boolean.valueOf(((String) filteredBuilderParams.get(INCLUDE_PUBLISHED_ARTIFACTS))));
                }
                if (filteredBuilderParams.containsKey(DISABLE_AUTOMATIC_LICENSE_DISCOVERY) &&
                        StringUtils.isNotBlank(
                                ((String) filteredBuilderParams.get(DISABLE_AUTOMATIC_LICENSE_DISCOVERY)))) {
                    setDisableAutoLicenseDiscovery(
                            Boolean.valueOf(((String) filteredBuilderParams.get(DISABLE_AUTOMATIC_LICENSE_DISCOVERY))));
                }

                if (filteredBuilderParams.containsKey(PUBLISH_ARTIFACTS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(PUBLISH_ARTIFACTS_PARAM)))) {
                    setPublishArtifacts(Boolean.valueOf(((String) filteredBuilderParams.get(PUBLISH_ARTIFACTS_PARAM))));
                }
                if (filteredBuilderParams.containsKey(PUBLISH_MAVEN_DESCRIPTORS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(PUBLISH_MAVEN_DESCRIPTORS_PARAM)))) {
                    setPublishMavenDescriptors(
                            Boolean.valueOf(((String) filteredBuilderParams.get(PUBLISH_MAVEN_DESCRIPTORS_PARAM))));
                }
                if (filteredBuilderParams.containsKey(PUBLISH_IVY_DESCRIPTORS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(PUBLISH_IVY_DESCRIPTORS_PARAM)))) {
                    setPublishIvyDescriptors(
                            Boolean.valueOf(((String) filteredBuilderParams.get(PUBLISH_IVY_DESCRIPTORS_PARAM))));
                }
                if (filteredBuilderParams.containsKey(USE_M2_COMPATIBLE_PATTERNS_PARAM) &&
                        StringUtils
                                .isNotBlank(((String) filteredBuilderParams.get(USE_M2_COMPATIBLE_PATTERNS_PARAM)))) {
                    setUseM2CompatiblePatterns(
                            Boolean.valueOf(((String) filteredBuilderParams.get(USE_M2_COMPATIBLE_PATTERNS_PARAM))));
                }
                if (filteredBuilderParams.containsKey(IVY_PATTERN_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(IVY_PATTERN_PARAM)))) {
                    setIvyPattern((String) filteredBuilderParams.get(IVY_PATTERN_PARAM));
                }
                if (filteredBuilderParams.containsKey(ARTIFACT_PATTERN_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(ARTIFACT_PATTERN_PARAM)))) {
                    setArtifactPattern((String) filteredBuilderParams.get(ARTIFACT_PATTERN_PARAM));
                }
                if (filteredBuilderParams.containsKey(PUBLISH_INCLUDE_PATTERNS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(PUBLISH_INCLUDE_PATTERNS_PARAM)))) {
                    setPublishIncludePatterns((String) filteredBuilderParams.get(PUBLISH_INCLUDE_PATTERNS_PARAM));
                }
                if (filteredBuilderParams.containsKey(PUBLISH_EXCLUDE_PATTERNS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(PUBLISH_EXCLUDE_PATTERNS_PARAM)))) {
                    setPublishExcludePatterns((String) filteredBuilderParams.get(PUBLISH_EXCLUDE_PATTERNS_PARAM));
                }
                return;
            }
        }
        setArtifactoryServerId(-1);
        setResolutionRepo(null);
        setPublishingRepo(null);
        setDeployerUsername(null);
        setDeployerPassword(null);

        setPublishBuildInfo(true);
        setIncludeEnvVars(false);
        setRunLicenseChecks(false);
        setLicenseViolationRecipients(null);
        setLimitChecksToScopes(null);
        setIncludePublishedArtifacts(false);
        setDisableAutoLicenseDiscovery(false);

        setPublishArtifacts(true);
        setPublishMavenDescriptors(true);
        setPublishIvyDescriptors(true);
        setUseM2CompatiblePatterns(true);
        setIvyPattern(null);
        setArtifactPattern(null);
        setPublishIncludePatterns(null);
        setPublishExcludePatterns(null);
    }

    @Override
    protected ErrorCollection validate(FilteredMap<String> map) {
        ErrorCollection errorCollection = new SimpleErrorCollection();

        if (!map.containsKey(SERVER_ID_PARAM)) {
            return errorCollection;
        }

        long configuredServerId;
        try {
            configuredServerId = Long.valueOf(map.get(SERVER_ID_PARAM));
        } catch (ConversionException ce) {
            configuredServerId = -1;
        }
        if (configuredServerId == -1) {
            return errorCollection;
        }

        ServerConfig serverConfig = serverConfigManager.getServerConfigById(configuredServerId);
        if (serverConfig == null) {
            errorCollection.addError(getKeyPrefix(), SERVER_ID_PARAM,
                    "Could not find Artifactory server configuration by the ID " + configuredServerId);
        }

        if (StringUtils.isBlank(map.get(PUBLISHING_REPO_PARAM))) {
            errorCollection.addError(getKeyPrefix(), PUBLISHING_REPO_PARAM,
                    "Please choose a repository to publish to.");
        }

        String runLicenseChecksValue = map.get(RUN_LICENSE_CHECKS);
        if (StringUtils.isNotBlank(runLicenseChecksValue) && Boolean.valueOf(runLicenseChecksValue)) {
            String recipients = map.get(LICENSE_VIOLATION_RECIPIENTS);
            if (StringUtils.isNotBlank(recipients)) {
                String[] recipientTokens = StringUtils.split(recipients, ' ');
                for (String recipientToken : recipientTokens) {
                    if (StringUtils.isNotBlank(recipientToken) &&
                            (!recipientToken.contains("@")) || recipientToken.startsWith("@") ||
                            recipientToken.endsWith("@")) {
                        errorCollection.addError(getKeyPrefix(), LICENSE_VIOLATION_RECIPIENTS, "'" +
                                recipientToken + "' is not a valid e-mail address.");
                        break;
                    }
                }
            }
        }

        return errorCollection;
    }

    @NotNull
    public ErrorCollection validate(@NotNull BuildConfiguration buildConfiguration) {
        ErrorCollection errorCollection = new SimpleErrorCollection();
        SubnodeConfiguration config = buildConfiguration.configurationAt(getKeyPrefix());

        if (!config.containsKey(SERVER_ID_PARAM)) {
            return errorCollection;
        }

        long configuredServerId;
        try {
            configuredServerId = config.getLong(SERVER_ID_PARAM);
        } catch (ConversionException ce) {
            configuredServerId = -1;
        }
        if (configuredServerId == -1) {
            return errorCollection;
        }

        ServerConfig serverConfig = serverConfigManager.getServerConfigById(configuredServerId);
        if (serverConfig == null) {
            errorCollection.addError(getKeyPrefix(), SERVER_ID_PARAM,
                    "Could not find Artifactory server configuration by the ID " + configuredServerId);
        }

        if (StringUtils.isBlank(config.getString(PUBLISHING_REPO_PARAM))) {
            errorCollection.addError(getKeyPrefix(), PUBLISHING_REPO_PARAM,
                    "Please choose a repository to publish to.");
        }

        String runLicenseChecksValue = config.getString(RUN_LICENSE_CHECKS);
        if (StringUtils.isNotBlank(runLicenseChecksValue) && Boolean.valueOf(runLicenseChecksValue)) {
            String recipients = config.getString(LICENSE_VIOLATION_RECIPIENTS);
            if (StringUtils.isNotBlank(recipients)) {
                String[] recipientTokens = StringUtils.split(recipients, ' ');
                for (String recipientToken : recipientTokens) {
                    if (StringUtils.isNotBlank(recipientToken) &&
                            (!recipientToken.contains("@")) || recipientToken.startsWith("@") ||
                            recipientToken.endsWith("@")) {
                        errorCollection.addError(getKeyPrefix(), LICENSE_VIOLATION_RECIPIENTS, "'" +
                                recipientToken + "' is not a valid e-mail address.");
                        break;
                    }
                }
            }
        }

        return errorCollection;
    }

    public Map<String, LabelPathMap> addDefaultLabelPathMaps(Map<String, LabelPathMap> map) {
        return map;
    }

    public void addDefaultValues(@NotNull BuildConfiguration configuration) {
    }

    public boolean isPathValid(String homeDirectory) {
        if (homeDirectory == null) {
            return false;
        }

        String files[] = null;
        File testOriginalDataDir = new File(homeDirectory, "bin/");
        if (testOriginalDataDir.exists()) {
            files = testOriginalDataDir.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.equals("gradle") || name.equals("gradle.bat");
                }
            });
        }

        return files != null && files.length > 0;
    }

    @NotNull
    @Override
    public Map<String, String> getFullParams() {
        Map supersFieldParams = super.getFullParams();
        long selectedServerId = getArtifactoryServerId();
        Map fieldParams = EasyMap.build(
                getParamMapKey(SWITCHES_PARAM), getSwitches(),
                getParamMapKey(TASKS_PARAM), getTasks(),
                getParamMapKey(BUILD_FILE_PARAM), getBuildFile(),
                getParamMapKey(BUILD_SCRIPT_PARAM), getBuildScript(),

                getParamMapKey(SERVER_ID_PARAM), Long.toString(selectedServerId),
                getParamMapKey(RESOLUTION_REPO_PARAM), getResolutionRepo(),
                getParamMapKey(PUBLISHING_REPO_PARAM), getPublishingRepo(),
                getParamMapKey(DEPLOYER_USERNAME_PARAM), getDeployerUsername(),
                getParamMapKey(DEPLOYER_PASSWORD_PARAM), getDeployerPassword(),

                getParamMapKey(PUBLISH_BUILD_INFO_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isPublishBuildInfo()),
                getParamMapKey(INCLUDE_ENV_VARS_PARAM), (selectedServerId == -1) ? Boolean.FALSE.toString() :
                Boolean.toString(isIncludeEnvVars()),
                getParamMapKey(RUN_LICENSE_CHECKS), (selectedServerId == -1) ? Boolean.FALSE.toString() :
                Boolean.toString(isRunLicenseChecks()),
                getParamMapKey(LICENSE_VIOLATION_RECIPIENTS), getLicenseViolationRecipients(),
                getParamMapKey(LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES), getLimitChecksToScopes(),
                getParamMapKey(INCLUDE_PUBLISHED_ARTIFACTS), (selectedServerId == -1) ? Boolean.FALSE.toString() :
                Boolean.toString(isIncludePublishedArtifacts()),
                getParamMapKey(DISABLE_AUTOMATIC_LICENSE_DISCOVERY),
                (selectedServerId == -1) ? Boolean.FALSE.toString() :
                        Boolean.toString(isDisableAutoLicenseDiscovery()),

                getParamMapKey(PUBLISH_ARTIFACTS_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isPublishArtifacts()),
                getParamMapKey(PUBLISH_MAVEN_DESCRIPTORS_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isPublishMavenDescriptors()),
                getParamMapKey(PUBLISH_IVY_DESCRIPTORS_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isPublishIvyDescriptors()),
                getParamMapKey(USE_M2_COMPATIBLE_PATTERNS_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isUseM2CompatiblePatterns()),
                getParamMapKey(IVY_PATTERN_PARAM), getIvyPattern(),
                getParamMapKey(ARTIFACT_PATTERN_PARAM), getArtifactPattern(),
                getParamMapKey(PUBLISH_INCLUDE_PATTERNS_PARAM), getPublishIncludePatterns(),
                getParamMapKey(PUBLISH_EXCLUDE_PATTERNS_PARAM), getPublishExcludePatterns()

        );
        supersFieldParams.putAll(fieldParams);
        return supersFieldParams;
    }

    public String getGradleDependenciesDir() {
        return gradleDependenciesDir;
    }

    public String getSwitches() {
        return switches;
    }

    public void setSwitches(String switches) {
        this.switches = switches;
    }

    public String getTasks() {
        return tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public String getBuildScript() {
        return buildScript;
    }

    public void setBuildScript(String buildScript) {
        this.buildScript = buildScript;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    public long getArtifactoryServerId() {
        return artifactoryServerId;
    }

    public void setArtifactoryServerId(long artifactoryServerId) {
        this.artifactoryServerId = artifactoryServerId;
    }

    public String getResolutionRepo() {
        return resolutionRepo;
    }

    public void setResolutionRepo(String resolutionRepo) {
        this.resolutionRepo = resolutionRepo;
    }

    public String getPublishingRepo() {
        return publishingRepo;
    }

    public void setPublishingRepo(String publishingRepo) {
        this.publishingRepo = publishingRepo;
    }

    public String getDeployerUsername() {
        return deployerUsername;
    }

    public void setDeployerUsername(String deployerUsername) {
        this.deployerUsername = deployerUsername;
    }

    public String getDeployerPassword() {
        return deployerPassword;
    }

    public void setDeployerPassword(String deployerPassword) {
        this.deployerPassword = deployerPassword;
    }

    public boolean isPublishBuildInfo() {
        return publishBuildInfo;
    }

    public void setPublishBuildInfo(boolean publishBuildInfo) {
        this.publishBuildInfo = publishBuildInfo;
    }

    public boolean isIncludeEnvVars() {
        return includeEnvVars;
    }

    public void setIncludeEnvVars(boolean includeEnvVars) {
        this.includeEnvVars = includeEnvVars;
    }

    public boolean isRunLicenseChecks() {
        return runLicenseChecks;
    }

    public void setRunLicenseChecks(boolean runLicenseChecks) {
        this.runLicenseChecks = runLicenseChecks;
    }

    public String getLicenseViolationRecipients() {
        return licenseViolationRecipients;
    }

    public void setLicenseViolationRecipients(String licenseViolationRecipients) {
        this.licenseViolationRecipients = licenseViolationRecipients;
    }

    public String getLimitChecksToScopes() {
        return limitChecksToScopes;
    }

    public void setLimitChecksToScopes(String limitChecksToScopes) {
        this.limitChecksToScopes = limitChecksToScopes;
    }

    public boolean isIncludePublishedArtifacts() {
        return includePublishedArtifacts;
    }

    public void setIncludePublishedArtifacts(boolean includePublishedArtifacts) {
        this.includePublishedArtifacts = includePublishedArtifacts;
    }

    public boolean isDisableAutoLicenseDiscovery() {
        return disableAutoLicenseDiscovery;
    }

    public void setDisableAutoLicenseDiscovery(boolean disableAutoLicenseDiscovery) {
        this.disableAutoLicenseDiscovery = disableAutoLicenseDiscovery;
    }

    public boolean isPublishArtifacts() {
        return publishArtifacts;
    }

    public void setPublishArtifacts(boolean publishArtifacts) {
        this.publishArtifacts = publishArtifacts;
    }

    public boolean isPublishMavenDescriptors() {
        return publishMavenDescriptors;
    }

    public void setPublishMavenDescriptors(boolean publishMavenDescriptors) {
        this.publishMavenDescriptors = publishMavenDescriptors;
    }

    public boolean isPublishIvyDescriptors() {
        return publishIvyDescriptors;
    }

    public void setPublishIvyDescriptors(boolean publishIvyDescriptors) {
        this.publishIvyDescriptors = publishIvyDescriptors;
    }

    public boolean isUseM2CompatiblePatterns() {
        return useM2CompatiblePatterns;
    }

    public void setUseM2CompatiblePatterns(boolean useM2CompatiblePatterns) {
        this.useM2CompatiblePatterns = useM2CompatiblePatterns;
    }

    public String getIvyPattern() {
        return ivyPattern;
    }

    public void setIvyPattern(String ivyPattern) {
        this.ivyPattern = ivyPattern;
    }

    public String getArtifactPattern() {
        return artifactPattern;
    }

    public void setArtifactPattern(String artifactPattern) {
        this.artifactPattern = artifactPattern;
    }

    public String getPublishIncludePatterns() {
        return publishIncludePatterns;
    }

    public void setPublishIncludePatterns(String publishIncludePatterns) {
        this.publishIncludePatterns = publishIncludePatterns;
    }

    public String getPublishExcludePatterns() {
        return publishExcludePatterns;
    }

    public void setPublishExcludePatterns(String publishExcludePatterns) {
        this.publishExcludePatterns = publishExcludePatterns;
    }

    @Override
    public CurrentBuildResult runBuild(@NotNull BuildContext buildContext, @NotNull ReadOnlyCapabilitySet capabilitySet)
            throws InterruptedException {

        try {
            gradleDependenciesDir = extractGradleDependencies();
        } catch (IOException ioe) {
            gradleDependenciesDir = null;
            log.error("Error occurred while preparing Artifactory Gradle Runner dependencies. Build Info support is " +
                    "disabled.", ioe);
        }

        if (StringUtils.isNotBlank(gradleDependenciesDir)) {
            configurationPathHolder = getGradleInitScriptFile(buildContext);

            if (configurationPathHolder != null) {
                activateBuildInfoRecording = true;
            }
        }
        return super.runBuild(buildContext, capabilitySet);
    }

    private ConfigurationPathHolder getGradleInitScriptFile(BuildContext buildContext) {
        File gradleJarFile = new File(gradleDependenciesDir, PluginUtils.getPluginProperty(PluginUtils.GRADLE_KEY));
        if (!gradleJarFile.exists()) {
            log.warn("Unable to locate the Gradle extractor. Build-info task will not be added.");
            return null;
        }

        InputStream initScriptStream = null;
        JarFile gradleJar = null;
        try {
            gradleJar = new JarFile(gradleJarFile);
            ZipEntry initScriptEntry = gradleJar.getEntry("initscripttemplate.gradle");

            if (initScriptEntry == null) {
                log.warn("Unable to locate the Gradle init script. Build-info task will not be added.");
                return null;
            }

            initScriptStream = gradleJar.getInputStream(initScriptEntry);
            if (initScriptStream == null) {
                log.warn("Unable to locate the gradle init script template. Build-info task will not be added.");
                return null;
            }

            String scriptTemplate = IOUtils.toString(initScriptStream);
            GradleInitScriptHelper initScriptHelper = new GradleInitScriptHelper();
            initScriptHelper.init(buildContext);
            initScriptHelper.setAdministrationConfiguration(administrationConfiguration);
            return null;
        } catch (IOException e) {
            log.warn("Unable to read from the Gradle extractor jar. Build-info task will not be added: " +
                    e.getMessage());
            return null;
        } finally {
            IOUtils.closeQuietly(initScriptStream);
            try {
                gradleJar.close();
            } catch (IOException e) {
                log.warn("Unable to close the Gradle extractor jar: " + e.getMessage());
            }
        }
    }

    @NotNull
    @Override
    public String getCommandExecutable(ReadOnlyCapabilitySet capabilitySet) {
        String gradleHome = getPath(capabilitySet);

        if (isWindowsPlatform()) {
            return "cmd.exe";
        } else {
            return new StringBuilder(gradleHome).append(File.separator).append("bin").append(File.separator).
                    append("gradle").toString();
        }
    }

    @NotNull
    @Override
    public String[] getCommandArguments(ReadOnlyCapabilitySet capabilitySet) {
        List<String> arguments = Lists.newArrayList();

        if (isWindowsPlatform()) {
            String gradleHome = getPath(capabilitySet);
            StringBuilder gradleScript = new StringBuilder(gradleHome).append(File.separator).append("bin").
                    append(File.separator).append("gradle.bat");
            arguments.add("/c");
            arguments.add("call");
            arguments.add(Commandline.quoteArgument(gradleScript.toString()));
        }

        if (StringUtils.isNotBlank(switches)) {
            String[] switchTokens = StringUtils.split(switches, ' ');
            arguments.addAll(Arrays.asList(switchTokens));
        }

        arguments.add("-b");

        StringBuilder buildFileBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(buildScript)) {
            buildFileBuilder.append(buildScript).append(File.separator);
        }

        if (StringUtils.isNotBlank(buildFile)) {
            buildFileBuilder.append(buildFile);
        } else {
            buildFileBuilder.append("build.gradle");
        }

        arguments.add(buildFileBuilder.toString());

        List<String> tasksToAdd = Lists.newArrayList();
        if (StringUtils.isNotBlank(tasks)) {
            String[] taskTokens = StringUtils.split(tasks, ' ');
            tasksToAdd.addAll(Arrays.asList(taskTokens));
        }

        if (activateBuildInfoRecording) {
            arguments.add("-I");
            arguments.add(Commandline.quoteArgument(configurationPathHolder.getInitScriptPath()));
            arguments.add(Commandline.quoteArgument("-D" + BuildInfoConfigProperties.PROP_PROPS_FILE + "=" +
                    configurationPathHolder.getClientConfPath()));
            tasksToAdd.add(BuildInfoTask.BUILD_INFO_TASK_NAME);
        }

        arguments.addAll(tasksToAdd);

        return arguments.toArray(new String[arguments.size()]);
    }

    @Override
    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        super.setAdministrationConfiguration(administrationConfiguration);
        this.administrationConfiguration = administrationConfiguration;
    }

    /**
     * Extracts the Artifactory Gradle recorder and all the needed to dependencies
     *
     * @return Path of recorder and dependency jar folder if extraction succeeded. Null if not
     */

    private String extractGradleDependencies() throws IOException {

        if (artifactoryServerId == -1) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(getBuildDir(), null, PluginUtils.GRADLE_KEY);
    }

    /**
     * Appends the configuration map prefix to the given parameter name
     *
     * @param paramName Parameter name to prefix
     * @return Prefixed parameter name
     */
    private String getParamMapKey(String paramName) {
        return (new StringBuilder()).append(getKeyPrefix()).append(".").append(paramName).toString();
    }
}