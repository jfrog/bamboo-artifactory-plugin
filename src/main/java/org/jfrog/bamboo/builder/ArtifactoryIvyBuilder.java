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

import com.atlassian.bamboo.builder.AntBuilder;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plugin.descriptor.BuilderModuleDescriptor;
import com.atlassian.bamboo.utils.error.ErrorCollection;
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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.IvyPropertyHelper;
import org.jfrog.bamboo.util.PluginUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 * @deprecated Bamboo does not use builders anymore. Replaced by {@link org.jfrog.bamboo.task.ArtifactoryIvyTask}
 */
@Deprecated
public class ArtifactoryIvyBuilder extends AntBuilder implements ArtifactoryBuilder {

    private static final Logger log = Logger.getLogger(ArtifactoryIvyBuilder.class);

    private static final String ANT_OPTS_PARAM = "antOpts";
    private static final String SERVER_ID_PARAM = "artifactoryServerId";
    private static final String DEPLOYABLE_REPO_PARAM = "deployableRepo";
    private static final String DEPLOYER_USERNAME_PARAM = "deployerUsername";
    private static final String DEPLOYER_PASSWORD_PARAM = "deployerPassword";
    private static final String DEPLOY_ARTIFACTS_PARAM = "deployArtifacts";
    private static final String DEPLOY_INCLUDE_PATTERNS_PARAM = "deployIncludePatterns";
    private static final String DEPLOY_EXCLUDE_PATTERNS_PARAM = "deployExcludePatterns";
    private static final String USE_M2_COMPATIBLE_PATTERNS_PARAM = "useM2CompatiblePatterns";
    private static final String IVY_PATTERN_PARAM = "ivyPattern";
    private static final String ARTIFACT_PATTERN_PARAM = "artifactPattern";
    private static final String RUN_LICENSE_CHECKS = "runLicenseChecks";
    private static final String LICENSE_VIOLATION_RECIPIENTS = "licenseViolationRecipients";
    private static final String LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES = "limitChecksToScopes";
    private static final String INCLUDE_PUBLISHED_ARTIFACTS = "includePublishedArtifacts";
    private static final String DISABLE_AUTOMATIC_LICENSE_DISCOVERY = "disableAutoLicenseDiscovery";

    private boolean activateBuildInfoRecording = false;
    private String ivyDependenciesDir = null;
    private String buildInfoPropertiesFile = null;

    private String antOpts;
    private long artifactoryServerId;
    private String deployableRepo;
    private String deployerUsername;
    private String deployerPassword;
    private boolean deployArtifacts;
    private String deployIncludePatterns;
    private String deployExcludePatterns;
    private boolean useM2CompatiblePatterns;
    private String ivyPattern;
    private String artifactPattern;
    private boolean runLicenseChecks;
    private String licenseViolationRecipients;
    private String limitChecksToScopes;
    private boolean includePublishedArtifacts;
    private boolean disableAutoLicenseDiscovery;

    private transient BuilderModuleDescriptor descriptor;
    private AdministrationConfiguration administrationConfiguration;
    private transient ServerConfigManager serverConfigManager;
    private BuilderDependencyHelper dependencyHelper;

