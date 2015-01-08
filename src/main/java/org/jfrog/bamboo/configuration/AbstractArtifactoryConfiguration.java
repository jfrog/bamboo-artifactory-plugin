package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.atlassian.spring.container.ComponentNotFoundException;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.ConstantValues;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all {@link com.atlassian.bamboo.task.TaskConfigurator}s that are used by the plugin. It sets the
 * {@link ServerConfigManager} to be used for populating the Artifactory relevant fields. It also serves as a common
 * ground for setting common fields in the context of the build.
 *
 * @author Tomer Cohen
 */
public abstract class AbstractArtifactoryConfiguration extends AbstractTaskConfigurator implements
        TaskTestResultsSupport, BuildTaskRequirementSupport {

    public static final String CFG_TEST_RESULTS_FILE_PATTERN_OPTION_CUSTOM = "customTestDirectory";
    public static final String CFG_TEST_RESULTS_FILE_PATTERN_OPTION_STANDARD = "standardTestDirectory";
    private static final Map TEST_RESULTS_FILE_PATTERN_TYPES = ImmutableMap
            .of(CFG_TEST_RESULTS_FILE_PATTERN_OPTION_STANDARD, "Look in the standard test results directory.",
                    CFG_TEST_RESULTS_FILE_PATTERN_OPTION_CUSTOM, "Specify custom results directories");
    protected transient ServerConfigManager serverConfigManager;
    protected AdministrationConfiguration administrationConfiguration;
    protected UIConfigSupport uiConfigSupport;
    private String builderContextPrefix;
    private String capabilityPrefix;

    protected AbstractArtifactoryConfiguration(String builderContextPrefix) {
        this(builderContextPrefix, null);
    }

    protected AbstractArtifactoryConfiguration(String builderContextPrefix, @Nullable String capabilityPrefix) {
        this.builderContextPrefix = builderContextPrefix;
        this.capabilityPrefix = capabilityPrefix;

        try {
            if (ContainerManager.isContainerSetup()) {
                serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                        ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
            }
        } catch (ComponentNotFoundException cnfe) {
            System.out.println(ArtifactoryGradleConfiguration.class.getName() + " - " + new Date() +
                    " - Warning: could not find component 'Artifactory Server Configuration Manager' (Can be ignored " +
                    "when running on a remote agent).");
        }
    }

    public String getTestDirectory(AbstractBuildContext buildContext) {
        String directoryOption = buildContext.getTestDirectoryOption();
        if (CFG_TEST_RESULTS_FILE_PATTERN_OPTION_STANDARD.equals(directoryOption)) {
            return getDefaultTestDirectory();
        } else if (CFG_TEST_RESULTS_FILE_PATTERN_OPTION_CUSTOM.equals(directoryOption)) {
            return buildContext.getTestDirectory();
        }
        return null;
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateContextForAllOperations(context);
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateContextForAllOperations(context);
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        populateContextForAllOperations(context);
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
            @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfigMap.put("baseUrl", administrationConfiguration.getBaseUrl());
        return taskConfigMap;
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        String serverKey = "builder." + getKey() + "." + AbstractBuildContext.SERVER_ID_PARAM;
        if (!params.containsKey(serverKey)) {
            return;
        }
        long configuredServerId;
        try {
            configuredServerId = params.getLong(serverKey, -1);
        } catch (ConversionException ce) {
            configuredServerId = -1;
        }
        if (configuredServerId == -1) {
            return;
        }
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(configuredServerId);
        if (serverConfig == null) {
            errorCollection.addError(serverKey,
                    "Could not find Artifactory server configuration by the ID " + configuredServerId);
        }
        String deployerRepoKey = "builder." + getKey() + "." + getDeployableRepoKey();
        if (StringUtils.isBlank(params.getString(deployerRepoKey))) {
            errorCollection.addError(deployerRepoKey, "Please choose a repository to deploy to.");
        }
        String runLicensesKey = "builder." + getKey() + "." + AbstractBuildContext.RUN_LICENSE_CHECKS;
        String runLicenseChecksValue = params.getString(runLicensesKey);
        if (StringUtils.isNotBlank(runLicenseChecksValue) && Boolean.valueOf(runLicenseChecksValue)) {
            String violationsKey = "builder." + getKey() + "." + AbstractBuildContext.LICENSE_VIOLATION_RECIPIENTS;
            String recipients = params.getString(violationsKey);
            if (StringUtils.isNotBlank(recipients)) {
                String[] recipientTokens = StringUtils.split(recipients, ' ');
                for (String recipientToken : recipientTokens) {
                    if (StringUtils.isNotBlank(recipientToken) &&
                            (!recipientToken.contains("@")) || recipientToken.startsWith("@") ||
                            recipientToken.endsWith("@")) {
                        errorCollection
                                .addError(violationsKey, "'" + recipientToken + "' is not a valid e-mail address.");
                        break;
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition, @NotNull Job job) {
        Set<Requirement> requirements = Sets.newHashSet();
        taskConfiguratorHelper.addJdkRequirement(requirements, taskDefinition,
                builderContextPrefix + TaskConfigConstants.CFG_JDK_LABEL);
        if (StringUtils.isNotBlank(capabilityPrefix)) {
            taskConfiguratorHelper.addSystemRequirementFromConfiguration(requirements, taskDefinition,
                    builderContextPrefix + AbstractBuildContext.EXECUTABLE, capabilityPrefix);
        }
        return requirements;
    }

    // populate common objects into context
    private void populateContextForAllOperations(@NotNull Map<String, Object> context) {
        context.put("uiConfigBean", uiConfigSupport);
        context.put("testDirectoryTypes", TEST_RESULTS_FILE_PATTERN_TYPES);
        context.put(AbstractBuildContext.PUBLISH_BUILD_INFO_PARAM, "true");
        context.put(AbstractBuildContext.ENV_VARS_EXCLUDE_PATTERNS, "*password*,*secret*");
    }

    /**
     * Sets the UI config bean from bamboo. NOTE: This method is called from Bamboo upon instantiation of this class by
     * reflection.
     *
     * @param uiConfigSupport The UI config bean for select values.
     */
    public void setUiConfigSupport(UIConfigSupport uiConfigSupport) {
        this.uiConfigSupport = uiConfigSupport;
    }

    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }

    /**
     * Reset the build context configuration back to the default values if no server id was selected
     *
     * @param buildContext The build context which holds the environment for the configuration.
     */
    protected void resetConfigIfNeeded(AbstractBuildContext buildContext) {
        long serverId = buildContext.getArtifactoryServerId();
        if (serverId == -1) {
            buildContext.resetContextToDefault();
        }
    }

    /**
     * @return The unique key identifier of the task configuration.
     */
    protected abstract String getKey();

    /**
     * @return The key for the deployable/publishing repo key for the environment.
     */
    protected abstract String getDeployableRepoKey();

    protected abstract String getDefaultTestDirectory();
}
