package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.build.DefaultJob;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.variable.VariableDefinition;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by user on 06/01/2015.
 */
public class BambooUtilsHelper {
    private PlanManager planManager;
    private VariableDefinitionManager variableDefinitionManager;

    public static BambooUtilsHelper getInstance() {
        return (BambooUtilsHelper)ContainerManager.getComponent("artifactoryBambooUtilsHelper");
    }

    public Map<String, String> getGlobalVariables() {
        return getAllVariables(null);
    }

    public Map<String, String> getAllVariables(String planKey) {
        Map<String, String> variableMap = Maps.newHashMap();
        if (StringUtils.isNotBlank(planKey)) {
            Plan plan = planManager.getPlanByKey(planKey);
            if (plan instanceof DefaultJob) {
                // Default jobs don't have any global vars, so fetch the actual plan itself instead
                plan = planManager.getPlanByKey(StringUtils.removeEnd(plan.getKey(), "-" + plan.getBuildKey()));
            }
            appendVariableDefs(variableMap, variableDefinitionManager.getPlanVariables(plan));
            appendVariableDefs(variableMap, variableDefinitionManager.getGlobalNotOverriddenVariables(plan));
        } else {
            appendVariableDefs(variableMap, variableDefinitionManager.getGlobalVariables());
        }

        return variableMap;
    }

    private void appendVariableDefs(Map<String, String> globalVariableMap, List<VariableDefinition> globalVariables) {
        for (VariableDefinition variableDefinition : globalVariables) {
            globalVariableMap.put(variableDefinition.getKey(), variableDefinition.getValue());
        }
    }

    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }

    public void setVariableDefinitionManager(VariableDefinitionManager variableDefinitionManager) {
        this.variableDefinitionManager = variableDefinitionManager;
    }
}
