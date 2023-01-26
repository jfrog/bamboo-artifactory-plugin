package org.jfrog.bamboo.util.generic;

import com.atlassian.bamboo.task.CommonTaskContext;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.AntPatternsDependenciesHelper;
import org.jfrog.build.extractor.clientConfiguration.util.BuildDependenciesHelper;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader;

import java.io.IOException;
import java.util.List;

/**
 * Resolver that knows how to bring the dependencies from the resolve pattern.
 *
 * @author Lior Hasson
 */
public class GenericArtifactsResolver {
    private final ArtifactoryManager artifactoryManager;
    private String resolvePattern;
    private Log log;
    private CommonTaskContext context;

    public GenericArtifactsResolver(CommonTaskContext context, ArtifactoryManager artifactoryManager, String resolvePattern, Log log) {
        this.context = context;
        this.artifactoryManager = artifactoryManager;
        this.resolvePattern = resolvePattern;
        this.log = log;
    }

    public List<Dependency> retrievePublishedDependencies() throws IOException, InterruptedException {
        AntPatternsDependenciesHelper helper = new AntPatternsDependenciesHelper(createDependenciesDownloader(), log);
        return helper.retrievePublishedDependencies(resolvePattern);
    }

    public List<BuildDependency> retrieveBuildDependencies() throws IOException, InterruptedException {
        BuildDependenciesHelper helper = new BuildDependenciesHelper(createDependenciesDownloader(), log);
        return helper.retrieveBuildDependencies(resolvePattern);
    }

    private DependenciesDownloader createDependenciesDownloader() {
        return new DependenciesDownloaderImpl(artifactoryManager, context.getWorkingDirectory(), log);
    }
}
