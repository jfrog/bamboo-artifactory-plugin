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

import bucket.core.actions.PagerPaginationSupport;
import com.atlassian.bamboo.security.GlobalApplicationSecureObject;
import com.atlassian.bamboo.ww2.actions.admin.user.AbstractEntityPagerSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.search.page.DefaultPager;
import org.jfrog.bamboo.util.ConstantValues;

/**
 * Existing global Artifactory server configuration list action
 *
 * @author Noam Y. Tenne
 */
public class ExistingArtifactoryServerAction extends AbstractEntityPagerSupport implements GlobalAdminSecurityAware {

    private ServerConfigManager serverConfigManager;

    public ExistingArtifactoryServerAction() {
        serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
    }

    public String doBrowse() throws Exception {
        getPaginationSupport().setItems(new DefaultPager(serverConfigManager.getAllServerConfigs()));
        return super.execute();
    }

    @Override
    public Object getSecuredDomainObject() {
        return GlobalApplicationSecureObject.INSTANCE;
    }

    @Override
    public PagerPaginationSupport getPaginationSupport() {
        return super.getPaginationSupport();
    }
}
