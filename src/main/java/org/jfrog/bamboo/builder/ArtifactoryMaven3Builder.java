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
import com.atlassian.bamboo.builder.AbstractMavenBuilder;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.LabelPathMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plugin.descriptor.BuilderModuleDescriptor;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.map.FilteredMap;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.spring.container.ComponentNotFoundException;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.PluginUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Artifactory Maven 3 action form and builder
 *
 * @author Noam Y. Tenne
 * @deprecated Replaced by {@link org.jfrog.bamboo.task.ArtifactoryMaven3Task}
 */
@Deprecated
public class ArtifactoryMaven3Builder extends AbstractMavenBuilder implements ArtifactoryBuilder {

    private static final Logger log = Logger.getLogger(ArtifactoryMaven3Builder.class);

    private static final String ADDITIONAL_MAVEN_PARAMS = "additionalMavenParams";
    private static final String MAVEN_OPTS_PARAM = "mavenOpts";
    private static final String SERVER_ID_PARAM = "artifactoryServerId";
    private static final String DEPLOYABLE_REPO_PARAM = "deployableRepo";
    private static final String DEPLOYER_USERNAME_PARAM = "deployerUsername";
    private static final String DEPLOYER_PASSWORD_PARAM = "deployerPassword";
    private static final String DEPLOY_ARTIFACTS_PARAM = "deployMavenArtifacts";
    private static final String DEPLOY_INCLUDE_PATTERNS_PARAM = "deployIncludePatterns";
    private static final String DEPLOY_EXCLUDE_PATTERNS_PARAM = "deployExcludePatterns";
    private static final String RUN_LICENSE_CHECKS = "runLicenseChecks";
    private static final String LICENSE_VIOLATION_RECIPIENTS = "licenseViolationRecipients";
    private static final String LIMIT_CHECKS_TO_THE_FOLLOWING_SCOPES = "limitChecksToScopes";
    private static final String INCLUDE_PUBLISHED_ARTIFACTS = "includePublishedArtifacts";
    private static final String DISABLE_AUTOMATIC_LICENSE_DISCOVERY = "disableAutoLicenseDiscovery";

    private boolean activateBuildInfoRecording;
    private String maven3DependenciesDir = null;
    private String buildInfoPropertiesFile = null;

    private String additionalMavenParams;
    private String mavenOpts;
    private long artifactoryServerId;
    private String deployableRepo;
    private String deployerUsername;
    private String deployerPassword;
    private boolean deployMavenArtifacts;
    private String deployIncludePatterns;
    private String deployExcludePatterns;
    private boolean runLicenseChecks;
    private String licenseViolationRecipients;
    private String limitChecksToScopes;
    private boolean includePublishedArtifacts;
    private boolean disableAutoLicenseDiscovery;

    private transient BuilderModuleDescriptor descriptor;
    private AdministrationConfiguration administrationConfiguration;
    private transient ServerConfigManager serverConfigManager;
    private BuilderDependencyHelper dependencyHelper;

