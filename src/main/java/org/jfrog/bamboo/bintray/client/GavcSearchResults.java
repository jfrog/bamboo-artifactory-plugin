package org.jfrog.bamboo.bintray.client;

import java.util.List;

/**
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
