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
        for (int i = 0; i < tasks.size(); i++)
        {
            final TaskDefinition definition = tasks.get(i);
            if (definition != null && definition.getPluginKey().contains("bamboo-artifactory-plugin") && isTaskHasVcsConfiguration(definition)) {
                return definition.getConfiguration();
            }
        }
        return null;
    }

    /**
     * checks if task definition has VCS configurations by checking config keys with prefix AbstractBuildContext.VCS_PREFIX
     *
     * @param taskDefinition task definition. not null
     * @return true, if definition contains vcs config
     */
    private boolean isTaskHasVcsConfiguration(TaskDefinition taskDefinition) {
        final Map<String, String> configuration = taskDefinition.getConfiguration();

        for (String key : configuration.keySet()) {
            if (key.startsWith(AbstractBuildContext.VCS_PREFIX)) {
                return true;
            }
        }

        return false;
    }
}
