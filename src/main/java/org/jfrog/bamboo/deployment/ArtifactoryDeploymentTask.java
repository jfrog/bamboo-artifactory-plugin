package org.jfrog.bamboo.deployment;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.runtime.RuntimeTaskDefinition;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.util.BambooBuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bamboo deployment Artifactory task - Takes pre defined artifacts from a build plan and deploy it to Artifactory
 *
 * @author Aviad Shikloshi
 */
public class ArtifactoryDeploymentTask implements DeploymentTaskType {

    /* This is the name of the "Download Artifacts" task in bamboo , we are looking it up as downloading artifacts is a pre condition to our task */
    private static final String DOWNLOAD_ARTIFACTS_TASK_KEY = "com.atlassian.bamboo.plugins.bamboo-artifact-downloader-plugin:artifactdownloadertask";
    private static final String PATH_KEY = "localPath";
    private static final Logger log = Logger.getLogger(ArtifactoryDeploymentTask.class);

    private BuildLogger buildLogger;

    @NotNull
    @Override
    public TaskResult execute(@NotNull DeploymentTaskContext deploymentTaskContext) throws TaskException {

        buildLogger = deploymentTaskContext.getBuildLogger();
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        final String serverId = deploymentTaskContext.getConfigurationMap().get("artifactoryServerId");
        final ServerConfig serverConfig = serverConfigManager.getServerConfigById(Long.parseLong(serverId));
        if (serverConfig == null) {
            buildLogger.addErrorLogEntry("Please check Artifactory server configuration in the job configuration.");
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }

        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(serverConfig.getUrl(),
                serverConfig.getUsername(), serverConfig.getPassword(), new BambooBuildInfoLog(log));
        String matrixParamStr = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.MATRIX_PARAM);
        String repositoryKey = deploymentTaskContext.getConfigurationMap().get(ArtifactoryDeploymentConfiguration.DEPLOYMENT_REPOSITORY);

        File deploymentProjectDirectory = deploymentTaskContext.getRootDirectory();
        // Looking for the "Download Artifacts" task so we know where to look all of the shared artifacts to we want to deploy
        RuntimeTaskDefinition artifactsDownloadTask = getDownloadArtifactsTask(deploymentTaskContext.getCommonContext().getRuntimeTaskDefinitions());

        TaskResult result;
        Set<DeployDetails> deployDetailsForAllArtifacts;
        try {
            deployDetailsForAllArtifacts = getArtifactsFromSubDirectories(matrixParamStr, repositoryKey, deploymentProjectDirectory, artifactsDownloadTask);
            try {
                deploy(client, deployDetailsForAllArtifacts);
                result = TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
            } catch (Exception e) {
                buildLogger.addErrorLogEntry("Error while deploying artifacts to Artifactory: " + e.getMessage());
                result = TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
            }
        } catch (Exception e) {
            buildLogger.addBuildLogEntry("Error while preparing artifacts to deploy: " + e.getMessage());
            result = TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
        client.shutdown();
        return result;
    }

