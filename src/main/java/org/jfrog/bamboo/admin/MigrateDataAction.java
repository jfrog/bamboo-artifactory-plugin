package org.jfrog.bamboo.admin;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import com.atlassian.spring.container.ContainerManager;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.util.ConstantValues;

/**
 * Created by diman on 01/11/2016.
 */
public class MigrateDataAction extends BambooActionSupport implements GlobalAdminSecurityAware {

    private final ServerConfigManager serverConfigManager;
    private transient Logger log = Logger.getLogger(MigrateDataAction.class);

    public MigrateDataAction() {
        serverConfigManager = (ServerConfigManager) ContainerManager.getComponent(
                ConstantValues.PLUGIN_CONFIG_MANAGER_KEY);
    }

    public String doMigrate() {
        log.info("Migrating Artifactory plugin data to plugins 2");
        try {
            serverConfigManager.migrateToVersionTwo();
        } catch (Exception e) {
            String errorMessage = e.getClass().getCanonicalName() + ": " + e.getMessage();
            addActionError("Migration failed " + errorMessage);
            log.error("Error occurred while migrating data to plugins 2.", e);
            return INPUT;
        }
        addActionMessage("Migration completed successfully!");
        return INPUT;
    }
}
