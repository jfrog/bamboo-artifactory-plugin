package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.task.TaskDefinition;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.GenericContext;

import java.util.Map;

import static org.jfrog.bamboo.context.ArtifactoryBuildContext.DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE;

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

        // Backward compatibility for tasks with overridden username and password
        Map<String, String> taskConfiguration = taskDefinition.getConfiguration();
        GenericContext taskContext = new GenericContext(taskConfiguration);
        if (StringUtils.isBlank(taskContext.getDeployerOverrideCredentialsChoice()) &&
                StringUtils.isNoneBlank(taskContext.getUsername(), taskContext.getPassword())) {
            context.put(DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, CVG_CRED_USERNAME_PASSWORD);
        }
    }

    @Override
    protected String getKey() {
        return KEY;
    }
}
