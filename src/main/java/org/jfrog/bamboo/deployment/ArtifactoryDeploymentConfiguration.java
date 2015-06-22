package org.jfrog.bamboo.deployment;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.AbstractArtifactoryConfiguration;
import org.jfrog.bamboo.context.AbstractBuildContext;

import java.util.Map;

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
    public static final String SELECTED_REPO_KEY = "selectedRepoKey";
    public static final String ARTIFACTORY_SERVER_ID = "artifactoryServerId";

    public ArtifactoryDeploymentConfiguration() {
        super(DEPLOYMENT_PREFIX);
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition) {
        final Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfigMap.put("selectedServerId", params.getString("artifactory.deployment.artifactoryServerId"));
        taskConfigMap.put(AbstractBuildContext.SERVER_ID_PARAM, params.getString(DEPLOYMENT_PREFIX + AbstractBuildContext.SERVER_ID_PARAM));
        taskConfigMap.put(SELECTED_REPO_KEY, params.getString(DEPLOYMENT_PREFIX + DEPLOYMENT_REPOSITORY));
        taskConfigMap.put(USERNAME, params.getString(DEPLOYMENT_PREFIX + USERNAME));
        taskConfigMap.put(PASSWORD, params.getString(DEPLOYMENT_PREFIX + PASSWORD));
        taskConfigMap.put(DEPLOYMENT_REPOSITORY, params.getString(DEPLOYMENT_PREFIX + DEPLOYMENT_REPOSITORY));
        return taskConfigMap;
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
        contextPutEmpty(context, ARTIFACTORY_SERVER_ID);
        context.put("selectedRepoKey", StringUtils.EMPTY);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);

        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedRepoKey", taskDefinition.getConfiguration().get("selectedRepoKey"));
        context.put("selectedServerId", taskDefinition.getConfiguration().get("artifactoryServerId"));

        contextPut(context, taskDefinition, USERNAME);
        contextPut(context, taskDefinition, PASSWORD);
        contextPut(context, taskDefinition, DEPLOYMENT_REPOSITORY);
        contextPut(context, taskDefinition, AbstractBuildContext.SERVER_ID_PARAM);
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        context.put("serverConfigManager", serverConfigManager);
        contextPut(context, taskDefinition, AbstractBuildContext.SERVER_ID_PARAM);
        contextPutEmpty(context, USERNAME);
        contextPutEmpty(context, DEPLOYMENT_REPOSITORY);
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
    protected String getDefaultTestDirectory() {
        return StringUtils.EMPTY;
    }

    @Override
    public boolean taskProducesTestResults(TaskDefinition taskDefinition) {
        return false;
    }

    // shorter way of putting key and its corresponded val in the context
    private void contextPut(Map<String, Object> context, TaskDefinition taskDefinition, String key) {
        context.put(DEPLOYMENT_PREFIX + key, taskDefinition.getConfiguration().get(key));
    }

    // shorter way of putting empty value to a key
    private void contextPutEmpty(Map<String, Object> context, String key) {
        context.put(DEPLOYMENT_PREFIX + key, StringUtils.EMPTY);
    }
}
