package org.jfrog.bamboo.release.vcs;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.version.VcsHelper;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Tomer Cohen
 */
public abstract class AbstractVcsManager implements VcsManager {
    private static final Logger log = Logger.getLogger(AbstractVcsManager.class);
    private BuildContext context;
    private final BuildLogger buildLogger;

    public AbstractVcsManager(BuildContext context, BuildLogger buildLogger) {
        this.context = context;
        this.buildLogger = buildLogger;
    }

    @Nullable
    protected File getAndValidateCheckoutDirectory() {
        File checkoutDir = VcsHelper.getCheckoutDirectory(context);
        if (checkoutDir == null) {
            throw new IllegalStateException("Unable to resolve checkout directory.");
        }
        return checkoutDir;
    }

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }

    protected BuildContext getContext() {
        return context;
    }

    @Nullable
    protected Map<String, String> getTaskConfiguration() {
        List<TaskDefinition> tasks = this.getContext().getBuildDefinition().getTaskDefinitions();
        for (TaskDefinition taskDefinition : tasks) {
            // Check that task definition enabled.
            if (taskDefinition.isEnabled()) {
                AbstractBuildContext config = AbstractBuildContext.createContextFromMap(taskDefinition.getConfiguration());
                // Check the release management is enabled
                if ((config != null) && config.releaseManagementContext.isReleaseMgmtEnabled()) {
                    return taskDefinition.getConfiguration();
                }
            }
        }
        return null;
    }
}
