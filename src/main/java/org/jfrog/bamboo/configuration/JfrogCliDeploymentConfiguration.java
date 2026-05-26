package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.JfrogCliDeploymentContext;

import java.util.Map;

/**
 * Task configurator for the JFrog CLI Deployment task.
 * Populates the UI context with the configured Artifactory servers and the saved jf command.
 */
public class JfrogCliDeploymentConfiguration extends AbstractArtifactoryConfiguration {

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put(JfrogCliDeploymentContext.JF_COMMAND, "");
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context,
                                       @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, JfrogCliDeploymentContext.getFieldsToCopy());

        String selectedServerId = taskDefinition.getConfiguration().get(JfrogCliDeploymentContext.SERVER_ID);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", StringUtils.defaultIfBlank(selectedServerId, "-1"));
        context.put(JfrogCliDeploymentContext.JF_COMMAND,
                StringUtils.defaultString(taskDefinition.getConfiguration().get(JfrogCliDeploymentContext.JF_COMMAND)));
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
                                                     @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> taskConfigMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(
                taskConfigMap, params, JfrogCliDeploymentContext.getFieldsToCopy());
        return taskConfigMap;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition taskDefinition) {
        return false;
    }

    @Override
    protected String getKey() {
        return JfrogCliDeploymentContext.PREFIX;
    }
}
