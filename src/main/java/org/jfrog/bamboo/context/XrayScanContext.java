package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 30/05/2018.
 */
public class XrayScanContext {
    public static final String SERVER_ID_PARAM = "artifactory.xrayScan.artifactoryServerId";
    public static final String FAIL_IF_VULNERABLE = "artifactory.xrayScan.failIfVulnerable";
    public static final String USERNAME = "artifactory.xrayScan.username";
    public static final String PASSWORD = "artifactory.xrayScan.password";
    public static final String BUILD_NAME = "artifactory.task.buildName";
    public static final String BUILD_NUMBER = "artifactory.task.buildNumber";

    private final Map<String, String> env;

    public XrayScanContext(Map<String, String> env) {
        this.env = env;
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD, FAIL_IF_VULNERABLE, BUILD_NAME, BUILD_NUMBER);
    }

    public String getUsername() {
        return env.get(USERNAME);
    }

    public String getPassword() {
        return env.get(PASSWORD);
    }

    public String getArtifactoryServerId() {
        return env.get(SERVER_ID_PARAM);
    }

    public boolean isFailIfVulnerable() {
        return Boolean.parseBoolean(env.get(FAIL_IF_VULNERABLE));
    }

    public String getBuildName() {
        return env.get(BUILD_NAME);
    }

    public String getBuildNumber() {
        return env.get(BUILD_NUMBER);
    }
}