    public ArtifactoryMaven3Builder() {
        try {
            if (ContainerManager.isContainerSetup()) {
                serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                        ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
            }
        } catch (ComponentNotFoundException cnfe) {
            System.out.println(ArtifactoryMaven3Builder.class.getName() + " - " + new Date() +
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
        return "artifactoryMaven3Builder";
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
    public String getName() {
        //return "Artifactory Maven 3 (Legacy)";
        return "Artifactory Maven 3.x";
    }

    public Map<String, LabelPathMap> addDefaultLabelPathMaps(Map<String, LabelPathMap> map) {
        return map;
    }

    @Override
    protected String getDefaultTestReportsDirectory() {
        return "**/target/surefire-reports/*.xml";
    }

    @Override
    public void setParams(@NotNull FilteredMap filteredBuilderParams) {
        super.setParams(filteredBuilderParams);

        if (filteredBuilderParams.containsKey(MAVEN_OPTS_PARAM)) {
            setMavenOpts((String) filteredBuilderParams.get(MAVEN_OPTS_PARAM));
        }

        if (filteredBuilderParams.containsKey(ADDITIONAL_MAVEN_PARAMS)) {
            setAdditionalMavenParams((String) filteredBuilderParams.get(ADDITIONAL_MAVEN_PARAMS));
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
                    setDeployMavenArtifacts(
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
        setDeployMavenArtifacts(true);
        setDeployIncludePatterns(null);
        setDeployExcludePatterns(null);
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
        Map fieldParams = EasyMap.build(getParamMapKey(ADDITIONAL_MAVEN_PARAMS), getAdditionalMavenParams(),
                getParamMapKey(MAVEN_OPTS_PARAM), getMavenOpts(),
                getParamMapKey(SERVER_ID_PARAM), Long.toString(selectedServerId),
                getParamMapKey(DEPLOYABLE_REPO_PARAM), getDeployableRepo(),
                getParamMapKey(DEPLOYER_USERNAME_PARAM), getDeployerUsername(),
                getParamMapKey(DEPLOYER_PASSWORD_PARAM), getDeployerPassword(),
                getParamMapKey(DEPLOY_ARTIFACTS_PARAM), (selectedServerId == -1) ? Boolean.TRUE.toString() :
                Boolean.toString(isDeployMavenArtifacts()),
                getParamMapKey(DEPLOY_INCLUDE_PATTERNS_PARAM), getDeployIncludePatterns(),
                getParamMapKey(DEPLOY_EXCLUDE_PATTERNS_PARAM), getDeployExcludePatterns(),
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

    public boolean isActivateBuildInfoRecording() {
        return activateBuildInfoRecording;
    }

    public String getAdditionalMavenParams() {
        return additionalMavenParams;
    }

    public void setAdditionalMavenParams(String additionalMavenParams) {
        this.additionalMavenParams = additionalMavenParams;
    }

    public String getMavenOpts() {
        return mavenOpts;
    }

    public void setMavenOpts(String mavenOpts) {
        this.mavenOpts = mavenOpts;
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

    public boolean isDeployMavenArtifacts() {
        return deployMavenArtifacts;
    }

    public void setDeployMavenArtifacts(boolean deployMavenArtifacts) {
        this.deployMavenArtifacts = deployMavenArtifacts;
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
        return true;
    }

    public String getBuilderSpecificActivationCommand() {
        return BuildInfoConfigProperties.ACTIVATE_RECORDER;
    }

    public boolean isPublishArtifacts() {
        return isDeployMavenArtifacts();
    }

    @Override
    public CurrentBuildResult runBuild(@NotNull BuildContext buildContext, @NotNull ReadOnlyCapabilitySet capabilitySet)
            throws InterruptedException {
        BuildLogger logger = getBuildLoggerManager().getBuildLogger(buildContext.getPlanKey());
        //logger.addBuildLogEntry(
        //        "################################################################################################");
        //logger.addBuildLogEntry(
        //        "#                WARNING: YOU ARE USING THE LEGACY ARTIFACTORY MAVEN 3 BUILDER.                #");
        //logger.addBuildLogEntry("" +
        //        "# PLEASE USE THE NATIVE MAVEN 3 BUILDER INSTEAD. DEVELOPMENT OF THIS ONE WILL NOT BE CONTINUED.#");
        //logger.addBuildLogEntry(
        //        "################################################################################################");
        try {
            maven3DependenciesDir = extractMaven3Dependencies();
        } catch (IOException ioe) {
            maven3DependenciesDir = null;
            String errorMessage = "Error occurred while preparing Artifactory Maven 3 Runner dependencies. " +
                    "Build Info support is disabled.";
            logger.addErrorLogEntry(errorMessage);
            log.error(errorMessage, ioe);
        }

        if (StringUtils.isNotBlank(maven3DependenciesDir)) {
            ArtifactoryBuildInfoPropertyHelper propertyHelper = new ArtifactoryBuildInfoPropertyHelper();
            propertyHelper.init(buildContext);
            propertyHelper.setAdministrationConfiguration(administrationConfiguration);
            buildInfoPropertiesFile = propertyHelper.createFileAndGetPath(null, null, null, null);
            if (StringUtils.isNotBlank(buildInfoPropertiesFile)) {
                activateBuildInfoRecording = true;
            }
        }

        return super.runBuild(buildContext, capabilitySet);
    }

    @NotNull
    @Override
    public String getCommandExecutable(ReadOnlyCapabilitySet capabilitySet) {
        if (isWindowsPlatform()) {
            return "cmd.exe";
        }
        return getJavaBin(capabilitySet);
    }

    @NotNull
    @Override
    public String[] getCommandArguments(ReadOnlyCapabilitySet capabilitySet) {
        List<String> arguments = Lists.newArrayList();
        if (isWindowsPlatform()) {
            arguments.add("/c");
            arguments.add("call");
            arguments.add(Commandline.quoteArgument(getJavaBin(capabilitySet)));
        }

        appendMavenOpts(arguments);

        String mavenHomePath = getPath(capabilitySet);

        appendClassPathArguments(arguments, mavenHomePath);
        appendClassWorldsConfArgument(arguments, mavenHomePath);
        appendBuildInfoPropertiesArgument(arguments);
        appendMavenHomeArguments(arguments, mavenHomePath);

        arguments.add("org.codehaus.plexus.classworlds.launcher.Launcher");

        appendProjectFile(arguments);
        appendGoals(arguments);
        appendAdditionalMavenParameters(arguments);
        return arguments.toArray(new String[arguments.size()]);
    }

    @Override
    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        super.setAdministrationConfiguration(administrationConfiguration);
        this.administrationConfiguration = administrationConfiguration;
    }

    @Override
    protected String getExecutableFileName() {
        return null;
    }

    /**
     * Returns the path of the java executable binary of the select JDK
     *
     * @param capabilitySet Build capabilities
     * @return Java bin path
     */
    private String getJavaBin(ReadOnlyCapabilitySet capabilitySet) {
        String jdkHome;
        String jdkCapabilityKey = (new StringBuilder()).append("system.jdk.").append(getBuildJdk()).toString();
        Capability capability = capabilitySet.getCapability(jdkCapabilityKey);
        if (capability != null) {
            jdkHome = capability.getValue();
        } else {
            return null;
        }

        if (StringUtils.isBlank(jdkHome)) {
            return null;
        }

        StringBuilder binPathBuilder = getPathBuilder(jdkHome);

        if (isWindowsPlatform()) {
            binPathBuilder.append("bin").append(File.separator).append("java.exe");
        } else {
            // IBM's AIX JDK has different locations
            String aixJdkLocation = "jre" + File.separator + "sh" + File.separator + "java";
            File aixJdk = new File(binPathBuilder.toString() + aixJdkLocation);
            if (aixJdk.isFile()) {
                binPathBuilder.append(aixJdkLocation);
            } else {
                binPathBuilder.append("bin").append(File.separator).append("java");
            }
        }

        String binPath = binPathBuilder.toString();
        binPath = getCanonicalPath(binPath);
        return binPath;
    }

    private void appendMavenOpts(List<String> arguments) {
        if (StringUtils.isNotBlank(mavenOpts)) {
            String[] mavenOptsToken = StringUtils.split(mavenOpts, " ");
            for (String opt : mavenOptsToken) {
                if (StringUtils.isNotBlank(opt)) {
                    arguments.add(Commandline.quoteArgument(opt));
                }
            }
        }
    }

    /**
     * Appends the maven classworlds classpath arguments to the command
     *
     * @param arguments     Aggregated command arguments
     * @param mavenHomePath Path to Maven installation home
     */
    private void appendClassPathArguments(List<String> arguments, String mavenHomePath) {
        arguments.add("-cp");

        StringBuilder classPathBuilder = getPathBuilder(mavenHomePath).append("boot");

        String mavenBootPath = classPathBuilder.toString();

        File mavenBootFolder = new File(mavenBootPath);
        if (!mavenBootFolder.isDirectory()) {
            throw new IllegalStateException("Could not find the Maven lib directory in the expected path: " +
                    mavenBootPath + ".");
        }
        String[] bootJars = mavenBootFolder.list();
        for (String bootJar : bootJars) {
            if (StringUtils.startsWithIgnoreCase(bootJar, "plexus-classworlds") &&
                    StringUtils.endsWithIgnoreCase(bootJar, ".jar")) {
                classPathBuilder.append(File.separator).append(bootJar);
                String classPath = getCanonicalPath(classPathBuilder.toString());
                arguments.add(Commandline.quoteArgument(classPath));
                return;
            }
        }

        throw new IllegalStateException("Could not find plexus classworlds jar in " + mavenBootPath + ".");
    }

    /**
     * Appends the maven classworlds configuration file argument to the command
     *
     * @param arguments     Aggregated command arguments
     * @param mavenHomePath Path to Maven installation home
     */
    private void appendClassWorldsConfArgument(List<String> arguments, String mavenHomePath) {
        String originalConfPath = getPathBuilder(mavenHomePath).append("bin").append(File.separator).append("m2.conf").
                toString();

        String m2ConfPath;

        /**
         * Customize the classworlds conf to activate the build info recorder only if received a valid dependency
         * directory path
         */
        if (activateBuildInfoRecording) {
            try {
                List m2ConfLines = FileUtils.readLines(new File(originalConfPath), "utf-8");
                m2ConfLines.add("load " + maven3DependenciesDir + File.separator + "*.jar");

                File tempM2Conf = File.createTempFile("artifactoryM2", "conf");
                FileUtils.writeLines(tempM2Conf, m2ConfLines);
                m2ConfPath = tempM2Conf.getCanonicalPath();
            } catch (IOException ioe) {
                throw new RuntimeException("Error occurred while writing Maven 3 customized m2.conf", ioe);
            }
        } else {
            m2ConfPath = originalConfPath;
        }

        arguments.add(Commandline.quoteArgument("-Dclassworlds.conf=" + m2ConfPath));
    }

    /**
     * Extracts the Artifactory Maven 3 recorder and all the needed to dependencies
     *
     * @return Path of recorder and dependency jar folder if extraction succeeded. Null if not
     */
    private String extractMaven3Dependencies() throws IOException {

        if (artifactoryServerId == -1) {
            return null;
        }

        return dependencyHelper.downloadDependenciesAndGetPath(getBuildDir(), null, PluginUtils.MAVEN3_KEY);
    }

    private void appendBuildInfoPropertiesArgument(List<String> arguments) {
        if (activateBuildInfoRecording) {
            arguments.add(Commandline.quoteArgument("-D" + BuildInfoConfigProperties.PROP_PROPS_FILE + "=" +
                    buildInfoPropertiesFile));
        }
    }

    /**
     * Appends the maven home argument to the command
     *
     * @param arguments     Aggregated command arguments
     * @param mavenHomePath Path to Maven installation home
     */
    private void appendMavenHomeArguments(List<String> arguments, String mavenHomePath) {
        arguments.add(Commandline.quoteArgument("-Dmaven.home=" + getCanonicalPath(mavenHomePath)));
    }

    private void appendProjectFile(List<String> arguments) {
        if (StringUtils.isNotEmpty(getProjectFile())) {
            arguments.add("-f");
            arguments.add(Commandline.quoteArgument(getProjectFile()));
        }
    }

    private void appendGoals(List<String> arguments) {
        String goals = getStringWithoutNewLines(getGoal());
        String[] goalArray = StringUtils.split(goals, " ");
        arguments.addAll(Arrays.asList(goalArray));
    }

    private void appendAdditionalMavenParameters(List<String> arguments) {
        String additionalParams = getAdditionalMavenParams();
        if (StringUtils.isNotBlank(additionalParams)) {
            String formattedParams = getStringWithoutNewLines(additionalParams);
            String[] paramArray = StringUtils.split(formattedParams, " ");
            arguments.addAll(Arrays.asList(paramArray));
        }
    }

    /**
     * Returns a general usage path {@link StringBuilder} ending with a file-system seperator
     *
     * @param homePath Base home path
     * @return String builder
     */
    private StringBuilder getPathBuilder(String homePath) {
        StringBuilder confPathBuilder = new StringBuilder(homePath);
        if (!homePath.endsWith(File.separator)) {
            confPathBuilder.append(File.separator);
        }
        return confPathBuilder;
    }

    private String getCanonicalPath(String path) {
        if (StringUtils.contains(path, " ")) {
            try {
                File f = new File(path);
                path = f.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException("IO Exception trying to get canonical path of item: " + path, e);
            }
        }
        return path;
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

    private String getStringWithoutNewLines(String stringToModify) {
        return StringUtils.replaceChars(stringToModify, "\r\n", "  ");
    }
}
