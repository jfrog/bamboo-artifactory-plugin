package org.jfrog.bamboo.deployment;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.AbstractArtifactoryConfiguration;
import org.jfrog.bamboo.context.AbstractBuildContext;

import java.util.Map;
import java.util.Set;

/**
 * Artifactory Deployment task configuration
 *
 * @author Aviad Shikloshi
 */
public class ArtifactoryDeploymentConfiguration extends AbstractArtifactoryConfiguration {

    public static final String DEPLOYMENT_PREFIX = "artifactory.deployment.";
    public static final String DEPLOYMENT_REPOSITORY = "deploymentRepository";
    public static final String PASSWORD = "password";
    public static final String USERNAME = "username";

    private static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(
                DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM,
                DEPLOYMENT_PREFIX + USERNAME,
                DEPLOYMENT_PREFIX + PASSWORD,
                DEPLOYMENT_PREFIX + DEPLOYMENT_REPOSITORY
        );
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put(AbstractBuildContext.SERVER_ID_PARAM, -1);

        contextPutEmpty(context, USERNAME);
        contextPutEmpty(context, PASSWORD);
        contextPutEmpty(context, DEPLOYMENT_REPOSITORY);
        contextPutEmpty(context, AbstractBuildContext.SERVER_ID_PARAM);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, getFieldsToCopy());
        String selectedServerId = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM);
        String selectedRepoKey = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + DEPLOYMENT_REPOSITORY);
        String username = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + USERNAME);
        String password = taskDefinition.getConfiguration().get(DEPLOYMENT_PREFIX + PASSWORD);
        if (StringUtils.isBlank(selectedServerId)) {
            // Compatibility with 1.8.0
            selectedServerId = taskDefinition.getConfiguration().get("artifactoryServerId");
        }
        if (StringUtils.isBlank(selectedRepoKey)) {
            // Compatibility with 1.8.0
            selectedRepoKey = taskDefinition.getConfiguration().get("deploymentRepository");
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
        context.put("selectedRepoKey", selectedRepoKey);
    }

    @Override
    protected String getKey() {
        return DEPLOYMENT_PREFIX;
    }

    @Override
    protected String getDeployableRepoKey() {
        return DEPLOYMENT_REPOSITORY;
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
        context.put(DEPLOYMENT_PREFIX + key, StringUtils.EMPTY);
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
