package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.task.ArtifactoryDotNetCoreTask;

import java.util.Map;

/**
 * Created by Bar Belity on 14/10/2020.
 *
 * Configuration for {@link ArtifactoryDotNetCoreTask}
 */
public class ArtifactoryDotNetCoreConfiguration extends AbstractDotNetBuildConfiguration {

    private static final String KEY = "artifactoryDotNetCoreBuilder";

    public ArtifactoryDotNetCoreConfiguration() {
        super(CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".dotnet");
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("artifactoryDotNetCoreTask", this);
        context.put("builderType", this);
        context.put("builder", this);
    }

    @Override
    protected String getKey() {
        return KEY;
    }
}
