package org.jfrog.bamboo.bintray.client;

import java.util.List;

/**
 * Contains a result entry from Artifactory when using Artifactory Query Language
 *
 * Spring migration class
 * @author Aviad Shikloshi
 */
public class AQLSearchResults {

    List<AQLEntry> results;

    public List<AQLEntry> getResults() {
        return results;
    }

    public void setResults(List<AQLEntry> results) {
        this.results = results;
    }
}
