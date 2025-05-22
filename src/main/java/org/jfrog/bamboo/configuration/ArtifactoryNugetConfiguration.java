package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.task.ArtifactoryNugetTask;

import java.util.Map;

/**
 * Created by Bar Belity on 12/10/2020.
 *
 * Configuration for {@link ArtifactoryNugetTask}
 */
public class ArtifactoryNugetConfiguration extends AbstractDotNetBuildConfiguration {

    private static final String KEY = "artifactoryNugetBuilder";

    @Override
    protected String getCapabilityPrefix() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".nuget";
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("artifactoryNugetTask", this);
        context.put("builderType", this);
        context.put("builder", this);
    }

    @Override
    protected String getKey() {
        return KEY;
    }
}
