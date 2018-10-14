package org.jfrog.bamboo.util.deployment;

import com.atlassian.bamboo.task.CommonTaskContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.configuration.ArtifactoryDeploymentUploadConfiguration;

/**
 * Created by Dima Nevelev on 18/09/2018.
 */
public class LegacyDeploymentUtils {

    public static String buildDeploymentSpec(@NotNull CommonTaskContext deploymentTaskContext) {
        String repo = getRepoKey(deploymentTaskContext);
        if (StringUtils.isBlank(repo)) {
            // If repo is not configured, no conversion is needed.
            return "";
        }
        return buildDeploymentSpec(repo);
    }

    public static String buildDeploymentSpec(String repo) {
        return "{\n" +
                "  \"files\": [\n" +
                "    {\n" +
                "      \"pattern\": \"*\",\n" +
                "      \"target\": \"" + repo + "\",\n" +
                "      \"flat\": \"false\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private static String getRepoKey(@NotNull CommonTaskContext commonTaskContext) {
        String repositoryKey = commonTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentUploadConfiguration.LEGACY_DEPLOYMENT_REPOSITORY);
        if (StringUtils.isBlank(repositoryKey)) {
            // Compatibility with version 1.8.0
            repositoryKey = commonTaskContext.getConfigurationMap().get(ArtifactoryDeploymentUploadConfiguration.LEGACY_DEPLOYMENT_REPOSITORY);
        }
        return repositoryKey;
    }
}