    public ArtifactoryIvyBuilder() {
        try {
            if (ContainerManager.isContainerSetup()) {
                serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                        ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
            }
        } catch (ComponentNotFoundException cnfe) {
            System.out.println(ArtifactoryIvyBuilder.class.getName() + " - " + new Date() +
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
        return "artifactoryIvyBuilder";
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

        String deployableRepoParamKey = getParamMapKey(DEPLOYABLE_REPO_PARAM);
        String deployableRepo = buildConfiguration.containsKey(deployableRepoParamKey) ?
                buildConfiguration.getString(deployableRepoParamKey) : "noDeployableRepoKeyConfigured";
        context.put("selectedRepoKey", deployableRepo);

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
    @Override
    public String getName() {
        return "Artifactory Ivy";
    }

    @Override
    public void setParams(@NotNull FilteredMap filteredBuilderParams) {
        super.setParams(filteredBuilderParams);

        if (filteredBuilderParams.containsKey(ANT_OPTS_PARAM)) {
            setAntOpts((String) filteredBuilderParams.get(ANT_OPTS_PARAM));
        }

        if (filteredBuilderParams.containsKey(SERVER_ID_PARAM) &&
                StringUtils.isNotBlank(((String) filteredBuilderParams.get(SERVER_ID_PARAM)))) {
            long serverId = Long.valueOf(((String) filteredBuilderParams.get(SERVER_ID_PARAM)));
            setArtifactoryServerId(serverId);
            if (serverId != -1) {
                if (filteredBuilderParams.containsKey(DEPLOYABLE_REPO_PARAM)) {
                    setDeployableRepo((String) filteredBuilderParams.get(DEPLOYABLE_REPO_PARAM));
                }
                if (filteredBuilderParams.containsKey(DEPLOYER_USERNAME_PARAM)) {
                    setDeployerUsername((String) filteredBuilderParams.get(DEPLOYER_USERNAME_PARAM));
                }
                if (filteredBuilderParams.containsKey(DEPLOYER_PASSWORD_PARAM)) {
                    setDeployerPassword((String) filteredBuilderParams.get(DEPLOYER_PASSWORD_PARAM));
                }
                if (filteredBuilderParams.containsKey(DEPLOY_ARTIFACTS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(DEPLOY_ARTIFACTS_PARAM)))) {
                    setDeployArtifacts(
                            Boolean.valueOf(((String) filteredBuilderParams.get(DEPLOY_ARTIFACTS_PARAM))));
                }
                if (filteredBuilderParams.containsKey(DEPLOY_INCLUDE_PATTERNS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(DEPLOY_INCLUDE_PATTERNS_PARAM)))) {
                    setDeployIncludePatterns((String) filteredBuilderParams.get(DEPLOY_INCLUDE_PATTERNS_PARAM));
                }
                if (filteredBuilderParams.containsKey(DEPLOY_EXCLUDE_PATTERNS_PARAM) &&
                        StringUtils.isNotBlank(((String) filteredBuilderParams.get(DEPLOY_EXCLUDE_PATTERNS_PARAM)))) {
                    setDeployExcludePatterns((String) filteredBuilderParams.get(DEPLOY_EXCLUDE_PATTERNS_PARAM));
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
                return;
            }
        }
        setArtifactoryServerId(-1);
        setDeployableRepo(null);
        setDeployerUsername(null);
        setDeployerPassword(null);
        setDeployArtifacts(true);
        setDeployIncludePatterns(null);
        setDeployExcludePatterns(null);
        setUseM2CompatiblePatterns(true);
        setIvyPattern(null);
        setArtifactPattern(null);
        setRunLicenseChecks(false);
        setLicenseViolationRecipients(null);
        setLimitChecksToScopes(null);
        setIncludePublishedArtifacts(false);
        setDisableAutoLicenseDiscovery(false);
    }

    @NotNull
    @Override
    public ErrorCollection validate(@NotNull BuildConfiguration buildConfiguration) {
        ErrorCollection supersErrorCollection = super.validate(buildConfiguration);
        SubnodeConfiguration config = buildConfiguration.configurationAt(getKeyPrefix());

        if (!config.containsKey(SERVER_ID_PARAM)) {
            return supersErrorCollection;
        }

        long configuredServerId;
        try {
            configuredServerId = config.getLong(SERVER_ID_PARAM);
        } catch (ConversionException ce) {
            configuredServerId = -1;
        }
        if (configuredServerId == -1) {
            return supersErrorCollection;
        }

        ServerConfig serverConfig = serverConfigManager.getServerConfigById(configuredServerId);
        if (serverConfig == null) {
            supersErrorCollection.addError(getKeyPrefix(), SERVER_ID_PARAM,
                    "Could not find Artifactory server configuration by the ID " + configuredServerId);
        }

        if (StringUtils.isBlank(config.getString(DEPLOYABLE_REPO_PARAM))) {
            supersErrorCollection.addError(getKeyPrefix(), DEPLOYABLE_REPO_PARAM,
                    "Please choose a repository to deploy to.");
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
                        supersErrorCollection.addError(getKeyPrefix(), LICENSE_VIOLATION_RECIPIENTS, "'" +
                                recipientToken + "' is not a valid e-mail address.");
                        break;
                    }
                }
            }
        }

