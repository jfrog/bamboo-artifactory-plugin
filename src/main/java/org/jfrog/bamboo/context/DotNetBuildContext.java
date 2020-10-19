package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.jfrog.bamboo.configuration.AbstractDotNetBuildConfiguration;

import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 12/10/2020.
 */
public class DotNetBuildContext extends PackageManagersContext {
    public static final String PREFIX = "artifactory.task.dotnet.";
    public static final String COMMAND_CHOICE = PREFIX + "command.choice";
    public static final String ARGUMENTS = PREFIX + "arguments";
    private static final String WORKING_SUBDIRECTORY = PREFIX + "workingSubdirectory";
    public static final String PUSH_PATTERN = PREFIX + "pushPattern";
    public static final String PUSH_TARGET = PREFIX + "pushTarget";
    public static final String DOTNET_EXECUTABLE = PREFIX + EXECUTABLE;
    public static final String RESOLVER_SERVER_ID = PREFIX + RESOLUTION_SERVER_ID_PARAM;
    public static final String RESOLUTION_REPO = PREFIX + RESOLUTION_REPO_PARAM;
    private static final String RESOLVER_USERNAME = PREFIX + RESOLVER_USERNAME_PARAM;
    private static final String RESOLVER_PASSWORD = PREFIX + RESOLVER_PASSWORD_PARAM;
    public static final String DEPLOYER_SERVER_ID = PREFIX + SERVER_ID_PARAM;
    public static final String PUBLISHING_REPO = PREFIX + PUBLISHING_REPO_PARAM;
    private static final String DEPLOYER_USERNAME = PREFIX + DEPLOYER_USERNAME_PARAM;
    private static final String DEPLOYER_PASSWORD = PREFIX + DEPLOYER_PASSWORD_PARAM;
    public static final String PUSH_PATTERN_DEFAULT_VALUE = "*.nupkg";

    public DotNetBuildContext(Map<String, String> env) {
        super(PREFIX, env);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(COMMAND_CHOICE, ARGUMENTS, WORKING_SUBDIRECTORY, DOTNET_EXECUTABLE, ARGUMENTS,
                RESOLVER_SERVER_ID, RESOLUTION_REPO, RESOLVER_USERNAME, RESOLVER_PASSWORD, DEPLOYER_SERVER_ID, PUSH_PATTERN,
                PUSH_TARGET, PUBLISHING_REPO, DEPLOYER_USERNAME, DEPLOYER_PASSWORD, INCLUDE_ENV_VARS_PARAM,
                ENV_VARS_EXCLUDE_PATTERNS, ENV_VARS_INCLUDE_PATTERNS, CAPTURE_BUILD_INFO, BUILD_NAME, BUILD_NUMBER,
                RESOLVER_OVERRIDE_CREDENTIALS_CHOICE, RESOLVER_SHARED_CREDENTIALS, DEPLOYER_OVERRIDE_CREDENTIALS_CHOICE,
                DEPLOYER_SHARED_CREDENTIALS);
    }

    public String getArguments() {
        return env.get(ARGUMENTS);
    }

    public String getWorkingSubdirectory() {
        return env.get(WORKING_SUBDIRECTORY);
    }

    public String getPushPattern() {
        return env.get(PUSH_PATTERN);
    }

    public String getPushTarget() {
        return env.get(PUSH_TARGET);
    }

    public boolean isRestoreCommand() {
        return (AbstractDotNetBuildConfiguration.CFG_COMMAND_RESTORE.equals(env.get(COMMAND_CHOICE)));
    }
}
