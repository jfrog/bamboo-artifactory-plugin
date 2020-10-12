package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 30/05/2018.
 */
public class XrayScanContext extends ArtifactoryBuildContext {
    public static final String PREFIX = "artifactory.xrayScan.";
    public static final String SERVER_ID_PARAM = PREFIX + "artifactoryServerId";
    public static final String FAIL_IF_VULNERABLE = PREFIX + "failIfVulnerable";
    public static final String USERNAME = PREFIX + USERNAME_PARAM;
    public static final String PASSWORD = PREFIX + PASSWORD_PARAM;

    public XrayScanContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD, FAIL_IF_VULNERABLE, BUILD_NAME, BUILD_NUMBER,
                DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS);
    }

    public boolean isFailIfVulnerable() {
        return Boolean.parseBoolean(env.get(FAIL_IF_VULNERABLE));
    }
}
