package org.jfrog.bamboo.bintray.client;

import org.jfrog.build.api.Build;

/**
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
