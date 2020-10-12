package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.configuration.ArtifactoryDeploymentUploadConfiguration.*;

/**
 * @author yahavi
 */
public class DeploymentUploadContext extends ArtifactoryBuildContext {

    public static final String USERNAME = DEPLOYMENT_PREFIX + USERNAME_PARAM;
    public static final String PASSWORD = DEPLOYMENT_PREFIX + PASSWORD_PARAM;

    public DeploymentUploadContext(Map<String, String> env) {
        super(DEPLOYMENT_PREFIX, env);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(
                DEPLOYMENT_PREFIX + SERVER_ID_PARAM, USERNAME, PASSWORD,
                DEPLOYMENT_PREFIX + LEGACY_DEPLOYMENT_REPOSITORY,
                DEPLOYMENT_PREFIX + SPEC_SOURCE_CHOICE,
                DEPLOYMENT_PREFIX + SPEC_SOURCE_JOB_CONFIGURATION,
                DEPLOYMENT_PREFIX + SPEC_SOURCE_FILE,
                DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE,
                DEPLOYER_SHARED_CREDENTIALS
        );
    }
}