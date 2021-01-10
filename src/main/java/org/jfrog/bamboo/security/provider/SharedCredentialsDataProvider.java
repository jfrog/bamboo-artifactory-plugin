package org.jfrog.bamboo.security.provider;

import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.credentials.CredentialsData;
import com.atlassian.bamboo.serialization.WhitelistedSerializable;
import com.atlassian.bamboo.task.RuntimeTaskDataProvider;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.runtime.RuntimeTaskDefinition;
import com.atlassian.bamboo.v2.build.CommonContext;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.jfrog.bamboo.context.ArtifactoryBuildContext.DEPLOYER_SHARED_CREDENTIALS;
import static org.jfrog.bamboo.context.ArtifactoryBuildContext.RESOLVER_SHARED_CREDENTIALS;

/**
 * Provides shared credentials to the running build agent.
 *
 * @author yahavi
 */
public class SharedCredentialsDataProvider implements RuntimeTaskDataProvider {
    public static final String RESOLVER_SHARED_CREDENTIALS_USER = "resolverSharedCredentialsUser";
    public static final String RESOLVER_SHARED_CREDENTIALS_PASSWORD = "resolverSharedCredentialsPassword";
    public static final String DEPLOYER_SHARED_CREDENTIALS_USER = "deployerSharedCredentialsUser";
    public static final String DEPLOYER_SHARED_CREDENTIALS_PASSWORD = "deployerSharedCredentialsPassword";

    private CredentialsAccessor credentialsAccessor;

    @NotNull
    @Override
    public Map<String, String> populateRuntimeTaskData(@NotNull TaskDefinition taskDefinition, @NotNull CommonContext commonContext) {
        Map<String, String> result = Maps.newHashMap();
        populateSharedCredentials(taskDefinition, result, RESOLVER_SHARED_CREDENTIALS, RESOLVER_SHARED_CREDENTIALS_USER, RESOLVER_SHARED_CREDENTIALS_PASSWORD);
        populateSharedCredentials(taskDefinition, result, DEPLOYER_SHARED_CREDENTIALS, DEPLOYER_SHARED_CREDENTIALS_USER, DEPLOYER_SHARED_CREDENTIALS_PASSWORD);
        return result;
    }

    private void populateSharedCredentials(TaskDefinition taskDefinition, Map<String, String> result, String type, String credUser, String credPassword) {
        String credentialsName = taskDefinition.getConfiguration().get(type);
        if (StringUtils.isBlank(credentialsName)) {
            return;
        }

        CredentialsData credentialsData = credentialsAccessor.getCredentialsByName(credentialsName);
        if (credentialsData == null) {
            return;
        }
        Map<String, String> credentialsConfiguration = credentialsData.getConfiguration();
        String username = credentialsConfiguration.get("username");
        String password = credentialsConfiguration.get("password");
        if (username == null || password == null) {
            // If username or password are null, the credentials type is not "Username and password" type
            return;
        }
        result.put(credUser, username);
        result.put(credPassword, password);
        taskDefinition.getConfiguration().put(credUser, username);
        taskDefinition.getConfiguration().put(credPassword, password);
    }

    @NotNull
    @Override
    public Map<String, WhitelistedSerializable> createRuntimeTaskData(@NotNull RuntimeTaskDefinition runtimeTaskDefinition, @NotNull CommonContext commonContext) {
        return new HashMap<>();
    }

    @Override
    public void processRuntimeTaskData(@NotNull RuntimeTaskDefinition runtimeTaskDefinition, @NotNull CommonContext commonContext) {
    }

    public void setCredentialsAccessor(final CredentialsAccessor credentialsAccessor) {
        this.credentialsAccessor = credentialsAccessor;
    }
}