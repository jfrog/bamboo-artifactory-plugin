package org.jfrog.bamboo.release.action.condition;

import com.atlassian.bamboo.build.DefaultJob;
import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plugins.web.conditions.AbstractPlanPermissionCondition;
import com.atlassian.bamboo.security.acegi.acls.BambooPermission;
import com.atlassian.bamboo.task.TaskDefinition;
import org.jfrog.bamboo.context.AbstractBuildContext;

import java.util.List;
import java.util.Map;

/**
 * A condition that checks whether the action of {@link org.jfrog.bamboo.release.action.ReleaseAndPromotionAction},
 * which is the "Artifactory Release & Promotion" tab should be displayed.
 *
 * @author Tomer Cohen
 */
public class ReleaseAndPromotionActionCondition extends AbstractPlanPermissionCondition {

    private PlanManager planManager;

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        String planKey = (String) context.get("planKey");
        Job job = (DefaultJob) planManager.getPlanByKey(PlanKeys.getPlanKey(planKey));
        if (job == null) {
            return false;
        }
        List<TaskDefinition> taskDefs = job.getBuildDefinition().getTaskDefinitions();
        for (TaskDefinition taskDef : taskDefs) {
            AbstractBuildContext buildContext = AbstractBuildContext.createContextFromMap(taskDef.getConfiguration());
            if (buildContext != null) {
                if (checkPlanPermission(context, BambooPermission.BUILD) &&
                        buildContext.releaseManagementContext.isReleaseMgmtEnabled()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }
}
