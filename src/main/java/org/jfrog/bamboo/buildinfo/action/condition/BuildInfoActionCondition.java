/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.buildinfo.action.condition;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import org.jfrog.bamboo.util.ConstantValues;

import java.util.Map;

/**
 * Condition class to determine if the Build info summary tab should be displayed
 *
 * @author Noam Y. Tenne
 */
public class BuildInfoActionCondition implements Condition {

    private PlanManager planManager;
    private ResultsSummaryManager resultsSummaryManager;

    public void init(Map<String, String> params) throws PluginParseException {
    }

    public boolean shouldDisplay(Map<String, Object> context) {
        String planKey = context.get("planKey") == null ? null
                : (String) context.get("planKey");
        String buildNumber = context.get("buildNumber") == null ? null
                : (String) context.get("buildNumber");
        if (planKey == null || buildNumber == null) {
            return false;
        }

        Plan plan = planManager.getPlanByKey(planKey);
        if (plan == null) {
            return false;
        }
        PlanResultKey planResultKey = PlanKeys.getPlanResultKey(plan.getPlanKey(), Integer.parseInt(buildNumber));
        ResultsSummary resultsSummary = resultsSummaryManager.getResultsSummary(planResultKey);
        if (resultsSummary == null) {
            return false;
        } else if (resultsSummary.isSuccessful()) {
            Map<String, String> customData = resultsSummary.getCustomBuildData();
            return customData.containsKey(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM) &&
                    Boolean.valueOf(customData.get(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM));
        }
        return false;
    }

    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }

    public void setResultsSummaryManager(ResultsSummaryManager resultsSummaryManager) {
        this.resultsSummaryManager = resultsSummaryManager;
    }
}
