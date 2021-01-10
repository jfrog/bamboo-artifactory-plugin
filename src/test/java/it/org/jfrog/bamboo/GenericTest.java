package it.org.jfrog.bamboo;

import com.atlassian.bamboo.plan.PlanExecutionManager;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.google.common.collect.Sets;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Run upload and download.
 *
 * @author yahavi
 */
@RunWith(AtlassianPluginsTestRunner.class)
public class GenericTest extends IntegrationTestsBase {
    private static final String PLAN_KEY = "IT-GEN";

    public GenericTest(PlanManager planManager, PlanExecutionManager planExecutionManager) {
        super(planManager, planExecutionManager, PLAN_KEY);
    }

    @Test
    public void checkPlanResults() {
        Build buildInfo = getAndAssertPlanBuildInfo();
        assertEquals(1, buildInfo.getModules().size());
        Module module = helper.getAndAssertModule(buildInfo, buildInfo.getName() + ":" + buildInfo.getNumber());
        helper.assertModuleArtifacts(module, Sets.newHashSet("README.md"));
        helper.assertModuleDependencies(module, Sets.newHashSet("test-file"));
    }
}