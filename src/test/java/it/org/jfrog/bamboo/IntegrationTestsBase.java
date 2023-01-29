package it.org.jfrog.bamboo;

import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.plan.*;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.user.impl.DefaultUser;
import com.jfrog.testing.IntegrationTestsHelper;
import it.org.jfrog.bamboo.utils.TestRepositories;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static it.org.jfrog.bamboo.utils.Utils.*;
import static org.junit.Assert.*;

/**
 * Base class for all integration tests.
 *
 * @author yahavi
 */
public abstract class IntegrationTestsBase {
    private static final Logger log = LogManager.getLogger(IntegrationTestsBase.class);

    private static final int PLAN_MAX_SECS = 300;

    private final PlanExecutionManager planExecutionManager;
    private final Map<String, String> overrideVars;
    private final Map<String, String> env;
    private final PlanManager planManager;
    private final String planKey;
    IntegrationTestsHelper helper;

    public IntegrationTestsBase(@ComponentImport PlanManager planManager, @ComponentImport PlanExecutionManager planExecutionManager, String planKey) {
        String localRepoKey = IntegrationTestsHelper.getRepoKey(TestRepositories.LOCAL_REPO.getTestRepository());
        String jcenter = IntegrationTestsHelper.getRepoKey(TestRepositories.JCENTER_REMOTE_REPO.getTestRepository());
        this.planExecutionManager = planExecutionManager;
        this.overrideVars = createOverrideVars(localRepoKey, jcenter);
        this.env = createEnv();
        this.planManager = planManager;
        this.planKey = planKey;
    }

    @SuppressWarnings("unused")
    abstract void checkPlanResults();

    /**
     * Execute a plan and wait for termination.
     */
    @Before
    public void executePlan() {
        helper = new IntegrationTestsHelper();
        PlanKey planKey = PlanKeys.getPlanKey(this.planKey);
        ImmutableChain chain = planManager.getPlanByKey(planKey, Chain.class);
        assertNotNull(chain);
        if (chain.isSuspended()) {
            log.warn("Plan '" + this.planKey + "' is suspended - resuming it.");
            planManager.setPlanSuspendedState(planKey, false);
        }
        ExecutionRequestResult result = planExecutionManager.startManualExecution(chain, new DefaultUser("tester"), env, overrideVars);
        assertFalse(String.join("\n", result.getErrors().getAllErrorMessages()), result.getErrors().hasAnyErrors());
        waitForPlanToFinish(result);
    }

    @After
    public void tearDown() {
        helper.close();
        System.gc();
    }

    /**
     * Wait for plan to finish. Fail the test if the plan failed.
     *
     * @param result - The "startManualExecution" results
     */
    private void waitForPlanToFinish(ExecutionRequestResult result) {
        PlanResultKey planResultKey = result.getPlanResultKey();
        assertNotNull(planResultKey);
        ChainExecution chainExecution = (ChainExecution) planExecutionManager.getExecutionStatus(planResultKey);
        assertNotNull(chainExecution);
        try {
            for (int i = 0; i < PLAN_MAX_SECS; i++) {
                if (chainExecution.isCompleted()) {
                    break;
                }
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            }
        } catch (InterruptedException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }

        assertFalse("Plan " + planKey + " failed.\n" +
                downloadBuildLog(log, planResultKey.getBuildNumber(), planKey), chainExecution.isFailed());
        assertTrue("Timeout occurred for plan ID " + planResultKey, chainExecution.isCompleted());
    }

    /**
     * Get plan build info from Artifactory.
     *
     * @return build info for the plan
     */
    BuildInfo getAndAssertPlanBuildInfo() {
        ImmutableChain chain = planManager.getPlanByKey(PlanKeys.getPlanKey(planKey), Chain.class);
        assertNotNull(chain);
        try {
            String buildName = chain.getAllJobs().get(0).getName();
            String buildNumber = String.valueOf(chain.getLastBuildNumber());
            BuildInfo buildInfo = helper.getBuildInfo(buildName, buildNumber, "");
            assertNotNull(buildInfo);
            helper.assertFilteredProperties(buildInfo);
            return buildInfo;
        } catch (IOException e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }
}
