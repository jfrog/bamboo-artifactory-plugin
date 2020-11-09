package org.jfrog.bamboo.release.provider;

import com.atlassian.bamboo.security.SecureTokenService;
import com.atlassian.bamboo.serialization.WhitelistedSerializable;
import com.atlassian.bamboo.task.RuntimeTaskDataProvider;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.runtime.RuntimeTaskDefinition;
import com.atlassian.bamboo.v2.build.CommonContext;
import com.atlassian.bamboo.v2.build.agent.messages.AuthenticableMessage;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.security.provider.SharedCredentialsDataProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides extra data from the server to the agents.
 * Currently only the security token is passed for remote agents to be able to copy the maven build-info.json
 * and gradle gradle.properties files so the release management will be able to read them.
 *
 * @author Shay Yaakov
 */
public class TokenDataProvider extends SharedCredentialsDataProvider implements RuntimeTaskDataProvider {
    public static final String SECURITY_TOKEN = "securityToken";

    private SecureTokenService secureTokenService;

    @NotNull
    @Override
    public Map<String, String> populateRuntimeTaskData(@NotNull TaskDefinition taskDefinition, @NotNull CommonContext commonContext) {
        final Map<String, String> result = super.populateRuntimeTaskData(taskDefinition, commonContext);
        result.put(SECURITY_TOKEN, secureTokenService.generate(AuthenticableMessage.Identification.forResultKey(commonContext.getResultKey())).getToken());

        /*
         * Looks like there is a bug when trying to fetch the above property from the taskContext.getRuntimeTaskContext() in the actual task.
         * This method returns null since it checks for equality with the TaskDefinition but the context map in RuntimeTaskContextImpl
         * saves keys by TaskDefinition and this object is not equal since it has extra configuration parameters (see TaskDefinitionImpl
         * it's hashCode() and equals() checks for the configuration map as well so the objects are never the same).
         * As a workaround, I'm saving the token on the task definition configuration map and grabs it from there.
         */
        taskDefinition.getConfiguration().put(SECURITY_TOKEN, secureTokenService.generate(AuthenticableMessage.Identification.forResultKey(commonContext.getResultKey())).getToken());
        return result;
    }

    @NotNull
    @Override
    public Map<String, WhitelistedSerializable> createRuntimeTaskData(@NotNull RuntimeTaskDefinition runtimeTaskDefinition, @NotNull CommonContext commonContext) {
        return new HashMap<>();
    }

    @Override
    public void processRuntimeTaskData(@NotNull RuntimeTaskDefinition runtimeTaskDefinition, @NotNull CommonContext commonContext) {
        secureTokenService.invalidate(commonContext.getResultKey());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSecureTokenService(final SecureTokenService secureTokenService) {
        this.secureTokenService = secureTokenService;
    }
}
