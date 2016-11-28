package org.jfrog.bamboo.release.scm;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.vcs.configuration.PlanRepositoryDefinition;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.util.version.ScmHelper;

import java.io.File;

/**
 * @author Tomer Cohen
 */
public abstract class AbstractScmManager implements ScmManager {
    private static final Logger log = Logger.getLogger(AbstractScmManager.class);

    public static final String COMMENT_PREFIX = "[artifactory-release] ";
    private BuildContext context;
    protected final PlanRepositoryDefinition repository;
    protected final RepositoryConfiguration configuration;
    private final BuildLogger buildLogger;

    public AbstractScmManager(BuildContext context, PlanRepositoryDefinition repository, BuildLogger buildLogger) {
        this.context = context;
        this.repository = repository;
        this.buildLogger = buildLogger;
        this.configuration = new RepositoryConfiguration(repository);
    }

    public PlanRepositoryDefinition getBambooScm() {
        return getRepository();
    }

    @Nullable
    protected File getAndValidateCheckoutDirectory() {
        File checkoutDir = ScmHelper.getCheckoutDirectory(context);
        if (checkoutDir == null) {
            throw new IllegalStateException("Unable to resolve checkout directory.");
        }
        return checkoutDir;
    }

    private PlanRepositoryDefinition getRepository() {
        return repository;
    }

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }

    protected BuildContext getContext() {
        return context;
    }
}
