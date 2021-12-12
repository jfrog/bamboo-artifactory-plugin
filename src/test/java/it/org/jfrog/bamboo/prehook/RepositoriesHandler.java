package it.org.jfrog.bamboo.prehook;

import com.jfrog.testing.IntegrationTestsHelper;
import it.org.jfrog.bamboo.IntegrationTestsBase;
import it.org.jfrog.bamboo.utils.TestRepositories;
import org.apache.commons.text.StringSubstitutor;

/**
 * Creates required Artifactory repositories before integration tests.
 * Deletes Artifactory required repositories after integration tests.
 * The repositories specs are under 'test/resources/integration/settings/'.
 *
 * @author yahavi
 */
public class RepositoriesHandler {

    /**
     * Create integration tests repositories.
     */
    static void createTestRepositories() {
        try (IntegrationTestsHelper helper = new IntegrationTestsHelper()) {
            helper.cleanUpArtifactory();
            StringSubstitutor repoSubstitotor = new StringSubstitutor();
            for (TestRepositories testRepository : TestRepositories.values()) {
                helper.createRepo(testRepository.getTestRepository(), repoSubstitotor, IntegrationTestsBase.class.getClassLoader());
            }
        }

        Runtime.getRuntime().addShutdownHook(new DeleteTestRepositories());
    }

    /**
     * Implement a shutdown hook to delete test repositories before Tomcat shutdown.
     */
    private static class DeleteTestRepositories extends Thread {
        @Override
        public void run() {
            try (IntegrationTestsHelper helper = new IntegrationTestsHelper()) {
                for (TestRepositories testRepository : TestRepositories.values()) {
                    helper.deleteRepo(testRepository.getTestRepository());
                }
            }
        }
    }
}
