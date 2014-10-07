package org.jfrog.bamboo.util.generic;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;

import java.io.Serializable;
import java.util.List;

import static org.jfrog.build.api.BuildBean.ROOT;

/**
 * This class goal is to represent JSON structure that will be share between
 * the Generic Resolve and Generic Deploy tasks.
 *
 * @author Lior Hasson
 */

@XStreamAlias(ROOT)
public class GenericData implements Serializable {

    @XStreamAlias("buildDependencies")
    private List<BuildDependency> buildDependencies;
    @XStreamAlias("dependencies")
    private List<Dependency> dependencies;

    public List<BuildDependency> getBuildDependencies() {
        return buildDependencies;
    }

    public void setBuildDependencies(List<BuildDependency> buildDependencies) {
        this.buildDependencies = buildDependencies;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }
}
