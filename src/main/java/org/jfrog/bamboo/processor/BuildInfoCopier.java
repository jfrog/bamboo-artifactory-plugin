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
import com.google.common.io.ByteStreams;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.release.provider.TokenDataProvider;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.bamboo.util.version.VcsHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Copy the {@code build-info.json} file to the artifacts folder of the build, this will be later used for parsing the
 * modules for release management. This applies only to {@link org.jfrog.bamboo.task.ArtifactoryMaven3Task} type
 * builds.
 *
 * @author Tomer Cohen
 */
public class BuildInfoCopier extends AbstractBuildTask implements CustomBuildProcessor {
    private static final Logger log = Logger.getLogger(BuildInfoCopier.class);

    private volatile ArtifactManager artifactManager;
    private BuildLoggerManager buildLoggerManager;

    @Override
    @NotNull
    public BuildContext call() throws Exception {
        PlanResultKey planResultKey = buildContext.getPlanResultKey();
        BuildLogger buildLogger = buildLoggerManager.getLogger(planResultKey);
        File checkoutDir = VcsHelper.getCheckoutDirectory(buildContext);
        if (checkoutDir == null) {
            return buildContext;
        }
        List<TaskDefinition> taskDefinitions = buildContext.getBuildDefinition().getTaskDefinitions();
        TaskDefinition mavenDefinition = TaskDefinitionHelper.findMavenDefinition(taskDefinitions);
        if (mavenDefinition == null) {
            log.debug("No Maven task definition found");
            return buildContext;
        }
        Maven3BuildContext conf = new Maven3BuildContext(mavenDefinition.getConfiguration());
        String location = "target";
        String directory = conf.getWorkingSubDirectory();
        if (StringUtils.isNotBlank(directory)) {
            location = directory + "/" + location;
        }
        File buildInfo = new File(new File(checkoutDir, location), "build-info.json");
        if (buildInfo.exists()) {
            log.info(buildLogger.addBuildLogEntry("Copying the buildinfo artifacts for " +
                    "build: " + buildContext.getBuildResultKey()));

            TaskDefinition definition = TaskDefinitionHelper.findMavenDefinition(buildContext.getRuntimeTaskDefinitions());
            String securityToken = buildContext.getRuntimeTaskContext()
                    .getRuntimeContextForTask(definition)
                    .get(TokenDataProvider.SECURITY_TOKEN);

            ArtifactDefinitionContextImpl artifact = new ArtifactDefinitionContextImpl("buildInfo", false, SecureToken.createFromString(securityToken));
            File buildInfoZip = createBuildInfoZip(buildInfo);
            if (buildInfoZip != null) {
                artifact.setLocation(location);
                artifact.setCopyPattern(buildInfoZip.getName());
                Map<String, String> config = Maps.newHashMap();
                artifactManager.publish(buildLogger, planResultKey, checkoutDir, artifact, config, 1);
            }
        }
        return buildContext;
    }

    @Nullable
    private File createBuildInfoZip(File buildInfoFile) throws IOException {
        File buildInfoZipFile = new File(buildInfoFile.getParent(), "build-info.json.zip");
        if (!buildInfoZipFile.exists()) {
            if (!buildInfoZipFile.createNewFile()) {
                log.error("Unable to create build info archive: the file '" + buildInfoZipFile.getAbsolutePath() +
                        "' could not be created.");
                return null;
            }
        }
        FileInputStream buildInfoFileStream = null;
        GZIPOutputStream stream = null;
        try {
            buildInfoFileStream = new FileInputStream(buildInfoFile);
            stream = new GZIPOutputStream(new FileOutputStream(buildInfoZipFile));
            ByteStreams.copy(buildInfoFileStream, stream);
        } finally {
            if (buildInfoFileStream != null) {
                try {
                    buildInfoFileStream.close();
                } catch (IOException e) { // Ignore
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) { // Ignore
                }
            }
        }
        return buildInfoZipFile;
    }

    public void setArtifactManager(ArtifactManager artifactManager) {
        this.artifactManager = artifactManager;
    }

    public void setBuildLoggerManager(BuildLoggerManager buildLoggerManager) {
        this.buildLoggerManager = buildLoggerManager;
    }
}
