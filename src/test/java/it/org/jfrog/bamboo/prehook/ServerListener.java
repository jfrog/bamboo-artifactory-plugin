package it.org.jfrog.bamboo.prehook;

import com.atlassian.bamboo.event.ServerStartedEvent;
import com.atlassian.event.api.EventListener;
import com.jfrog.testing.IntegrationTestsHelper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static it.org.jfrog.bamboo.prehook.RemoteAgent.startAgent;
import static it.org.jfrog.bamboo.prehook.RepositoriesHandler.createTestRepositories;
import static it.org.jfrog.bamboo.utils.Utils.GRADLE_HOME_ENV;
import static it.org.jfrog.bamboo.utils.Utils.MAVEN_HOME_ENV;

/**
 * Contains the ServerStarted listener event.
 *
 * @author yahavi
 **/
public class ServerListener {

    private static final Logger log = LogManager.getLogger(RemoteAgent.class);

    /**
     * Run after Tomcat server started with the Bamboo CI.
     *
     * @param buildStarted - Exist just to filter out event types other than ServerStartedEvent.
     */
    @SuppressWarnings("unused")
    @EventListener
    public void serverStarted(ServerStartedEvent buildStarted) {
        try {
            verifyEnvironment();
            createTestRepositories();
            startAgent();
        } catch (Exception e) {
            String msg = "Bamboo Artifactory plugin tests: An error occurred";
            log.error(msg + ": " + ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(msg, e);
        }
        // Run the garbage collector to clear up the heap
        System.gc();
    }

    /**
     * Verify required environment variables for the tests.
     */
    private void verifyEnvironment() {
        IntegrationTestsHelper.verifyEnvironment(MAVEN_HOME_ENV);
        IntegrationTestsHelper.verifyEnvironment(GRADLE_HOME_ENV);
    }
}
