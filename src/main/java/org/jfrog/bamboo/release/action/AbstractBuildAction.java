package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.v2.build.BaseConfigurableBuildPlugin;
import org.apache.log4j.Logger;

/**
 * Abstract build action class which is has convenience methods especially for logging by adding the '[RELEASE]' prefix
 * to the logs that are printed.
 *
 * @author Tomer Cohen
 */
public abstract class AbstractBuildAction extends BaseConfigurableBuildPlugin {
    private static final Logger log = Logger.getLogger(AbstractBuildAction.class);
    private BuildLogger buildLogger;

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }

    protected void setBuildLogger(BuildLogger buildLogger) {
        this.buildLogger = buildLogger;
    }
}
