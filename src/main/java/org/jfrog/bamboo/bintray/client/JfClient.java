package org.jfrog.bamboo.bintray.client;

import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.build.api.Build;

import java.util.List;
import java.util.Map;

/**
 * Artifactory & Bintray client
 *
 * @author Aviad Shikloshi
 */
public interface JfClient {

    /**
     * Get Build info for specific build
     *
     * @param buildName   Bamboo build name
     * @param buildNumber Bamboo build number
     * @return Build object
     */
    Build getBuildInfo(String buildName, String buildNumber);

    /**
     * Sync your build from Bintray to Maven Central
     */
    List<String> mavenCentralSync(String subject, String repo, String packageName, String version);

    /**
     * Retrieves repository properties
     *
     * @param repoKey Artifactory valid and exists key
     * @return all properties for the repoKey
     */
    AQLSearchResults getPropertiesForRepository(String repoKey);

    /**
     * Search Artifactory by Group and Artifact
     */
    GavcSearchResults gavcSearch(String group, String artifact);

    /**
     * Get user plugins info from Artifactory
     */
    Map<String, List<Map>> getUserPluginInfo();

    ServerConfig getArtifactoryServerConfig();
}
