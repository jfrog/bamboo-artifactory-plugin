package org.jfrog.bamboo.bintray.client;

import org.jfrog.build.api.Build;

/**
 * Mapping of Artifactory Build REST call response to Java Bean
 * This will be returned to us from calling /builds/[name]/[number]
 *
 * @author Aviad Shikloshi
 */
public class BuildInfo {

    private Build buildInfo;

    public Build getBuildInfo() {
        return buildInfo;
    }

    public void setBuildInfo(Build buildInfo) {
        this.buildInfo = buildInfo;
    }
}
