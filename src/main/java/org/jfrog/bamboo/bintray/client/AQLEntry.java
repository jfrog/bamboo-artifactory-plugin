package org.jfrog.bamboo.bintray.client;

import java.util.Map;

/**
 * AQL result entry that will return in list of such entries when using Artifactory Query Language API
 *
 * Spring migration class
 * @author Aviad Shikloshi
 */
public class AQLEntry implements Map.Entry<String, String> {

    private String key;
    private String value;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String setValue(String value) {
        String oldValue = this.value;
        this.value = value;
        return oldValue;
    }
}
