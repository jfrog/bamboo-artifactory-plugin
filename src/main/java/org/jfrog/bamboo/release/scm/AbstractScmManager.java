package org.jfrog.bamboo.release.scm;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.Repository;
import org.apache.log4j.Logger;

/**
 * @author Tomer Cohen
 */
public abstract class AbstractScmManager<T extends Repository> implements ScmManager {
    private static final Logger log = Logger.getLogger(AbstractScmManager.class);

    public static final String COMMENT_PREFIX = "[artifactory-release] ";
    private final Repository repository;
    private final BuildLogger buildLogger;

    public AbstractScmManager(Repository repository, BuildLogger buildLogger) {
        this.repository = repository;
        this.buildLogger = buildLogger;
    }

    public T getBambooScm() {
        return (T) getRepository();
    }

    private Repository getRepository() {
        return repository;
    }

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }
}
