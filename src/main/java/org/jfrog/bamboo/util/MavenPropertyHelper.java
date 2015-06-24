package org.jfrog.bamboo.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.ArtifactoryBuildInfoPropertyHelper;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

/**
 * Helper to populate Maven specific properties to the client configuration
 *
 * @author Tomer Cohen
 */
public class MavenPropertyHelper extends ArtifactoryBuildInfoPropertyHelper {

    @Override
    protected void addClientProperties(AbstractBuildContext builder, ArtifactoryClientConfiguration clientConf,
            ServerConfig serverConfig) {
        super.addClientProperties(builder, clientConf, serverConfig);
        Maven3BuildContext buildContext = (Maven3BuildContext) builder;

        clientConf.publisher.setRecordAllDependencies(buildContext.isRecordAllDependencies());

        String resolutionRepo = buildContext.getResolutionRepo();
        if (buildContext.isResolveFromArtifactory() && StringUtils.isNotBlank(resolutionRepo) &&
                !AbstractBuildContext.NO_RESOLUTION_REPO_KEY_CONFIGURED.equals(resolutionRepo)) {
            long serverId = buildContext.getResolutionArtifactoryServerId();
            if (serverId > -1) {
                ServerConfig resolutionServerConfig = serverConfigManager.getServerConfigById(serverId);
                if (resolutionServerConfig != null) {
                    clientConf.resolver.setContextUrl(resolutionServerConfig.getUrl());
                    clientConf.resolver.setRepoKey(resolutionRepo);
                    String username = buildContext.getResolverUserName();
                    if (StringUtils.isBlank(username)) {
                        username = resolutionServerConfig.getUsername();
                    }
                    clientConf.resolver.setUsername(username);
                    String password = buildContext.getResolverPassword();
                    if (StringUtils.isBlank(password)) {
                        password = resolutionServerConfig.getPassword();
                    }
                    clientConf.resolver.setPassword(password);
                }
            }
        }
    }
}

