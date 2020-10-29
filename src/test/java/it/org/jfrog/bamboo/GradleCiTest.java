package it.org.jfrog.bamboo;

import com.atlassian.bamboo.plan.PlanExecutionManager;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.google.common.collect.Sets;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Run the "gradle-example-ci-server"
 *
 * @author yahavi
 */
@RunWith(AtlassianPluginsTestRunner.class)
public class GradleCiTest extends IntegrationTestsBase {
    private static final String PLAN_KEY = "IT-GRADCI";

    public GradleCiTest(PlanManager planManager, PlanExecutionManager planExecutionManager) {
        super(planManager, planExecutionManager, PLAN_KEY);
    }

    @Test
    public void checkPlanResults() {
        Set<String> expectedArtifacts = Sets.newHashSet("gradle-example-ci-server-1.0.jar");

        Build buildInfo = getAndAssertPlanBuildInfo();
        assertEquals(5, buildInfo.getModules().size());

        Module module = helper.getAndAssertModule(buildInfo, "org.jfrog.example.gradle:gradle-example-ci-server:1.0");
        helper.assertModuleArtifacts(module, expectedArtifacts);
        assertTrue(module.getDependencies() == null || module.getDependencies().isEmpty());

        helper.assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:services:1.0");
        helper.assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:api:1.0");
        helper.assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:shared:1.0");
        helper.assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:webservice:1.0");
    }
}