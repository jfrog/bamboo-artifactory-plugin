package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.configuration.util.TaskConfigurationValidations;
import org.jfrog.bamboo.context.ArtifactoryBuildContext;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.context.PackageManagersContext;

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
        context.put(GenericContext.ENV_VARS_EXCLUDE_PATTERNS, PackageManagersContext.ENV_VARS_TO_EXCLUDE);
        context.put(GenericContext.SIGN_METHOD_MAP_KEY, GenericContext.SIGN_METHOD_MAP);
        context.put(ArtifactoryBuildContext.BUILD_NAME, ArtifactoryBuildContext.DEFAULT_BUILD_NAME);
        context.put(ArtifactoryBuildContext.BUILD_NUMBER, ArtifactoryBuildContext.DEFAULT_BUILD_NUMBER);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);
        populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        String envVarsExcludePatterns = (String)context.get(GenericContext.ENV_VARS_EXCLUDE_PATTERNS);
        if (envVarsExcludePatterns == null) {
            context.put(GenericContext.ENV_VARS_EXCLUDE_PATTERNS, PackageManagersContext.ENV_VARS_TO_EXCLUDE);
        }
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", context.get(GenericContext.PREFIX + GenericContext.SERVER_ID_PARAM));
        populateDefaultBuildNameNumberInBuildContext(context);
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
        TaskConfigurationValidations.validateCaptureBuildInfoParams(ArtifactoryBuildContext.BUILD_NAME, ArtifactoryBuildContext.BUILD_NUMBER, PackageManagersContext.CAPTURE_BUILD_INFO, params, errorCollection);
    }
}
