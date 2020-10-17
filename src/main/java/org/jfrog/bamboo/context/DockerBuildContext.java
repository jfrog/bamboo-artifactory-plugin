package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.jfrog.bamboo.configuration.ArtifactoryDockerConfiguration;

import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 09/10/2020.
 */
public class DockerBuildContext extends PackageManagersContext {
    public static final String PREFIX = "artifactory.task.docker.";
    public static final String COMMAND_CHOICE = PREFIX + "command.choice";
    private static final String COMMAND_PULL = PREFIX + "command.pull";
    private static final String COMMAND_PUSH = PREFIX + "command.push";
    public static final String IMAGE_NAME = PREFIX + "imageName";
    private static final String HOST = PREFIX + "host";
    public static final String DOCKER_PUSH_SERVER_ID = PREFIX + SERVER_ID_PARAM;
    public static final String DOCKER_PULL_SERVER_ID = PREFIX + RESOLUTION_SERVER_ID_PARAM;
    public static final String DOCKER_PULL_REPO = PREFIX + RESOLUTION_REPO_PARAM;
    public static final String DOCKER_PUSH_REPO = PREFIX + PUBLISHING_REPO_PARAM;
    public static final String DOCKER_PULL_USERNAME = PREFIX + RESOLVER_USERNAME_PARAM;
    public static final String DOCKER_PULL_PASSWORD = PREFIX + RESOLVER_PASSWORD_PARAM;
    public static final String DOCKER_PUSH_USERNAME = PREFIX + DEPLOYER_USERNAME_PARAM;
    public static final String DOCKER_PUSH_PASSWORD = PREFIX + DEPLOYER_PASSWORD_PARAM;

    public DockerBuildContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    /**
     * @return Get a set of all the fields to copy while populating the build context for a Npm build.
     */
    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(COMMAND_CHOICE, COMMAND_PULL, COMMAND_PUSH, DOCKER_PUSH_SERVER_ID, DOCKER_PULL_SERVER_ID,
                DOCKER_PULL_REPO, DOCKER_PUSH_REPO, DOCKER_PULL_USERNAME, DOCKER_PULL_PASSWORD, DOCKER_PUSH_USERNAME,
                HOST, IMAGE_NAME, DOCKER_PUSH_PASSWORD, INCLUDE_ENV_VARS_PARAM, ENV_VARS_EXCLUDE_PATTERNS,
                ENV_VARS_INCLUDE_PATTERNS, CAPTURE_BUILD_INFO, BUILD_NAME, BUILD_NUMBER, RESOLVER_OVERRIDE_CREDENTIALS_CHOICE,
                RESOLVER_SHARED_CREDENTIALS, DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS);
    }

    public boolean isDockerCommandPull() {
        return (ArtifactoryDockerConfiguration.CFG_DOCKER_COMMAND_PULL.equals(env.get(COMMAND_CHOICE)));
    }

    public String getImageName() {
        return env.get(IMAGE_NAME);
    }

    public String getHost() {
        return env.get(HOST);
    }

}
