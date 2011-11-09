package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.artifact.AbstractArtifactManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContext;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.repository.Repository;
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
import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.GenericBuildInfoHelper;
import org.jfrog.bamboo.util.generic.PublishedItemsHelper;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.DeployDetails;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
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

    @NotNull
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        logger = taskContext.getBuildLogger();
        if (!taskContext.isFinalising()) {
            log.error(logger.addErrorLogEntry("Artifactory Generic Task must run as a final Task!"));
            return TaskResultBuilder.create(taskContext).success().build();
        }
        CurrentBuildResult result = taskContext.getBuildContext().getBuildResult();
        // if build wasn't a success don't do anything
        if (result.getBuildState().equals(BuildState.FAILED) || !result.getBuildErrors().isEmpty()) {
            log.error(logger.addErrorLogEntry("Build failed, not deploying to Artifactory."));
            return TaskResultBuilder.create(taskContext).success().build();
        }
        GenericContext genericContext = new GenericContext(taskContext.getConfigurationMap());
        Map<String, String> env = Maps.newHashMap();
        env.putAll(environmentVariableAccessor.getEnvironment(taskContext));
        env.putAll(environmentVariableAccessor.getEnvironment());
        String vcsRevision = taskContext.getBuildContext().getBuildChanges().getVcsRevisionKey();
        if (StringUtils.isBlank(vcsRevision)) {
            vcsRevision = "";
        }
        buildInfoHelper = new GenericBuildInfoHelper(env, vcsRevision);
        buildInfoHelper.init(taskContext.getBuildContext());
        try {
            File sourceCodeDirectory = getWorkingDirectory(taskContext);
            if (sourceCodeDirectory == null) {
                log.error(logger.addErrorLogEntry("No build directory found!"));
                return TaskResultBuilder.create(taskContext).success().build();
            }
            Multimap<String, FileSet> fileSetMap = buildTargetPathToFiles(sourceCodeDirectory, genericContext);
            deploy(fileSetMap, genericContext, taskContext, sourceCodeDirectory);
        } catch (Exception e) {
            String message = "Exception occurred while executing task";
            logger.addErrorLogEntry(message, e);
            log.error(message, e);
            return TaskResultBuilder.create(taskContext).failedWithError().build();
        }
        BuildContext buildContext = taskContext.getBuildContext();
        Map<String, String> customBuildData = buildContext.getBuildResult().getCustomBuildData();
        if (!customBuildData.containsKey(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM)) {
            customBuildData.put(BUILD_RESULT_COLLECTION_ACTIVATED_PARAM, "true");
        }
        return TaskResultBuilder.create(taskContext).success().build();
    }

    private File getWorkingDirectory(TaskContext taskContext) throws RepositoryException {
        Repository repository = taskContext.getBuildContext().getBuildDefinition().getRepository();
        if (repository != null) {
            PlanKey key = PlanKeys.getPlanKey(taskContext.getBuildContext().getPlanKey());
            return repository.getSourceCodeDirectory(key);
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
        Map<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(deployPattern);
        if (pairs.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
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
            File rootDir)
            throws IOException, NoSuchAlgorithmException {
        if (fileSetMap.isEmpty()) {
            return;
        }
        Set<DeployDetails> details = Sets.newHashSet();
        for (Map.Entry<String, FileSet> entry : fileSetMap.entries()) {
            details.addAll(
                    buildDeployDetailsFromFileSet(entry.getValue(), context.getRepoKey(), entry.getKey(), rootDir
                    ));
        }
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
            Build build = buildInfoHelper.extractBuildInfo(taskContext.getBuildContext(), details, username);
            String url = serverConfig.getUrl() + "/api/build";
            logger.addBuildLogEntry("Deploying build info to: " + url);
            client.sendBuildInfo(build);
            taskContext.getBuildContext().getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM,
                    serverConfig.getUrl());
        } finally {
            client.shutdown();
        }
    }


    private Set<DeployDetails> buildDeployDetailsFromFileSet(FileSet fileSet, String targetRepository,
            String targetPath, File rootDir) throws IOException, NoSuchAlgorithmException {
        Set<DeployDetails> result = Sets.newHashSet();
        Iterator<FileResource> iterator = fileSet.iterator();
        while (iterator.hasNext()) {
            FileResource fileResource = iterator.next();
            File file = fileResource.getFile();

            String relativePath = file.getAbsolutePath();
            if (StringUtils.startsWith(relativePath, rootDir.getAbsolutePath())) {
                relativePath = StringUtils.removeStart(file.getAbsolutePath(), rootDir.getAbsolutePath());
            } else {
                File fileBaseDir = fileResource.getBaseDir();
                if (fileBaseDir != null) {
                    relativePath = StringUtils.removeStart(file.getAbsolutePath(), fileBaseDir.getAbsolutePath());
                }
            }
            relativePath = FilenameUtils.separatorsToUnix(relativePath);
            relativePath = StringUtils.removeStart(relativePath, "/");
            String path = PublishedItemsHelper.calculateTargetPath(relativePath, targetPath, file.getName());
            path = StringUtils.replace(path, "//", "/");
            Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(file, "SHA1", "MD5");
            DeployDetails.Builder deployDetails = new DeployDetails.Builder().file(file).md5(checksums.get("MD5"))
                    .sha1(checksums.get("SHA1")).targetRepository(targetRepository).artifactPath(path);
            buildInfoHelper.addCommonProperties(deployDetails);
            result.add(deployDetails.build());
        }
        return result;
    }
}
