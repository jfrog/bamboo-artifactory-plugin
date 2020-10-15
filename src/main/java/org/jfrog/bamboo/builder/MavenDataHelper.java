package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.ProxyUtils;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Helper to populate Maven specific properties to the client configuration
 *
 * @author Tomer Cohen
 */
public class MavenDataHelper extends MavenAndIvyBuildInfoDataHelperBase {
    private String resolverUsername;
    private String resolverPassword;
    private String resolverUrl;

    public MavenDataHelper(BuildParamsOverrideManager buildParamsOverrideManager, TaskContext context,
                           PackageManagersContext buildContext, EnvironmentVariableAccessor envVarAccessor,
                           String artifactoryPluginVersion, boolean aggregateBuildInfo) {
        super(buildParamsOverrideManager, context, buildContext, envVarAccessor, artifactoryPluginVersion, aggregateBuildInfo);
        long selectedServerId = buildContext.getArtifactoryServerId();
        if (selectedServerId == -1) {
            // No deployment server configured, configure resolution server if needed.
            selectedServerId = buildContext.getResolutionArtifactoryServerId();
            if (selectedServerId != -1 && isServerConfigured(selectedServerId)) {
                setClientData(context, buildContext, clientConf, selectedServerConfig, envVarAccessor.getEnvironment(context));
            }
        }
    }

    @Override
    protected void setClientData(TaskContext taskContext, PackageManagersContext builder, ArtifactoryClientConfiguration clientConf,
                                 ServerConfig serverConfig, Map<String, String> environment) {
        Maven3BuildContext buildContext = (Maven3BuildContext) builder;
        clientConf.publisher.setRecordAllDependencies(buildContext.isRecordAllDependencies());
        String resolutionRepo = overrideParam(buildContext.getResolutionRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        if (isResolutionConfigured(buildContext, resolutionRepo)) {
            long serverId = buildContext.getResolutionArtifactoryServerId();
            ServerConfig resolutionServerConfig = serverConfigManager.getServerConfigById(serverId);
            if (resolutionServerConfig != null) {
                setResolverProperties(taskContext, buildContext, resolutionServerConfig);
                clientConf.resolver.setContextUrl(resolverUrl);
                clientConf.resolver.setRepoKey(resolutionRepo);
                clientConf.resolver.setUsername(resolverUsername);
                // Add proxy configurations.
                ProxyUtils.setProxyConfigurationToArtifactoryClientConfig(resolverUrl, clientConf);
            }
        }
    }

    public void addPasswordsSystemProps(List<String> command, PackageManagersContext builder, @NotNull TaskContext context) {
        Maven3BuildContext buildContext = (Maven3BuildContext) builder;
        super.addPasswordsSystemProps(command, buildContext, context);
        if (resolverPassword == null) {
            return;
        }

        // Set resolution data.
        clientConf.resolver.setPassword(resolverPassword);
        command.add("-D" + clientConf.resolver.getPrefix() + "password=" + clientConf.resolver.getPassword());
        // Adding the passwords as a variable with key that contains the word "password" will mask every instance of the password in bamboo logs.
        context.getBuildContext().getVariableContext().addLocalVariable("artifactory.password.mask.b", clientConf.resolver.getPassword());
    }

    private boolean isResolutionConfigured(Maven3BuildContext buildContext, String resolutionRepo) {
        return buildContext.isResolveFromArtifactory() &&
                StringUtils.isNotBlank(resolutionRepo) &&
                !PackageManagersContext.NO_RESOLUTION_REPO_KEY_CONFIGURED.equals(resolutionRepo) &&
                serverConfigManager.getServerConfigById(buildContext.getResolutionArtifactoryServerId()) != null;
    }

    private void setResolverProperties(TaskContext taskContext, Maven3BuildContext buildContext, ServerConfig resolutionServerConfig) {
        Map<String, String> runtimeContext = taskContext.getRuntimeTaskContext();
        Log buildInfoLog = new BuildInfoLog(log, taskContext.getBuildLogger());

        // Override username and password if needed
        ServerConfig overriderServerConfig = TaskUtils.getResolutionServerConfig(
                buildContext.getOverriddenUsername(runtimeContext, buildInfoLog, false),
                buildContext.getOverriddenPassword(runtimeContext, buildInfoLog, false),
                serverConfigManager, selectedServerConfig, buildParamsOverrideManager);
        resolverUrl = resolutionServerConfig.getUrl();
        resolverUsername = overriderServerConfig.getUsername();
        resolverPassword = overriderServerConfig.getPassword();
    }

    public ServerConfig getResolveServer() {
        if (resolverUrl == null) {
            return null;
        }
        return new ServerConfig(selectedServerConfig.getId(), resolverUrl, resolverUsername, resolverPassword, selectedServerConfig.getTimeout());
    }
}

