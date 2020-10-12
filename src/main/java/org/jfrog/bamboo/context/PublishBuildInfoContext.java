package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author Alexei Vainshtein
 */
public class PublishBuildInfoContext extends ArtifactoryBuildContext {
    public static final String PREFIX = "artifactory.task.publishBuildInfo.";
    public static final String SERVER_ID_PARAM = PREFIX + "artifactoryServerId";
    public static final String USERNAME = PREFIX + USERNAME_PARAM;
    public static final String PASSWORD = PREFIX + PASSWORD_PARAM;

    public PublishBuildInfoContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD, BUILD_NAME, BUILD_NUMBER,
                DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS);
    }
}
