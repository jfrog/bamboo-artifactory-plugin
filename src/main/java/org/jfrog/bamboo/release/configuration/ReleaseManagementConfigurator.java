package org.jfrog.bamboo.release.configuration;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plan.configuration.MiscellaneousPlanConfigurationPlugin;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Tomer Cohen
 */
public class ReleaseManagementConfigurator extends BaseConfigurablePlugin
        implements MiscellaneousPlanConfigurationPlugin {

    @Override
    public boolean isApplicableTo(@NotNull ImmutablePlan plan) {
        return false;
    }

    @Override
    public boolean isApplicableTo(@NotNull Plan plan) {
        return false;
    }

    public void transformBuildDefinition(@NotNull Map<String, Object> configObjects,
            @NotNull Map<String, String> configParameters, @NotNull BuildDefinition definition) {
    }
}
