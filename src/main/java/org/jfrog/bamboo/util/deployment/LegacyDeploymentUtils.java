package org.jfrog.bamboo.util.deployment;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.deployment.ArtifactoryDeploymentConfiguration;

/**
 * Created by Dima Nevelev on 18/09/2018.
 */
public class LegacyDeploymentUtils {

    public static String buildDeploymentSpec(@NotNull DeploymentTaskContext deploymentTaskContext) {
        String repo = getRepoKey(deploymentTaskContext);
        if (StringUtils.isBlank(repo)) {
            // If repo is not configured, the task is not legacy
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

    private static String getRepoKey(@NotNull DeploymentTaskContext deploymentTaskContext) {
        String repositoryKey = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_PREFIX + ArtifactoryDeploymentConfiguration.LEGACY_DEPLOYMENT_REPOSITORY);
        if (StringUtils.isBlank(repositoryKey)) {
            // Compatibility with version 1.8.0
            repositoryKey = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.LEGACY_DEPLOYMENT_REPOSITORY);
        }
        return repositoryKey;
    }
}
