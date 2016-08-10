package org.jfrog.bamboo.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.ArtifactoryBuildInfoPropertyHelper;
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
public class MavenPropertyHelper extends ArtifactoryBuildInfoPropertyHelper {

    @Override
    protected void addClientProperties(AbstractBuildContext builder, ArtifactoryClientConfiguration clientConf,
                                       ServerConfig serverConfig, Map<String, String> environment) {

        Maven3BuildContext buildContext = (Maven3BuildContext) builder;

        clientConf.publisher.setRecordAllDependencies(buildContext.isRecordAllDependencies());

        String resolutionRepo = overrideParam(buildContext.getResolutionRepo(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVE_REPO);
        if (buildContext.isResolveFromArtifactory() && StringUtils.isNotBlank(resolutionRepo) &&
                !AbstractBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED.equals(resolutionRepo)) {
            long serverId = buildContext.getResolutionArtifactoryServerId();
            if (serverId > -1) {
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
                    String password = buildContext.getResolverPassword();
                    password = overrideParam(password, BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD);
                    if (StringUtils.isBlank(password)) {
                        password = resolutionServerConfig.getPassword();
                    }
                    clientConf.resolver.setPassword(password);
                }
            }
        }
    }
}

