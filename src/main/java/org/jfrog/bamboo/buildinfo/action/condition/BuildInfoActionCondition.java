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

import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.spring.scanner.annotation.imports.BambooImport;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.bamboo.util.ConstantValues;

import javax.inject.Inject;

import java.util.Map;

/**
 * Condition class to determine if the Build info summary tab should be displayed
 *
 * @author Noam Y. Tenne
 */
public class BuildInfoActionCondition implements Condition {

    private ResultsSummaryManager resultsSummaryManager;

    public void init(Map<String, String> params) throws PluginParseException {
    }

    public boolean shouldDisplay(Map<String, Object> context) {
        final String buildKey = StringUtils.defaultString((String) context.get("planKey"), (String) context.get("buildKey"));
        String buildNumber = (String) context.get("buildNumber");
        if (buildKey == null || buildNumber == null) {
            return false;
        }

        PlanResultKey planResultKey = PlanKeys.getPlanResultKey(PlanKeys.getPlanKey(buildKey), Integer.parseInt(buildNumber));
        ResultsSummary resultsSummary = resultsSummaryManager.getResultsSummary(planResultKey);
        return resultsSummary != null &&
                resultsSummary.isSuccessful() &&
                Boolean.valueOf(resultsSummary.getCustomBuildData().get(ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM));
    }

    @Inject
    public BuildInfoActionCondition(@BambooImport ResultsSummaryManager resultsSummaryManager) {
        this.resultsSummaryManager = resultsSummaryManager;
    }
}
