package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.util.TaskConfigurationValidations;
import org.jfrog.bamboo.context.ArtifactoryBuildContext;
import org.jfrog.bamboo.context.IvyBuildContext;
import org.jfrog.bamboo.context.PackageManagersContext;

import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.context.ArtifactoryBuildContext.DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryIvyTask}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryIvyConfiguration extends AbstractArtifactoryConfiguration {
    public static final String KEY = "artifactoryIvyBuilder";
    protected static final String DEFAULT_TEST_REPORTS_XML = "**/test-reports/*.xml";
    private static final Set<String> FIELDS_TO_COPY = IvyBuildContext.getFieldsToCopy();

    @Override
    protected String getBuilderContextPrefix() {
        return IvyBuildContext.PREFIX;
    }

    @Override
    protected String getCapabilityPrefix() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".ivy";
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    protected String getDefaultTestDirectory() {
        return DEFAULT_TEST_REPORTS_XML;
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateLegacyContextForCreate(context);
        context.put("artifactoryIvyTask", this);
        context.put("builderType", this);
        context.put("builder", this);
        context.put("adminConfig", getAdministrationConfiguration());
        context.put("baseUrl", getAdministrationConfiguration().getBaseUrl());
        context.put("build", context.get("plan"));
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put("selectedRepoKey", "");
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);

        String publishingKey = IvyBuildContext.PREFIX + IvyBuildContext.DEPLOYABLE_REPO_KEY;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                IvyBuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        context.put("selectedServerId", context.get(IvyBuildContext.PREFIX + IvyBuildContext.SERVER_ID_PARAM));
        context.put("serverConfigManager", serverConfigManager);
        populateDefaultEnvVarsExcludePatternsInBuildContext(context);
        populateDefaultBuildNameNumberInBuildContext(context);

        // Backward compatibility for tasks with overridden username and password
        Map<String, String> taskConfiguration = taskDefinition.getConfiguration();
        IvyBuildContext taskContext = new IvyBuildContext(taskConfiguration);
        if (StringUtils.isBlank(taskContext.getDeployerOverrideCredentialsChoice()) &&
                StringUtils.isNoneBlank(taskContext.getDeployerUsername(), taskContext.getDeployerPassword())) {
            context.put(DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, CVG_CRED_USERNAME_PASSWORD);
        }
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
            @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> configMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(configMap, params, FIELDS_TO_COPY);
        IvyBuildContext buildContext = new IvyBuildContext(configMap);
        resetDeployerConfigIfNeeded(buildContext);
        resetResolverConfigIfNeeded(buildContext);
        configMap.put(IvyBuildContext.PREFIX + IvyBuildContext.TEST_RESULT_DIRECTORY, getTestDirectory(buildContext));

        decryptFields(configMap);
        return configMap;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition definition) {
        return new IvyBuildContext(definition.getConfiguration()).isTestChecked();
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        // Validate deployment server.
        String resolutionServerKey = IvyBuildContext.PREFIX + IvyBuildContext.SERVER_ID_PARAM;
        String resolutionRepoKey = IvyBuildContext.PREFIX + IvyBuildContext.DEPLOYABLE_REPO_KEY;
        TaskConfigurationValidations.validateArtifactoryServerAndRepo(resolutionServerKey, resolutionRepoKey, serverConfigManager, params, errorCollection);

        // Validate Build File.
        String buildFileKey = IvyBuildContext.PREFIX + IvyBuildContext.BUILD_FILE;
        if (StringUtils.isBlank(params.getString(buildFileKey))) {
            errorCollection.addError(buildFileKey, "Please specify Build File.");
        }

        // Validate Targets.
        String targetsKey = IvyBuildContext.PREFIX + IvyBuildContext.TARGET_OPTS_PARAM;
        if (StringUtils.isBlank(params.getString(targetsKey))) {
            errorCollection.addError(targetsKey, "Please specify Targets.");
        }

        // Validate Build JDK.
        String buildJdkKey = IvyBuildContext.PREFIX + PackageManagersContext.JDK;
        TaskConfigurationValidations.validateJdk(buildJdkKey, params, errorCollection);

        // Validate Executable.
        String executableKey = IvyBuildContext.PREFIX + PackageManagersContext.EXECUTABLE;
        TaskConfigurationValidations.validateExecutable(executableKey, params, errorCollection);

        // Validate build name and number.
        TaskConfigurationValidations.validateCaptureBuildInfoParams(ArtifactoryBuildContext.BUILD_NAME, ArtifactoryBuildContext.BUILD_NUMBER, IvyBuildContext.CAPTURE_BUILD_INFO, params, errorCollection);
    }
}
