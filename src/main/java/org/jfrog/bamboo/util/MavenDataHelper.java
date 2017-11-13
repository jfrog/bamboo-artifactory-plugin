package org.jfrog.bamboo.util;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.ArtifactoryBuildInfoDataHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.Map;

/**
 * Helper to populate Maven specific properties to the client configuration
 *
 * @author Tomer Cohen
 */
public class MavenDataHelper extends ArtifactoryBuildInfoDataHelper {

    public MavenDataHelper(BuildParamsOverrideManager buildParamsOverrideManager, TaskContext context,
                           AbstractBuildContext buildContext, EnvironmentVariableAccessor envVarAccessor,
                           String artifactoryPluginVersion) {
        super(buildParamsOverrideManager, context, buildContext, envVarAccessor, artifactoryPluginVersion);
        long selectedServerId = buildContext.getArtifactoryServerId();
        if (selectedServerId == -1) {
            selectedServerId = ((Maven3BuildContext) buildContext).getResolutionArtifactoryServerId();
            if (selectedServerId != -1 && isServerConfigured(context, selectedServerId)) {
                setClientData(buildContext, clientConf, serverConfig, envVarAccessor.getEnvironment(context));
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
                clientConf.resolver.setContextUrl(resolutionServerConfig.getUrl());
                clientConf.resolver.setRepoKey(resolutionRepo);
                String username = buildContext.getResolverUserName();
                username = overrideParam(username, BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME);
                if (StringUtils.isBlank(username)) {
                    username = resolutionServerConfig.getUsername();
                }
                clientConf.resolver.setUsername(username);
            }
        }
    }

    @NotNull
    public Map<String, String> getPasswordsMap(AbstractBuildContext builder) {
        Maven3BuildContext buildContext = (Maven3BuildContext) builder;
        Map<String, String> passwordsMap = super.getPasswordsMap(buildContext);
        if (serverConfig == null) {
            return passwordsMap;
        }
        String resolutionRepo = overrideParam(buildContext.getResolutionRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        if (isResolutionConfigured(buildContext, resolutionRepo)) {
            ServerConfig resolutionServerConfig =
                    serverConfigManager.getServerConfigById(buildContext.getResolutionArtifactoryServerId());
            String password = buildContext.getResolverPassword();
            password = overrideParam(password, BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD);
            if (StringUtils.isBlank(password)) {
                password = resolutionServerConfig.getPassword();
            }
            clientConf.resolver.setPassword(password);
            passwordsMap.put(clientConf.resolver.getPrefix() + "password", clientConf.resolver.getPassword());
        }
        return passwordsMap;
    }

    private boolean isResolutionConfigured(Maven3BuildContext buildContext, String resolutionRepo) {
        return buildContext.isResolveFromArtifactory() &&
                StringUtils.isNotBlank(resolutionRepo) &&
                !AbstractBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED.equals(resolutionRepo) &&
                serverConfigManager.getServerConfigById(buildContext.getResolutionArtifactoryServerId()) != null;
    }
}

