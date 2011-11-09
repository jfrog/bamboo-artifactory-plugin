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

package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.util.ConstantValues;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Exposes features of the Build Manager service to serve plugin modules which can't reach it.
 *
 * @author Noam Y. Tenne
 */
public class BuildServlet extends HttpServlet {

    private Logger log = Logger.getLogger(BuildServlet.class);

    private PlanManager planManager;

    public BuildServlet() {
        planManager = (PlanManager) ContainerManager.getComponent("planManager");
    }

    /**
     * Requires to be provided with a build key (param name is buildKey).<br> Returns the full name of the build with
     * the given key.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String buildKeyValue = req.getParameter(ConstantValues.BUILD_SERVLET_KEY_PARAM);
        if (StringUtils.isBlank(buildKeyValue)) {
            resp.sendError(HttpStatus.SC_BAD_REQUEST, "Please provide a build key.");
            log.error("Unable to retrieve build information. No build key was provided.");
            return;
        }

        Plan plan = planManager.getPlanByKey(buildKeyValue);

        if (plan == null) {
            resp.sendError(HttpStatus.SC_NOT_FOUND, "Could not find plan with the key " + buildKeyValue + ".");
            log.error("Unable to retrieve build information. No plan was found with the key " + buildKeyValue + ".");
            return;
        }

        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
            writer.write(plan.getName());
            writer.flush();
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
