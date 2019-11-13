package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author Alexei Vainshtein
 */
public class PublishBuildInfoContext {

    public static final String SERVER_ID_PARAM = "artifactory.task.publishBuildInfo.artifactoryServerId";
    public static final String USERNAME = "artifactory.task.publishBuildInfo.username";
    public static final String PASSWORD = "artifactory.task.publishBuildInfo.password";

    private final Map<String, String> env;

    public PublishBuildInfoContext(Map<String, String> env) {
        this.env = env;
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD);
    }

    public String getUsername() {
        return env.get(USERNAME);
    }

    public String getPassword() {
        return env.get(PASSWORD);
    }

    public long getArtifactoryServerId() {
        return Long.parseLong(env.get(SERVER_ID_PARAM));
    }
}
