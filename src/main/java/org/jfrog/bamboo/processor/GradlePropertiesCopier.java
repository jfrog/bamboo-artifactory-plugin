package org.jfrog.bamboo.processor;

import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.CustomBuildProcessor;
import com.atlassian.bamboo.build.artifact.ArtifactManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.security.SecureToken;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.task.AbstractBuildTask;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.version.VcsHelper;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.util.Utils.getTaskSecurityToken;

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
    public BuildContext call() {
        PlanResultKey planResultKey = buildContext.getPlanResultKey();
        BuildLogger buildLogger = buildLoggerManager.getLogger(planResultKey);
        File checkoutDir = VcsHelper.getCheckoutDirectory(buildContext);
        if (checkoutDir == null) {
            return buildContext;
        }
        List<TaskDefinition> taskDefinitions = buildContext.getBuildDefinition().getTaskDefinitions();
        TaskDefinition gradleDefinition = TaskDefinitionHelper.findGradleDefinition(taskDefinitions);
        if (gradleDefinition == null) {
            log.debug("Current build is not a gradle build");
            return buildContext;
        }
        if (checkoutDir.exists()) {
            GradleBuildContext gradleBuildContext = (GradleBuildContext) PackageManagersContext.createContextFromMap(gradleDefinition.getConfiguration());
            String location = "";
            String directory = gradleBuildContext == null ? "" : gradleBuildContext.getBuildScript();
            if (StringUtils.isNotBlank(directory)) {
                location = directory;
            }

            File gradleProps = new File(new File(checkoutDir, location), "gradle.properties");
            if (gradleProps.exists()) {
                TaskDefinition definition = TaskDefinitionHelper.findGradleDefinition(buildContext.getRuntimeTaskDefinitions());
                String securityToken = getTaskSecurityToken(buildContext, definition);
                if (securityToken == null) {
                    log.error("Security token not found");
                    return buildContext;
                }

                ArtifactDefinitionContextImpl artifact = new ArtifactDefinitionContextImpl("gradle", false, SecureToken.createFromString(securityToken));
                artifact.setLocation(location);
                artifact.setCopyPattern(gradleProps.getName());
                Map<String, String> config = Maps.newHashMap();
                artifactManager.publish(buildLogger, planResultKey, checkoutDir, artifact, config, 1);
            }
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