    /**
     * This function get all artifacts that are listed inside the root directory and not in a sub directory
     * We are passing in the list of pre configured sub directory so there will be no duplication or infinite loop
     *
     * @param rootDirectory  of the deployment project
     * @param subDirectories list of sub directories names
     * @param repoKey        the repo key to deploy to
     * @param matrixParamStr matrix param string configured in the UI
     * @return deploy details for all artifacts that are not inside a sub directory
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private Set<DeployDetails> getArtifactsFromRootDirectory(File rootDirectory, List<String> subDirectories, String repoKey, String matrixParamStr) throws IOException, NoSuchAlgorithmException {
        Set<DeployDetails> deployDetailsSet = Sets.newHashSet();
        for (File file : rootDirectory.listFiles()) {
            if (!subDirectories.contains(file.getName())) {
                deployDetailsSet.addAll(createDeployDetailsForDirectory(file, repoKey, matrixParamStr));
            }
        }
        return deployDetailsSet;
    }

    private Set<DeployDetails> getArtifactsFromSubDirectories(String matrixParamStr, String repositoryKey, File deploymentProjectDirectory,
                                                              RuntimeTaskDefinition artifactsDownloadTask) throws IOException, NoSuchAlgorithmException {
        if (artifactsDownloadTask == null) {
            throw new IllegalStateException("\"Artifact Download\" task must run prior to the \"Artifactory Deployment\" task.");
        }

        Set<DeployDetails> allArtifactsDetails = Sets.newHashSet();
        List<String> listOfSubDirs = collectAllSubDirectoriesWithSharedArtifacts(artifactsDownloadTask);

        List<File> allSubDirectoriesOfSharedArtifacts = getAllDirectoriesFromArtifactDownloadTask(deploymentProjectDirectory, listOfSubDirs);
        for (File dir : allSubDirectoriesOfSharedArtifacts) {
            for (File artifactRoot : dir.listFiles()) {
                allArtifactsDetails.addAll(createDeployDetailsForDirectory(artifactRoot, repositoryKey, matrixParamStr));
            }
        }

        allArtifactsDetails.addAll(getArtifactsFromRootDirectory(deploymentProjectDirectory, listOfSubDirs, repositoryKey, matrixParamStr));
        return allArtifactsDetails;
    }

    private void deploy(ArtifactoryBuildInfoClient client, Set<DeployDetails> artifactsDeployDetails) throws IOException {
        for (DeployDetails deployDetails : artifactsDeployDetails) {
            client.deployArtifact(deployDetails);
            buildLogger.addBuildLogEntry("Deployed: " + deployDetails.getArtifactPath() + "/" + deployDetails.getFile().getName()
                    + " to: " + deployDetails.getTargetRepository());
        }
    }

    /**
     * Collect the artifacts from the chosen root directory and create DeploymentDetails
     *
     * @param repositoryKey          the repository of which we are uploading the artifacts
     * @param artifactsRootDirectory the directory that we will scan to collect the artifacts
     * @return DeployDetails object for each artifact we are deploying
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private Set<DeployDetails> createDeployDetailsForDirectory(File artifactsRootDirectory, String repositoryKey, String matrixParamStr) throws IOException, NoSuchAlgorithmException {

        String artifactsRootPath = artifactsRootDirectory.getPath();
        Set<DeployDetails> deployDetailsSet = Sets.newHashSet();
        ArrayListMultimap<String, String> matrixParam = TaskUtils.extractMatrixParamFromString(matrixParamStr);

        // Scan recursively the root directory and collect all artifacts as they are described in the associated build plan
        Iterator<File> fileIterator = FileUtils.iterateFiles(artifactsRootDirectory, null, true);
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            if (file.isFile()) {
                DeployDetails deployDetailsBuilder = createDeployDetailsForOneArtifact(repositoryKey, artifactsRootPath, matrixParam, file);
                deployDetailsSet.add(deployDetailsBuilder);
            }
        }
        return deployDetailsSet;
    }

    /**
     * Create DeploymentDetails for artifact
     *
     * @param repositoryKey     repository to upload artifact to
     * @param artifactsRootPath root path on file system to all artifacts
     * @param artifact          artifact file object
     * @return DeploymentDetails for artifact
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private DeployDetails createDeployDetailsForOneArtifact(String repositoryKey, String artifactsRootPath, ArrayListMultimap<String, String> matrixParam,
                                                            File artifact) throws NoSuchAlgorithmException, IOException {
        DeployDetails.Builder deployDetailsBuilder = new DeployDetails.Builder();
        Map<String, String> checksum = FileChecksumCalculator.calculateChecksums(artifact, "SHA1", "MD5");
        deployDetailsBuilder
                .artifactPath(createArtifactPath(artifact.getPath(), artifactsRootPath))
                .file(artifact)
                .targetRepository(repositoryKey)
                .sha1(checksum.get("SHA1"))
                .md5(checksum.get("MD5"))
                .addProperties(matrixParam);
        return deployDetailsBuilder.build();
    }

    /**
     * Collect all sub directories of our shared artifacts from the root directory
     *
     * @param rootDir       root directory
     * @param listOfSubDirs list of relevant sub directories names - we will add the directory only if it name exist in this list
     * @return list of directories, each one will get its artifacts deployed
     */
    private List<File> getAllDirectoriesFromArtifactDownloadTask(@NotNull File rootDir, List<String> listOfSubDirs) {
        List<File> sharedArtifactsSubDirectories = Lists.newArrayList();
        for (File file : rootDir.listFiles()) {
            if (listOfSubDirs.contains(file.getName())) {
                sharedArtifactsSubDirectories.add(file);
            }
        }
        return sharedArtifactsSubDirectories;
    }

    /**
     * Download Artifacts task can configure several sub directories for the artifacts that had been downloaded
     * This method will collect each of the sub directories
     *
     * @param runtimeTaskDefinition is the Download Artifacts task configuration
     * @return list of all of the sub directories
     */
    private List<String> collectAllSubDirectoriesWithSharedArtifacts(RuntimeTaskDefinition runtimeTaskDefinition) {
        List<String> listOfSubDirs = Lists.newArrayList();
        if (runtimeTaskDefinition != null) {
            Map<String, String> downloadTaskConfiguration = runtimeTaskDefinition.getConfiguration();
            for (String s : downloadTaskConfiguration.keySet()) {
                // Each PATH_KEY_{number} in ArtifactoryDownload configuration map holds the directory path as the value,
                // we want to collect all of the sub directories.
                if (s.startsWith(PATH_KEY)) {
                    listOfSubDirs.add(downloadTaskConfiguration.get(s));
                }
            }
        }
        return listOfSubDirs;
    }

    /**
     * Searching for the "Artifacts Download" task in a list of tasks
     *
     * @param runtimeTaskDefinitionList all the job tasks
     * @return Artifacts Download task, null if no such task exists
     */
    @Nullable
    private RuntimeTaskDefinition getDownloadArtifactsTask(@NotNull List<RuntimeTaskDefinition> runtimeTaskDefinitionList) {
        for (RuntimeTaskDefinition rtd : runtimeTaskDefinitionList) {
            if (rtd.getPluginKey().equals(DOWNLOAD_ARTIFACTS_TASK_KEY)) {
                return rtd;
            }
        }
        return null;
    }

    /**
     * Create the artifact path in artifactory
     *
     * @param artifactPath full artifact path on file system
     * @param rootPath     root path for all artifacts
     * @return the calculated path to deploy to inside the repository
     */
    private String createArtifactPath(String artifactPath, String rootPath) {
        int start = rootPath.length() + 1;
        return StringUtils.substring(artifactPath, start).replace("\\", "/");
    }
}
