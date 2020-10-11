package org.jfrog.bamboo.configuration.util;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.release.vcs.VcsTypes;
import org.jfrog.bamboo.release.vcs.git.GitAuthenticationType;

/**
 * Created by Bar Belity on 10/03/2020.
 */
public class TaskConfigurationValidations {

    /**
     * Validate that an executable is provided.
     */
    public static void validateExecutable(String executableKey, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        if (StringUtils.isBlank(params.getString(executableKey))) {
            errorCollection.addError(executableKey, "Please specify an Executable.");
        }
    }

    /**
     * Validate that a build-jdk is provided.
     */
    public static void validateJdk(String buildJdkKey, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        if (StringUtils.isBlank(params.getString(buildJdkKey))) {
            errorCollection.addError(buildJdkKey, "Please specify Build JDK.");
        }
    }

    /**
     * Validate that an Artifactory server is provided, and that it is valid.
     */
    public static void validateArtifactoryServerProvidedAndValid(String serverKey, ServerConfigManager serverConfigManager, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        if (StringUtils.isBlank(params.getString(serverKey))) {
            errorCollection.addError(serverKey, "Please specify a Server.");
            return;
        }
        validateServer(serverKey, serverConfigManager, params, errorCollection);
    }

    /**
     * Validate that the provided server ID and the corresponding repository are valid.
     */
    public static void validateArtifactoryServerAndRepo(String serverKey, String repositoryKey, ServerConfigManager serverConfigManager, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        boolean shouldCheckRepo = validateServer(serverKey, serverConfigManager, params, errorCollection);
        if (shouldCheckRepo) {
            // If reached here, meaning that an Artifactory server was provided,
            // thus a repository name is also required.
            validateRepo(repositoryKey, params, errorCollection);
        }
    }

    /**
     * Validate that the provided server ID is valid.
     * Return if should continue checking repo.
     */
    private static boolean validateServer(String serverKey, ServerConfigManager serverConfigManager, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        if (!params.containsKey(serverKey)) {
            return false;
        }
        long configuredServerId = getConfiguredServerId(serverKey, params);
        if (configuredServerId == -1) {
            return false;
        }
        validateServerConfigExists(configuredServerId, serverKey, serverConfigManager, errorCollection);
        return true;
    }

    private static void validateRepo(String repositoryKey, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        if (StringUtils.isBlank(params.getString(repositoryKey))) {
            errorCollection.addError(repositoryKey, "Please choose a repository.");
        }
    }

    private static void validateServerConfigExists(long configuredServerId, String serverKey, ServerConfigManager serverConfigManager, @NotNull ErrorCollection errorCollection) {
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(configuredServerId);
        if (serverConfig == null) {
            errorCollection.addError(serverKey,
                    "Could not find Artifactory server configuration by the ID " + configuredServerId);
        }
    }

    private static long getConfiguredServerId(String serverKey, @NotNull ActionParametersMap params) {
        try {
            return params.getLong(serverKey, -1);
        } catch (ConversionException ce) {
            return -1;
        }
    }

    /**
     * Validate that the release-management configurations are provided.
     */
    public static void validateReleaseManagement(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        String enableReleaseManagementKey = PackageManagersContext.ENABLE_RELEASE_MANAGEMENT;
        if (!params.getBoolean(enableReleaseManagementKey)) {
            return;
        }
        String vcsTypeKey = PackageManagersContext.VCS_PREFIX + PackageManagersContext.VCS_TYPE;
        if (VcsTypes.GIT.toString().equals(params.getString(vcsTypeKey))) {
            String gitUrlKey = PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_URL;
            if (!params.containsKey(gitUrlKey) || StringUtils.isBlank(params.getString(gitUrlKey))) {
                errorCollection.addError(gitUrlKey, "Please specify Git URL.");
            }

            String gitAuthenticationKey = PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_AUTHENTICATION_TYPE;
            if (GitAuthenticationType.PASSWORD.toString().equals(params.getString(gitAuthenticationKey))) {
                String gitUsernameKey = PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_USERNAME;
                if (!params.containsKey(gitUsernameKey) || StringUtils.isBlank(params.getString(gitUsernameKey))) {
                    errorCollection.addError(gitUsernameKey, "Please specify Username.");
                }

                String gitPasswordKey = PackageManagersContext.VCS_PREFIX + PackageManagersContext.GIT_PASSWORD;
                if (!params.containsKey(gitPasswordKey) || StringUtils.isBlank(params.getString(gitPasswordKey))) {
                    errorCollection.addError(gitUsernameKey, "Please specify Password.");
                }
            }

            return;
        }

        if (VcsTypes.PERFORCE.toString().equals(params.getString(vcsTypeKey))) {
            String perforcePort = PackageManagersContext.VCS_PREFIX + PackageManagersContext.PERFORCE_PORT;
            if (!params.containsKey(perforcePort) || StringUtils.isBlank(params.getString(perforcePort))) {
                errorCollection.addError(perforcePort, "Please specify Port.");
            }

            String perforceClient = PackageManagersContext.VCS_PREFIX + PackageManagersContext.PERFORCE_CLIENT;
            if (!params.containsKey(perforceClient) || StringUtils.isBlank(params.getString(perforceClient))) {
                errorCollection.addError(perforceClient, "Please specify Client (workspace).");
            }

            String perforceDepot = PackageManagersContext.VCS_PREFIX + PackageManagersContext.PERFORCE_DEPOT;
            if (!params.containsKey(perforceDepot) || StringUtils.isBlank(params.getString(perforceDepot))) {
                errorCollection.addError(perforceDepot, "Please specify Depot view.");
            }
        }
    }

    public static void validateCaptureBuildInfoParams(String buildNameKey, String buildNumberKey, String captureBuildInfoKey, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        if (params.getBoolean(captureBuildInfoKey)) {
            validateBuildNameNumber(buildNameKey, buildNumberKey, params, errorCollection);
        }
    }

    public static void validateBuildNameNumber(String buildNameKey, String buildNumberKey, @NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        if (StringUtils.isBlank(params.getString(buildNameKey))) {
            errorCollection.addError(buildNameKey, "Please specify Build Name.");
        }

        if (StringUtils.isBlank(params.getString(buildNumberKey))) {
            errorCollection.addError(buildNumberKey, "Please specify Build Number.");
        }
    }
}
