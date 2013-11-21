package org.jfrog.bamboo.release.scm;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.util.version.ScmHelper;

import java.io.File;

/**
 * @author Tomer Cohen
 */
public abstract class AbstractScmManager<T extends Repository> implements ScmManager {
    private static final Logger log = Logger.getLogger(AbstractScmManager.class);

    public static final String COMMENT_PREFIX = "[artifactory-release] ";
    private BuildContext context;
    private final Repository repository;
    private final BuildLogger buildLogger;

    public AbstractScmManager(BuildContext context, Repository repository, BuildLogger buildLogger) {
        this.context = context;
        this.repository = repository;
        this.buildLogger = buildLogger;
    }

    public T getBambooScm() {
        return (T) getRepository();
    }

    @Nullable
    protected File getAndValidateCheckoutDirectory() {
        File checkoutDir = ScmHelper.getCheckoutDirectory(context);
        if (checkoutDir == null) {
            throw new IllegalStateException("Unable to resolve checkout directory.");
        }
        return checkoutDir;
    }

    private Repository getRepository() {
        return repository;
    }

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }

    protected BuildContext getContext() {
        return context;
    }
}
