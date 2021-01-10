package it.org.jfrog.bamboo;

import com.atlassian.bamboo.plan.PlanExecutionManager;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Run the "maven-example".
 *
 * @author yahavi
 */
@RunWith(AtlassianPluginsTestRunner.class)
public class MavenTest extends IntegrationTestsBase {
    private static final String PLAN_KEY = "IT-MAV";

    public MavenTest(PlanManager planManager, PlanExecutionManager planExecutionManager) {
        super(planManager, planExecutionManager, PLAN_KEY);
    }

    @Test
    public void checkPlanResults() {
        Set<String> expectedArtifacts = Sets.newHashSet("multi-3.7-SNAPSHOT.pom");
        Build buildInfo = getAndAssertPlanBuildInfo();
        assertEquals(4, buildInfo.getModules().size());

        Module module = helper.getAndAssertModule(buildInfo, "org.jfrog.test:multi:3.7-SNAPSHOT");
        helper.assertModuleArtifacts(module, expectedArtifacts);
        assertTrue(CollectionUtils.isEmpty(module.getDependencies()));
        helper.assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi1:3.7-SNAPSHOT");
        helper.assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi2:3.7-SNAPSHOT");
        helper.assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi3:3.7-SNAPSHOT");
    }
}