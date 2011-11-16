package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.spring.container.ComponentNotFoundException;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.context.IvyBuildContext;
import org.jfrog.bamboo.util.ConstantValues;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGenericTask}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGenericBuildConfiguration extends AbstractTaskConfigurator {
    private static final Logger log = Logger.getLogger(ArtifactoryGenericBuildConfiguration.class);
    private static final Set<String> FIELDS_TO_COPY = GenericContext.getFieldsToCopy();
    protected transient ServerConfigManager serverConfigManager;


    protected ArtifactoryGenericBuildConfiguration() {
        try {
            if (ContainerManager.isContainerSetup()) {
                serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                        ConstantValues.ARTIFACTORY_SERVER_CONFIG_MODULE_KEY);
            }
        } catch (ComponentNotFoundException cnfe) {
            System.out.println(ArtifactoryGenericBuildConfiguration.class.getName() + " - " + new Date() +
                    " - Warning: could not find component 'Artifactory Server Configuration Manager' (Can be ignored " +
                    "when running on a remote agent).");
        }
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("build", context.get("plan"));
        context.put("dummyList", Lists.newArrayList());
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", -1);
        context.put("selectedRepoKey", "");
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        String publishingKey = GenericContext.REPO_KEY;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                IvyBuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        context.put("selectedServerId", context.get(GenericContext.SELECTED_SERVER_ID));
        context.put("serverConfigManager", serverConfigManager);
    }

    @Override
    public void populateContextForView(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
        context.put("serverConfigManager", serverConfigManager);
        IvyBuildContext buildContext = IvyBuildContext.createContextFromMap(context);
        long serverId = buildContext.getArtifactoryServerId();
        context.put("selectedServerId", serverId);
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params,
            @Nullable TaskDefinition previousTaskDefinition) {
        Map<String, String> configMap = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(configMap, params, FIELDS_TO_COPY);
        return configMap;
    }
}
