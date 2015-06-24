package org.jfrog.bamboo.util.generic;

import com.atlassian.bamboo.task.TaskContext;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.BuildDependenciesHelper;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesHelper;

import java.io.IOException;
import java.util.List;

/**
 * Resolver that knows how to bring the dependencies from the resolve pattern.
 *
 * @author Lior Hasson
 */
public class GenericArtifactsResolver {
    private final ArtifactoryDependenciesClient client;
    private String resolvePattern;
    private Log log;
    private TaskContext context;

    public GenericArtifactsResolver(TaskContext context, ArtifactoryDependenciesClient client,
                                    String resolvePattern, BambooBuildInfoLog log) throws IOException, InterruptedException {
        this.context = context;
        this.client = client;
        this.resolvePattern = resolvePattern;
        this.log = log;
    }

    public List<Dependency> retrievePublishedDependencies() throws IOException, InterruptedException {
        DependenciesHelper helper = new DependenciesHelper(createDependenciesDownloader(), log);
        return helper.retrievePublishedDependencies(resolvePattern);
    }

    public List<BuildDependency> retrieveBuildDependencies() throws IOException, InterruptedException {
        BuildDependenciesHelper helper = new BuildDependenciesHelper(createDependenciesDownloader(), log);
        return helper.retrieveBuildDependencies(resolvePattern);
    }

    private DependenciesDownloader createDependenciesDownloader() {
        return new DependenciesDownloaderImpl(client, context.getWorkingDirectory(), log);
    }
}
