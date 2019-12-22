package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.jfrog.bamboo.configuration.ArtifactoryNpmConfiguration;

import java.util.Map;
import java.util.Set;

public class NpmBuildContext extends AbstractBuildContext {
    public static final String PREFIX = "artifactory.task.npm.";
    public static final String COMMAND_CHOICE = PREFIX + "command.choice";
    public static final String NPM_DEPLOYER_SERVER_ID = PREFIX + SERVER_ID_PARAM;
    public static final String NPM_PUBLISHING_REPO = PREFIX + PUBLISHING_REPO_PARAM;
    public static final String NPM_RESOLVER_SERVER_ID = PREFIX + RESOLUTION_SERVER_ID_PARAM;
    public static final String NPM_RESOLUTION_REPO = PREFIX + RESOLUTION_REPO_PARAM;
    private static final String COMMAND_INSTALL = PREFIX + "command.install";
    private static final String COMMAND_PUBLISH = PREFIX + "command.publish";
    private static final String NPM_ARGUMENTS = PREFIX + "install.npmArguments";
    private static final String WORKING_SUBDIRECTORY = PREFIX + "workingSubdirectory";
    private static final String NPM_EXECUTABLE = PREFIX + EXECUTABLE;
    private static final String NPM_ENV = PREFIX + ENVIRONMENT_VARIABLES;
    private static final String NPM_RESOLVER_USERNAME = PREFIX + RESOLVER_USERNAME_PARAM;
    private static final String NPM_RESOLVER_PASSWORD = PREFIX + RESOLVER_PASSWORD_PARAM;
    private static final String NPM_DEPLOYER_USERNAME = PREFIX + DEPLOYER_USERNAME_PARAM;
    private static final String NPM_DEPLOYER_PASSWORD = PREFIX + DEPLOYER_PASSWORD_PARAM;

    public NpmBuildContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    /**
     * @return Get a set of all the fields to copy while populating the build context for a Npm build.
     */
    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(COMMAND_CHOICE, COMMAND_INSTALL, COMMAND_PUBLISH, NPM_ARGUMENTS,
                WORKING_SUBDIRECTORY, NPM_EXECUTABLE, NPM_ENV, NPM_RESOLVER_SERVER_ID, NPM_RESOLUTION_REPO, NPM_RESOLVER_USERNAME,
                NPM_RESOLVER_PASSWORD, NPM_DEPLOYER_SERVER_ID, NPM_PUBLISHING_REPO, NPM_DEPLOYER_USERNAME, NPM_DEPLOYER_PASSWORD,
                INCLUDE_ENV_VARS_PARAM, ENV_VARS_EXCLUDE_PATTERNS, ENV_VARS_INCLUDE_PATTERNS, CAPTURE_BUILD_INFO);
    }

    public String getCommandChoice() {
        return env.get(COMMAND_CHOICE);
    }

    public String getCommandInstall() {
        return env.get(COMMAND_INSTALL);
    }

    public String getCommandPublish() {
        return env.get(COMMAND_PUBLISH);
    }

    public String getArguments() {
        return env.get(NPM_ARGUMENTS);
    }

    public String getWorkingSubdirectory() {
        return env.get(WORKING_SUBDIRECTORY);
    }

    public boolean isNpmCommandInstall() {
        return (ArtifactoryNpmConfiguration.CFG_NPM_COMMAND_INSTALL.equals(env.get(COMMAND_CHOICE)));
    }
}
