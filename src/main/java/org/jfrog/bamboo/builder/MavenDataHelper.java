package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
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
                           AbstractBuildContext buildContext, EnvironmentVariableAccessor envVarAccessor,
                           String artifactoryPluginVersion) {
        super(buildParamsOverrideManager, context, buildContext, envVarAccessor, artifactoryPluginVersion);
        long selectedServerId = buildContext.getArtifactoryServerId();
        if (selectedServerId == -1) {
            // No deployment server configured, configure resolution server if needed.
            selectedServerId = ((Maven3BuildContext) buildContext).getResolutionArtifactoryServerId();
            if (selectedServerId != -1 && isServerConfigured(context, selectedServerId)) {
                setClientData(buildContext, clientConf, selectedServerConfig, envVarAccessor.getEnvironment(context));
            }
        }
    }

    @Override
    protected void setClientData(AbstractBuildContext builder, ArtifactoryClientConfiguration clientConf,
                                 ServerConfig serverConfig, Map<String, String> environment) {
        Maven3BuildContext buildContext = (Maven3BuildContext) builder;
        clientConf.publisher.setRecordAllDependencies(buildContext.isRecordAllDependencies());
        String resolutionRepo = overrideParam(buildContext.getResolutionRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        if (isResolutionConfigured(buildContext, resolutionRepo)) {
            long serverId = buildContext.getResolutionArtifactoryServerId();
            ServerConfig resolutionServerConfig = serverConfigManager.getServerConfigById(serverId);
            if (resolutionServerConfig != null) {
                setResolverProperties(buildContext, resolutionServerConfig);
                clientConf.resolver.setContextUrl(resolverUrl);
                clientConf.resolver.setRepoKey(resolutionRepo);
                clientConf.resolver.setUsername(resolverUsername);
            }
        }
    }

    @NotNull
    public void addPasswordsSystemProps(List<String> command, AbstractBuildContext builder, @NotNull TaskContext context) {
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
                !AbstractBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED.equals(resolutionRepo) &&
                serverConfigManager.getServerConfigById(buildContext.getResolutionArtifactoryServerId()) != null;
    }

    private void setResolverProperties(Maven3BuildContext buildContext, ServerConfig resolutionServerConfig) {
        // Set url.
        resolverUrl = resolutionServerConfig.getUrl();
        // Set username.
        resolverUsername = buildContext.getResolverUserName();
        resolverUsername = overrideParam(resolverUsername, BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME);
        if (StringUtils.isBlank(resolverUsername)) {
            resolverUsername = resolutionServerConfig.getUsername();
        }
        // Set password.
        resolverPassword = buildContext.getResolverPassword();
        resolverPassword = overrideParam(resolverPassword, BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD);
        if (StringUtils.isBlank(resolverPassword)) {
            resolverPassword = resolutionServerConfig.getPassword();
        }
    }

    public ServerConfig getResolveServer() {
        if (resolverUrl == null) {
            return null;
        }
        return new ServerConfig(selectedServerConfig.getId(), resolverUrl, resolverUsername, resolverPassword, selectedServerConfig.getTimeout());
    }
}

