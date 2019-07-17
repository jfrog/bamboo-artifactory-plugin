package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.Map;

import static org.jfrog.bamboo.context.AbstractBuildContext.CAPTURE_BUILD_INFO;

/**
 * @author Alexei Vainshtein
 */
public class PublishBuildInfoContext implements ArtifactoryContextInterface {

    public static final String SERVER_ID_PARAM = "artifactory.artifactoryPublishBuildInfo.artifactoryServerId";
    public static final String USERNAME = "artifactory.artifactoryPublishBuildInfo.username";
    public static final String PASSWORD = "artifactory.artifactoryPublishBuildInfo.password";

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

    @Override
    public boolean isIncludeEnvVars() {
        return false;
    }

    @Override
    public String getRepoKey() {
        return null;
    }

    @Override
    public String getEnvVarsExcludePatterns() {
        return null;
    }

    @Override
    public String getEnvVarsIncludePatterns() {
        return null;
    }

    @Override
    public boolean isCaptureBuildInfo() {
        return Boolean.parseBoolean(env.get(CAPTURE_BUILD_INFO));
    }
}
