package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 30/05/2018.
 */
public class XrayScanContext extends AbstractBuildContext {
    public static final String PREFIX = "artifactory.xrayScan.";
    public static final String SERVER_ID_PARAM = PREFIX + "artifactoryServerId";
    public static final String FAIL_IF_VULNERABLE = PREFIX + "failIfVulnerable";
    public static final String USERNAME = PREFIX + "username";
    public static final String PASSWORD = PREFIX + "password";


    public XrayScanContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SERVER_ID_PARAM, USERNAME, PASSWORD, FAIL_IF_VULNERABLE, DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE, DEPLOYER_SHARED_CREDENTIALS);
    }

    @Override
    public String getDeployerUsername() {
        return env.get(USERNAME);
    }

    @Override
    public String getDeployerPassword() {
        return env.get(PASSWORD);
    }

    public boolean isFailIfVulnerable() {
        return Boolean.parseBoolean(env.get(FAIL_IF_VULNERABLE));
    }
}
