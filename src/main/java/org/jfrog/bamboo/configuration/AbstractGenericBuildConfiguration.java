package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.util.TaskConfigurationValidations;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GenericContext;

import java.util.Map;
import java.util.Set;

/**
 * @author Alexei Vainshtein
 */
public class AbstractGenericBuildConfiguration extends AbstractArtifactoryConfiguration {
    static final Set<String> FIELDS_TO_COPY = GenericContext.getFieldsToCopy();

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateLegacyContextForCreate(context);
        context.put("build", context.get("plan"));
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put("selectedRepoKey", "");
        context.put(GenericContext.ENV_VARS_EXCLUDE_PATTERNS, AbstractBuildContext.ENV_VARS_TO_EXCLUDE);
        context.put(GenericContext.SIGN_METHOD_MAP_KEY, GenericContext.SIGN_METHOD_MAP);
        context.put(GenericContext.BUILD_NAME, AbstractBuildContext.DEFAULT_BUILD_NAME);
        context.put(GenericContext.BUILD_NUMBER, AbstractBuildContext.DEFAULT_BUILD_NUMBER);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        String envVarsExcludePatterns = (String)context.get(GenericContext.ENV_VARS_EXCLUDE_PATTERNS);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", context.get(GenericContext.PREFIX + GenericContext.SERVER_ID_PARAM));
        if (envVarsExcludePatterns == null) {
            context.put(GenericContext.ENV_VARS_EXCLUDE_PATTERNS, AbstractBuildContext.ENV_VARS_TO_EXCLUDE);
        }
        String buildName = (String)context.get(GenericContext.BUILD_NAME);
        if (buildName == null) {
            context.put(GenericContext.BUILD_NAME, AbstractBuildContext.DEFAULT_BUILD_NAME);
        }
        String buildNumber = (String)context.get(GenericContext.BUILD_NUMBER);
        if (buildNumber == null) {
            context.put(GenericContext.BUILD_NUMBER, AbstractBuildContext.DEFAULT_BUILD_NUMBER);
        }
    }

    @Override
    protected String getKey() {
        return null;
    }

    @Override
    public boolean taskProducesTestResults(@NotNull TaskDefinition taskDefinition) {
        return false;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
                                                     @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> configMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(configMap, params, FIELDS_TO_COPY);
        decryptFields(configMap);
        return configMap;
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        // Validate build name and number.
        TaskConfigurationValidations.validateCaptureBuildInfoParams(GenericContext.BUILD_NAME, GenericContext.BUILD_NUMBER, AbstractBuildContext.CAPTURE_BUILD_INFO, params, errorCollection);
    }
}
