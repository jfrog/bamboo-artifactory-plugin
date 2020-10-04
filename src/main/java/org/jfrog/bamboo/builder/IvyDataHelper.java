package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.Map;

/**
 * @author Tomer Cohen
 */
public class IvyDataHelper extends MavenAndIvyBuildInfoDataHelperBase {

    public IvyDataHelper(BuildParamsOverrideManager buildParamsOverrideManager, TaskContext context, AbstractBuildContext buildContext, EnvironmentVariableAccessor envVarAccessor, String artifactoryPluginVersion, boolean aggregateBuildInfo) {
        super(buildParamsOverrideManager, context, buildContext, envVarAccessor, artifactoryPluginVersion, aggregateBuildInfo);
    }

    @Override
    protected void setClientData(TaskContext taskContext, AbstractBuildContext builder, ArtifactoryClientConfiguration clientConf,
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

