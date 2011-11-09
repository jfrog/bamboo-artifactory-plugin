package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.CustomBuildProcessor;
import com.atlassian.bamboo.build.artifact.ArtifactManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.repository.RepositoryV2;
import com.atlassian.bamboo.v2.build.task.AbstractBuildTask;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.util.TaskHelper;

import java.io.File;
import java.util.List;

/**
 * Copy the {@code gradle.properties} file to the artifacts folder of the build, this will be later used for detecting
 * the release properties and next integration properties. This applies only to {@link
 * org.jfrog.bamboo.task.ArtifactoryGradleTask} type builds. This is done since if a build is consistently built on a
 * remote agent, the server will may become out of date, and will continuously display incorrect properties and their
 * respective values.
 *
 * @author Tomer Cohen
 */
public class GradlePropertiesCopier extends AbstractBuildTask implements CustomBuildProcessor {
    private static final Logger log = Logger.getLogger(GradlePropertiesCopier.class);

    private volatile ArtifactManager artifactManager;
    private BuildLoggerManager buildLoggerManager;

    @NotNull
    public BuildContext call() throws InterruptedException, Exception {
        BuildLogger buildLogger = buildLoggerManager.getBuildLogger(buildContext.getPlanResultKey());
        buildLogger.startStreamingBuildLogs(buildContext.getPlanResultKey());
        RepositoryV2 repository = buildContext.getBuildDefinition().getRepositoryV2();
        if (repository == null) {
            return buildContext;
        }
        List<TaskDefinition> definitions = buildContext.getBuildDefinition().getTaskDefinitions();
        TaskDefinition taskDefinition = TaskHelper.findGradleBuild(definitions);
        if (taskDefinition == null) {
            log.debug("Current build is not a gradle build");
            return buildContext;
        }
        File rootDir = repository.getSourceCodeDirectory(PlanKeys.getPlanKey(buildContext.getPlanKey()));
        File gradleProps = new File(rootDir, "gradle.properties");
        if (rootDir.exists()) {
            log.info(buildLogger.addBuildLogEntry("Copying the gradle properties artifact for " +
                    "build: " + buildContext.getBuildResultKey()));
            ArtifactDefinitionContextImpl artifact = new ArtifactDefinitionContextImpl();
            artifact.setName("gradle");
            artifact.setLocation("");
            artifact.setCopyPattern(gradleProps.getName());
            artifact.setProducerJobKey(PlanKeys.getPlanKey(buildContext.getPlanKey()));
            artifactManager.publish(buildLogger, buildContext.getPlanResultKey(),
                    repository.getSourceCodeDirectory(buildContext.getPlanResultKey().getPlanKey()), artifact, false,
                    1);
        }
        return buildContext;
    }

    public void setArtifactManager(ArtifactManager artifactManager) {
        this.artifactManager = artifactManager;
    }

    public void setBuildLoggerManager(BuildLoggerManager buildLoggerManager) {
        this.buildLoggerManager = buildLoggerManager;
    }
}
