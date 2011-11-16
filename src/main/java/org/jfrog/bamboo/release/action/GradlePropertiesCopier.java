package org.jfrog.bamboo.release.action;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.CustomBuildProcessor;
import com.atlassian.bamboo.build.artifact.ArtifactManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.task.AbstractBuildTask;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.version.ScmHelper;

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

    @Override
    @NotNull
    public BuildContext call() throws Exception {
        PlanResultKey planResultKey = buildContext.getPlanResultKey();
        BuildLogger buildLogger = buildLoggerManager.getBuildLogger(planResultKey);
        buildLogger.startStreamingBuildLogs(planResultKey);
        BuildDefinition definition = buildContext.getBuildDefinition();
        File checkoutDir = ScmHelper.getCheckoutDirectory(buildContext);
        if (checkoutDir == null) {
            return buildContext;
        }
        List<TaskDefinition> taskDefinitions = definition.getTaskDefinitions();
        TaskDefinition gradleDefinition = TaskDefinitionHelper.findGradleDefinition(taskDefinitions);
        if (gradleDefinition == null) {
            log.debug("Current build is not a gradle build");
            return buildContext;
        }
        if (checkoutDir.exists()) {
            log.info(buildLogger.addBuildLogEntry("Copying the gradle properties artifact for " +
                    "build: " + buildContext.getBuildResultKey()));
            ArtifactDefinitionContextImpl artifact = new ArtifactDefinitionContextImpl();
            artifact.setName("gradle");
            artifact.setLocation("");
            File gradleProps = new File(checkoutDir, "gradle.properties");
            artifact.setCopyPattern(gradleProps.getName());
            artifact.setProducerJobKey(PlanKeys.getPlanKey(buildContext.getPlanKey()));
            artifactManager.publish(buildLogger, planResultKey, checkoutDir, artifact, false, 1);
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
