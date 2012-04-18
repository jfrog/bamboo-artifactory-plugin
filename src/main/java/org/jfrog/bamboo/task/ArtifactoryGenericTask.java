package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.artifact.AbstractArtifactManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContext;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FileSet;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.GenericBuildInfoHelper;
import org.jfrog.bamboo.util.version.ScmHelper;
import org.jfrog.build.api.Build;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.util.PublishedItemsHelper;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_COLLECTION_ACTIVATED_PARAM;
import static org.jfrog.bamboo.util.ConstantValues.BUILD_RESULT_SELECTED_SERVER_PARAM;

/**
 * @author Tomer Cohen
 */
public class ArtifactoryGenericTask implements TaskType {
    private static final Logger log = Logger.getLogger(ArtifactoryGenericTask.class);
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private BuildLogger logger;
    private GenericBuildInfoHelper buildInfoHelper;

    public ArtifactoryGenericTask(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
    }

    @Override
    @NotNull
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        logger = taskContext.getBuildLogger();
        if (!taskContext.isFinalising()) {
            log.error(logger.addErrorLogEntry("Artifactory Generic Task must run as a final Task!"));
            return TaskResultBuilder.create(taskContext).success().build();
        }
        BuildContext context = taskContext.getBuildContext();
        CurrentBuildResult result = context.getBuildResult();
        // if build wasn't a success don't do anything
        if (result.getBuildState().equals(BuildState.FAILED) || !result.getBuildErrors().isEmpty()) {
            log.error(logger.addErrorLogEntry("Build failed, not deploying to Artifactory."));
            return TaskResultBuilder.create(taskContext).success().build();
        }
        GenericContext genericContext = new GenericContext(taskContext.getConfigurationMap());
        Map<String, String> env = Maps.newHashMap();
        env.putAll(environmentVariableAccessor.getEnvironment(taskContext));
        env.putAll(environmentVariableAccessor.getEnvironment());
        String vcsRevision = ScmHelper.getRevisionKey(context);
        if (StringUtils.isBlank(vcsRevision)) {
            vcsRevision = "";
        }
        buildInfoHelper = new GenericBuildInfoHelper(env, vcsRevision);
        buildInfoHelper.init(context);
        try {
            File sourceCodeDirectory = getWorkingDirectory(context, taskContext);
            if (sourceCodeDirectory == null) {
                log.error(logger.addErrorLogEntry("No build directory found!"));
                return TaskResultBuilder.create(taskContext).success().build();
            }
            Multimap<String, FileSet> fileSetMap = buildTargetPathToFiles(sourceCodeDirectory, genericContext);
            if (!fileSetMap.isEmpty()) {
                deploy(fileSetMap, genericContext, taskContext, sourceCodeDirectory);
            }
        } catch (Exception e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.create(taskContext).failedWithError().build();
        }
        Map<String, String> customBuildData = result.getCustomBuildData();
        if (!customBuildData.containsKey(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) {
            customBuildData.put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        }
        return TaskResultBuilder.create(taskContext).success().build();
    }

    private File getWorkingDirectory(BuildContext context, TaskContext taskContext) throws RepositoryException {
        File checkoutDir = ScmHelper.getCheckoutDirectory(context);
        if (checkoutDir != null) {
            return checkoutDir;
        } else {
            return taskContext.getWorkingDirectory();
        }
    }

    private Multimap<String, FileSet> buildTargetPathToFiles(File directory, GenericContext context)
            throws IOException {
        Multimap<String, FileSet> result = HashMultimap.create();
        String deployPattern = context.getDeployPattern();
        deployPattern = StringUtils.replace(deployPattern, "\r\n", "\n");
        deployPattern = StringUtils.replace(deployPattern, ",", "\n");
        Multimap<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(deployPattern);
        if (pairs.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : pairs.entries()) {
            File scanningDir = directory;
            File dir = new File(entry.getKey());
            if (dir.isAbsolute()) {
                scanningDir = getRootDir(dir);
            }
            ArtifactDefinitionContext definitionContext =
                    createArtifactDefinitionContext(entry.getKey(), directory.getCanonicalPath(), scanningDir);
            FileSet fileSet = AbstractArtifactManager.createFileSet(scanningDir, definitionContext, false);
            if (fileSet != null) {
                log.info(logger.addBuildLogEntry(
                        "For pattern: " + entry.getKey() + " " + fileSet.size() + " artifacts were found"));
                result.put(entry.getValue(), fileSet);
            } else {
                log.warn(logger.addBuildLogEntry("For pattern: " + entry.getKey() + " no artifacts were found"));
            }
        }
        return result;
    }

    private File getRootDir(File directory) {
        File rootDir = directory;
        while (directory.getParentFile() != null) {
            rootDir = directory.getParentFile();
            directory = rootDir;
        }
        return rootDir;
    }

    private ArtifactDefinitionContext createArtifactDefinitionContext(String filePattern, String rootLocation,
            File dir) {
        ArtifactDefinitionContext context = new ArtifactDefinitionContextImpl();
        boolean absolute = new File(filePattern).isAbsolute();
        if (!absolute) {
            context.setLocation(rootLocation);
        }
        if (absolute) {
            filePattern = sanitizeFilePattern(filePattern, dir);
        }
        context.setCopyPattern(filePattern);
        context.setSharedArtifact(false);
        return context;
    }

    private String sanitizeFilePattern(String filePattern, File dir) {
        String pathToRemove = FilenameUtils.separatorsToUnix(dir.getAbsolutePath());
        filePattern = FilenameUtils.separatorsToUnix(filePattern);
        filePattern = StringUtils.removeStart(filePattern, pathToRemove);
        return filePattern;
    }

    private void deploy(Multimap<String, FileSet> fileSetMap, GenericContext context, TaskContext taskContext,
            File rootDir) throws IOException, NoSuchAlgorithmException {

        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(context.getSelectedServerId());
        String username = context.getUsername();
        if (StringUtils.isBlank(username)) {
            username = serverConfig.getUsername();
        }
        String password = context.getPassword();
        if (StringUtils.isBlank(password)) {
            password = serverConfig.getPassword();
        }
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(serverConfig.getUrl(), username, password, new BambooBuildInfoLog(log));
        try {
            BuildContext buildContext = taskContext.getBuildContext();
            Build build = buildInfoHelper.extractBuildInfo(buildContext, username);
            Set<DeployDetails> details = buildInfoHelper.createDeployDetailsAndAddToBuildInfo(build, fileSetMap,
                    rootDir, buildContext, context);
            for (DeployDetails detail : details) {
                StringBuilder deploymentPathBuilder = new StringBuilder(serverConfig.getUrl());
                deploymentPathBuilder.append("/").append(detail.getTargetRepository());
                if (!detail.getArtifactPath().startsWith("/")) {
                    deploymentPathBuilder.append("/");
                }
                deploymentPathBuilder.append(detail.getArtifactPath());
                logger.addBuildLogEntry(("Deploying artifact: " + deploymentPathBuilder.toString()));
                client.deployArtifact(detail);
            }
            String url = serverConfig.getUrl() + "/api/build";
            logger.addBuildLogEntry("Deploying build info to: " + url);
            client.sendBuildInfo(build);
            buildContext.getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM,
                    serverConfig.getUrl());
        } finally {
            client.shutdown();
        }
    }
}
