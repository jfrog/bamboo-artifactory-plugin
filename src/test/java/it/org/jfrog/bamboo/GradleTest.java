package it.org.jfrog.bamboo;

import com.atlassian.bamboo.plan.PlanExecutionManager;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import org.jfrog.build.api.Build;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Run the "gradle-example".
 *
 * @author yahavi
 */
@RunWith(AtlassianPluginsTestRunner.class)
public class GradleTest extends IntegrationTestsBase {
    private static final String PLAN_KEY = "IT-GRAD";

    public GradleTest(PlanManager planManager, PlanExecutionManager planExecutionManager) {
        super(planManager, planExecutionManager, PLAN_KEY);
    }

    @Test
    public void checkPlanResults() {
        Build buildInfo = getAndAssertPlanBuildInfo();
        assertEquals(3, buildInfo.getModules().size());

        helper.assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:api:1.0-SNAPSHOT");
        helper.assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:shared:1.0-SNAPSHOT");
        helper.assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:webservice:1.0-SNAPSHOT");
    }
}