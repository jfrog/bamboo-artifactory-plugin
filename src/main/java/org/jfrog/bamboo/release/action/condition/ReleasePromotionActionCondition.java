package org.jfrog.bamboo.release.action.condition;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.branch.ChainBranch;
import com.atlassian.bamboo.plugins.web.conditions.AbstractPlanPermissionCondition;
import com.atlassian.bamboo.security.acegi.acls.BambooPermission;
import com.atlassian.bamboo.task.TaskDefinition;
import org.jfrog.bamboo.context.AbstractBuildContext;

import java.util.List;
import java.util.Map;

/**
 * A condition that checks whether the action of {@link org.jfrog.bamboo.release.action.ReleasePromotionAction},
 * which is the "Artifactory Release & Promotion" tab should be displayed.
 *
 * @author Tomer Cohen
 */
public class ReleasePromotionActionCondition extends AbstractPlanPermissionCondition {

    private PlanManager planManager;

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        String planKeyStr = (String) context.get("planKey");
        if (planKeyStr == null) {
            return false;
        }
        PlanKey planKey = PlanKeys.getPlanKey(planKeyStr);
        if (!bambooPermissionManager.hasPlanPermission(BambooPermission.BUILD, planKey)) {
            return false;
        }
        Plan plan = planManager.getPlanByKey(planKey);
        if (plan == null) {
            plan = extractMasterPlanFromBranchPlan(planKey);
            if (plan == null) {
                return false;
            }
        }
        List<TaskDefinition> taskDefs = plan.getBuildDefinition().getTaskDefinitions();
        for (TaskDefinition taskDef : taskDefs) {
            if (taskDef.isEnabled()) {
                AbstractBuildContext buildContext = AbstractBuildContext.createContextFromMap(taskDef.getConfiguration());
                if (buildContext != null && buildContext.releaseManagementContext.isReleaseMgmtEnabled()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extract plan of the master branch
     *
     * @param planKey The plan key in form of <PROJECT>-<BRANCH#>-<JOB>
     * @return plan of the master branch
     */
    private Plan extractMasterPlanFromBranchPlan(PlanKey planKey) {
        // Get chain key: <PROJECT>-<BRANCH#>
        PlanKey chainKey = PlanKeys.getChainKeyIfJobKey(planKey);
        if (chainKey == null) {
            return null;
        }

        // Get plain of the branch plan
        Plan chainKeyPlan = planManager.getPlanByKey(chainKey);
        if (!(chainKeyPlan instanceof ChainBranch)) {
            return null;
        }

        // Get master of the branch plan
        Plan master = (Plan) chainKeyPlan.getMaster();
        if (master == null) {
            return null;
        }

        // Get master job key in form of <PROJECT>-<BRANCH>-<JOB>
        PlanKey masterJobKey = PlanKeys.getJobKey(master.getPlanKey(), PlanKeys.getPartialJobKey(planKey));

        // Get the master plan
        return planManager.getPlanByKey(masterJobKey);
    }

    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }
}
