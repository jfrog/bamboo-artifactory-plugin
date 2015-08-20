package org.jfrog.bamboo.bintray.client;

import java.util.List;

/**
 * List for all result entries returned from Artifactory by applying GAVC Search API
 *
 * Spring migration class
 * @author Aviad Shikloshi
 */
public class GavcSearchResults {

    List<GavcSearchEntry> results;

    public List<GavcSearchEntry> getResults() {
        return results;
    }

    public void setResults(List<GavcSearchEntry> results) {
        this.results = results;
    }
}
