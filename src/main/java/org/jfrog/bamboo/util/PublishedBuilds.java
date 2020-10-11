package org.jfrog.bamboo.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bar Belity on 05/10/2020.
 */
public class PublishedBuilds implements Serializable {

    private List<PublishedBuildDetails> builds;

    public List<PublishedBuildDetails> getBuilds() {
        return builds;
    }

    public void setBuilds(List<PublishedBuildDetails> builds) {
        this.builds = builds;
    }

    public void addBuild(PublishedBuildDetails build) {
        if (this.builds == null) {
            this.builds = new ArrayList<>();
        }
        this.builds.add(build);
    }
}
