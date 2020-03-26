package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.deployment.LegacyDeploymentUtils;

import java.util.Map;
import java.util.Set;

/**
 * Artifactory Deployment task configuration
 *
 * @author Aviad Shikloshi
 */
public class ArtifactoryDeploymentUploadConfiguration extends AbstractArtifactoryConfiguration {

    // Prefix for each configured field.
    public static final String DEPLOYMENT_PREFIX = "artifactory.deployment.";
    // The configured repository. Used in the old deoployment implementation.
    public static final String LEGACY_DEPLOYMENT_REPOSITORY = "deploymentRepository";
    public static final String PASSWORD = "password";
    public static final String USERNAME = "username";
    // DropDown field. Determines which spec source the task should use. Can be "file" or "jobConfiguration"
    public static final String SPEC_SOURCE_CHOICE = "specSourceChoice";
    // Plain text field that contains the configured fileSpec. Will be used if @SPEC_SOURCE_CHOICE is configured to "jobConfiguration"
    public static final String SPEC_SOURCE_JOB_CONFIGURATION = "jobConfiguration";
    // Plain text field that contains a path to a spec file on the filesystem. Will be used if @SPEC_SOURCE_CHOICE is configured to "file"
    public static final String SPEC_SOURCE_FILE = "file";

    private static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(
                DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM,
                DEPLOYMENT_PREFIX + USERNAME,
                DEPLOYMENT_PREFIX + PASSWORD,
                DEPLOYMENT_PREFIX + LEGACY_DEPLOYMENT_REPOSITORY,
                DEPLOYMENT_PREFIX + SPEC_SOURCE_CHOICE,
                DEPLOYMENT_PREFIX + SPEC_SOURCE_JOB_CONFIGURATION,
                DEPLOYMENT_PREFIX + SPEC_SOURCE_FILE
        );
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateLegacyContextForCreate(context);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put(AbstractBuildContext.SERVER_ID_PARAM, -1);

        contextPutEmpty(context, USERNAME);
        contextPutEmpty(context, PASSWORD);
        contextPutEmpty(context, AbstractBuildContext.SERVER_ID_PARAM);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, getFieldsToCopy());
        String selectedServerId = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM);
        String username = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + USERNAME);
        String password = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + PASSWORD);

        // In case selectedServerId, username or password are empty, try to read their values from an older
        // configuration format.
        if (StringUtils.isBlank(selectedServerId)) {
            // Compatibility with 1.8.0
            selectedServerId = taskDefinition.getConfiguration().get("artifactoryServerId");
        }
        if (StringUtils.isBlank(username)) {
            // Compatibility with 1.8.0
            username = taskDefinition.getConfiguration().get("username");
            context.put(DEPLOYMENT_PREFIX + USERNAME, username);
        }
        if (StringUtils.isBlank(password)) {
            // Compatibility with 1.8.0
            password = taskDefinition.getConfiguration().get("password");
            context.put(DEPLOYMENT_PREFIX + PASSWORD, password);
        }
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", selectedServerId);

        // If this task hasn't been configured using a File Spec, convert the configuration to use a File Spec.
        String specSource = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + SPEC_SOURCE_CHOICE);
        if (StringUtils.isBlank(specSource)) {
            String spec = createSpecFromLegacyConfig(taskDefinition);
            context.put(DEPLOYMENT_PREFIX + SPEC_SOURCE_JOB_CONFIGURATION, spec);
        }
    }

    private String createSpecFromLegacyConfig(@NotNull TaskDefinition taskDefinition) {
        String selectedRepoKey = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + LEGACY_DEPLOYMENT_REPOSITORY);
        if (StringUtils.isBlank(selectedRepoKey)) {
            // Compatibility with 1.8.0
            selectedRepoKey = taskDefinition.getConfiguration().get("deploymentRepository");
        }
        if (StringUtils.isBlank(selectedRepoKey)) {
            // If repo is not configured, the task is already converted or not legacy
            return "";
        }
        return LegacyDeploymentUtils.buildDeploymentSpec(selectedRepoKey);
    }

    @Override
    protected String getKey() {
        return DEPLOYMENT_PREFIX;
    }

    @Override
    public boolean taskProducesTestResults(TaskDefinition taskDefinition) {
        return false;
    }

    // shorter way of putting key and its corresponded val in the context
    private void contextPut(Map<String, Object> context, String key, String value) {
        context.put(DEPLOYMENT_PREFIX + key, value);
    }

    // shorter way of putting empty value to a key
    private void contextPutEmpty(Map<String, Object> context, String key) {
        context.put(DEPLOYMENT_PREFIX + key, "");
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition) {
        final Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(taskConfigMap, params, getFieldsToCopy());
        decryptFields(taskConfigMap);
        return taskConfigMap;
    }
}
