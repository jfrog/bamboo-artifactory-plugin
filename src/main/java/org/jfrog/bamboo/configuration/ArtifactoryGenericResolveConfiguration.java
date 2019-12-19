package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.task.TaskDefinition;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.context.IvyBuildContext;

import java.util.Map;

/**
 * Configuration for {@link org.jfrog.bamboo.task.ArtifactoryGenericResolveTask}
 *
 * @author Lior Hasson
 */
public class ArtifactoryGenericResolveConfiguration extends AbstractGenericBuildConfiguration {

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        populateLegacyContextForCreate(context);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        populateLegacyContextForEdit(context, taskDefinition);

        String publishingKey = GenericContext.REPO_KEY;
        String selectedPublishingRepoKey = context.get(publishingKey) != null ? context.get(publishingKey).toString() :
                IvyBuildContext.NO_PUBLISHING_REPO_KEY_CONFIGURED;
        context.put("selectedRepoKey", selectedPublishingRepoKey);
    }

    @Override
    protected String getKey() {
        return "";
    }
}
