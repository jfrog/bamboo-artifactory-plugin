package org.jfrog.bamboo.bintray.client;

import java.util.List;

/**
 * @author Aviad Shikloshi
 */
public class ArtifactorySearchResults {

    List<ArtifactoryResponseEntry> results;

    public List<ArtifactoryResponseEntry> getResults() {
        return results;
    }

    public void setResults(List<ArtifactoryResponseEntry> results) {
        this.results = results;
    }
}
