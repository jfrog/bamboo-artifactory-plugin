package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.task.TaskDefinition;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.GenericContext;

import java.util.Map;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGenericDeployTask}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGenericBuildConfiguration extends AbstractGenericBuildConfiguration {
    static final String KEY = "artifactoryGenericBuilder";

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateLegacyContextForCreate(context);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);
        String selectedPublishingRepoKey = context.get(GenericContext.REPO_KEY) != null ? context.get(GenericContext.REPO_KEY).toString() : null;
        if (StringUtils.isBlank(selectedPublishingRepoKey)) {
            // Compatibility with 1.8.0
            selectedPublishingRepoKey = taskDefinition.getConfiguration().get("artifactory.generic.deployableRepo");
        }
        context.put("selectedRepoKey", selectedPublishingRepoKey);
        context.put(GenericContext.SIGN_METHOD_MAP_KEY, GenericContext.SIGN_METHOD_MAP);
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    protected String getDeployableRepoKey() {
        return "deployableRepo";
    }
}