        return supersErrorCollection;
    }

    @NotNull
    @Override
    public Map<String, String> getFullParams() {
        Map supersFieldParams = super.getFullParams();
        long selectedServerId = getArtifactoryServerId();
        Map fieldParams = EasyMap.build(getParamMapKey(ANT_OPTS_PARAM), getAntOpts(),
                getParamMapKey(SERVER_ID_PARAM), Long.toString(selectedServerId),
                getParamMapKey(DEPLOYABLE_REPO_PARAM), getDeployableRepo(),
                getParamMapKey(DEPLOYER_USERNAME_PARAM), getDeployerUsername(),
                getParamMapKey(DEPLOYER_PASSWORD_PARAM), getDeployerPassword(),
                getParamMapKey(DEPLOY_ARTIFACTS_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isDeployArtifacts()),
                getParamMapKey(DEPLOY_INCLUDE_PATTERNS_PARAM), getDeployIncludePatterns(),
                getParamMapKey(DEPLOY_EXCLUDE_PATTERNS_PARAM), getDeployExcludePatterns(),
                getParamMapKey(USE_M2_COMPATIBLE_PATTERNS_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isUseM2CompatiblePatterns()),
                getParamMapKey(IVY_PATTERN_PARAM), getIvyPattern(),
                getParamMapKey(ARTIFACT_PATTERN_PARAM), getArtifactPattern(),
                getParamMapKey(RUN_LICENSE_CHECKS), (selectedServerId == -1) ? Boolean.FALSE.toString() :
                Boolean.toString(isRunLicenseChecks()),
                getParamMapKey(LICENSE_VIOLATION_RECIPIENTS), getLicenseViolationRecipients(),
                getParamMapKey(LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES), getLimitChecksToScopes(),
                getParamMapKey(INCLUDE_PUBLISHED_ARTIFACTS), (selectedServerId == -1) ? Boolean.FALSE.toString() :
                Boolean.toString(isIncludePublishedArtifacts()),
                getParamMapKey(DISABLE_AUTOMATIC_LICENSE_DISCOVERY),
                (selectedServerId == -1) ? Boolean.FALSE.toString() :
                        Boolean.toString(isDisableAutoLicenseDiscovery()));
        supersFieldParams.putAll(fieldParams);
        return supersFieldParams;
    }

    public String getAntOpts() {
        return antOpts;
    }

    public void setAntOpts(String antOpts) {
        this.antOpts = antOpts;
    }

    public long getArtifactoryServerId() {
        return artifactoryServerId;
    }

    public void setArtifactoryServerId(long artifactoryServerId) {
        this.artifactoryServerId = artifactoryServerId;
    }

    public String getDeployableRepo() {
        return deployableRepo;
    }

    public void setDeployableRepo(String deployableRepo) {
        this.deployableRepo = deployableRepo;
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

    public boolean isDeployArtifacts() {
        return deployArtifacts;
    }

    public void setDeployArtifacts(boolean deployArtifacts) {
        this.deployArtifacts = deployArtifacts;
    }

    public String getDeployIncludePatterns() {
        return deployIncludePatterns;
    }

    public void setDeployIncludePatterns(String deployIncludePatterns) {
        this.deployIncludePatterns = deployIncludePatterns;
    }

    public String getDeployExcludePatterns() {
        return deployExcludePatterns;
    }

    public void setDeployExcludePatterns(String deployExcludePatterns) {
        this.deployExcludePatterns = deployExcludePatterns;
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

    public boolean isHaveBuilderSpecificActivationCommand() {
        return false;
    }

    public String getBuilderSpecificActivationCommand() {
        return null;
    }

    public boolean isPublishArtifacts() {
        return isDeployArtifacts();
    }

    @Override
    public CurrentBuildResult runBuild(@NotNull BuildContext buildContext, @NotNull ReadOnlyCapabilitySet capabilitySet)
            throws InterruptedException {

        try {
            ivyDependenciesDir = extractIvyDependencies();
        } catch (IOException ioe) {
            ivyDependenciesDir = null;
            log.error("Error occurred while preparing Artifactory Ivy Runner dependencies. Build Info support is " +
                    "disabled.", ioe);
        }

        if (StringUtils.isNotBlank(ivyDependenciesDir)) {
            ArtifactoryBuildInfoPropertyHelper propertyHelper = new IvyPropertyHelper();
            propertyHelper.init(buildContext);
            propertyHelper.setAdministrationConfiguration(administrationConfiguration);
            buildInfoPropertiesFile = propertyHelper.createFileAndGetPath(null, null,
                    null, null);

            if (StringUtils.isNotBlank(buildInfoPropertiesFile)) {
                activateBuildInfoRecording = true;
            }
        }
        return super.runBuild(buildContext, capabilitySet);
    }

    @NotNull
    @Override
    public String[] getCommandArguments(ReadOnlyCapabilitySet capabilitySet) {
        List<String> commandArguments = Lists.newArrayList(super.getCommandArguments(capabilitySet));

        if (activateBuildInfoRecording) {
            commandArguments.add("-lib");
            commandArguments.add(Commandline.quoteArgument(ivyDependenciesDir));
        }

        return commandArguments.toArray(new String[commandArguments.size()]);
    }

    @Override
    public String[] getEnvironmentSetting(String vmParams, String javaHome) {
        StringBuilder antOptsBuilder = new StringBuilder();

        antOptsBuilder.append("ANT_OPTS=");

        if (StringUtils.isNotBlank(antOpts)) {
            antOptsBuilder.append(antOpts);
        }

        if (activateBuildInfoRecording) {
            antOptsBuilder.append(" ").append(Commandline.quoteArgument(new StringBuilder("-D").
                    append(BuildInfoConfigProperties.PROP_PROPS_FILE).append("=").append(buildInfoPropertiesFile).
                    toString()));
        }

        String[] environmentSetting = super.getEnvironmentSetting(vmParams, javaHome);
        String[] settingsWithAntOpts = Arrays.copyOf(environmentSetting, environmentSetting.length + 1);
        settingsWithAntOpts[environmentSetting.length] = antOptsBuilder.toString();
        return settingsWithAntOpts;
    }

    @Override
    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        super.setAdministrationConfiguration(administrationConfiguration);
        this.administrationConfiguration = administrationConfiguration;
    }

    /**
     * Extracts the Artifactory Ivy recorder and all the needed to dependencies
     *
     * @return Path of recorder and dependency jar folder if extraction succeeded. Null if not
     */
    private String extractIvyDependencies() throws IOException {

        if (artifactoryServerId == -1) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(getBuildDir(), null, PluginUtils.IVY_KEY);
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