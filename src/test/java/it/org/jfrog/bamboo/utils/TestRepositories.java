package it.org.jfrog.bamboo.utils;

import com.jfrog.testing.TestRepository;

/**
 * The test repositories enum. Each one of them will be created during Bamboo startup and will be deleted during shutdown.
 * The repository will be created with a unique name: jfrog-rt-tests-<repoName>-<timestamp>.
 * The repository specs in src/test/resources/integration/settings
 */
public enum TestRepositories {
    LOCAL_REPO("local", TestRepository.RepoType.LOCAL),
    JCENTER_REMOTE_REPO("jcenter", TestRepository.RepoType.REMOTE);

    private final TestRepository testRepository;

    TestRepositories(String repoName, TestRepository.RepoType repoType) {
        this.testRepository = new TestRepository(repoName, repoType);
    }

    public TestRepository getTestRepository() {
        return testRepository;
    }
}
