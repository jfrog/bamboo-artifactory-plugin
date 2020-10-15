package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.task.TaskDefinition;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.context.IvyBuildContext;

import java.util.Map;

import static org.jfrog.bamboo.context.ArtifactoryBuildContext.RESOLVER_OVERRIDE_CREDENTIALS_CHOICE;

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

        // Backward compatibility for tasks with overridden username and password
        Map<String, String> taskConfiguration = taskDefinition.getConfiguration();
        GenericContext taskContext = new GenericContext(taskConfiguration);
        if (StringUtils.isBlank(taskContext.getResolverOverrideCredentialsChoice()) &&
                StringUtils.isNoneBlank(taskContext.getUsername(), taskContext.getPassword())) {
            context.put(RESOLVER_OVERRIDE_CREDENTIALS_CHOICE, CVG_CRED_USERNAME_PASSWORD);
        }
    }

    @Override
    protected String getKey() {
        return "";
    }
}
