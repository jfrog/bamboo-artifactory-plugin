package org.jfrog.bamboo.util;

import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.builder.ArtifactoryBuildInfoPropertyHelper;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.Map;

/**
 * @author Tomer Cohen
 */
public class IvyPropertyHelper extends ArtifactoryBuildInfoPropertyHelper {

    @Override
    protected void addClientProperties(AbstractBuildContext builder, ArtifactoryClientConfiguration clientConf,
                                       ServerConfig serverConfig, Map<String, String> environment) {

        if (builder.isPublishArtifacts()) {
            clientConf.publisher.setM2Compatible(builder.isMaven2Compatible());
            if (!clientConf.publisher.isM2Compatible()) {
                clientConf.publisher.setIvyPattern(builder.getIvyPattern());
                clientConf.publisher.setIvyArtifactPattern(builder.getArtifactPattern());
            }
        }
    }
}

