package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author Alexei Vainshtein
 */
public class PublishBuildInfoContext extends AbstractBuildContext {
    public static final String PREFIX = "artifactory.task.publishBuildInfo.";
    public static final String SERVER_ID_PARAM = PREFIX + "artifactoryServerId";
    public static final String USERNAME = PREFIX + "username";
    public static final String PASSWORD = PREFIX + "password";

    public PublishBuildInfoContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD, DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS);
    }

    @Override
    public String getDeployerUsername() {
        return env.get(USERNAME);
    }

    @Override
    public String getDeployerPassword() {
        return env.get(PASSWORD);
    }
}
