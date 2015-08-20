package org.jfrog.bamboo.bintray.client;

/**
 * One entry in a result set from Artifactory GAVC search API response
 * An array of such entries will return from ARtifactory when calling this API
 *
 * Spring migration class
 * @author Aviad Shikloshi
 */
public class GavcSearchEntry {

    private String uri;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
