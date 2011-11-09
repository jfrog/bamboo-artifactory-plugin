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

package org.jfrog.bamboo.result;

import com.atlassian.bamboo.build.ViewBuildResults;
import org.jfrog.bamboo.util.ConstantValues;

/**
 * Build info action to display on successfully completed builds that were run with the build info collection activated
 *
 * @author Noam Y. Tenne
 */
public class ViewBuildInfoSummaryAction extends ViewBuildResults {

    private String artifactoryBuildInfoUrl;

    @Override
    public String doExecute() throws Exception {
        String superResult = super.doExecute();

        if (ERROR.equals(superResult)) {
            return ERROR;
        }

        StringBuilder builder = new StringBuilder(
                getBuildResultsSummary().getCustomBuildData().get(ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM));
        if (!builder.toString().endsWith("/")) {
            builder.append("/");
        }
        builder.append("webapp/builds/").append(getBuild().getName()).append("/").append(getBuildNumber());
        artifactoryBuildInfoUrl = builder.toString();

        return superResult;
    }

    public String getArtifactoryBuildInfoUrl() {
        return artifactoryBuildInfoUrl;
    }
}