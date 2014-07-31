package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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

        String vcsUrl = ScmHelper.getVcsUrl(context);
        if (StringUtils.isBlank(vcsUrl)) {
            vcsUrl = "";
        }

        buildInfoHelper = new GenericBuildInfoHelper(env, vcsRevision, vcsUrl);
        buildInfoHelper.init(context);
        try {
            File sourceCodeDirectory = getWorkingDirectory(context, taskContext);
            if (sourceCodeDirectory == null) {
                log.error(logger.addErrorLogEntry("No build directory found!"));
                return TaskResultBuilder.create(taskContext).success().build();
            }
            Multimap<String, File> filesMap = buildTargetPathToFiles(sourceCodeDirectory, genericContext);
            if (!filesMap.isEmpty()) {
                deploy(filesMap, genericContext, taskContext, sourceCodeDirectory);
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

    private Multimap<String, File> buildTargetPathToFiles(File directory, GenericContext context)
            throws IOException {
        Multimap<String, File> result = HashMultimap.create();
        String deployPattern = context.getDeployPattern();
        deployPattern = StringUtils.replace(deployPattern, "\r\n", "\n");
        deployPattern = StringUtils.replace(deployPattern, ",", "\n");

        // Collect all published items pair in the form of 'pattern -> targetPath'
        Multimap<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(deployPattern);
        if (pairs.isEmpty()) {
            return result;
        }

        // Collect all found items and put them into the result in the form of 'targetPath -> File'
        for (Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> filesMap = PublishedItemsHelper.buildPublishingData(directory, entry.getKey(),
                    entry.getValue());
            if (filesMap != null) {
                log.info(logger.addBuildLogEntry(
                        "For pattern: " + entry.getKey() + " " + filesMap.size() + " artifacts were found"));
                result.putAll(filesMap);
            } else {
                log.warn(logger.addBuildLogEntry("For pattern: " + entry.getKey() + " no artifacts were found"));
            }
        }

        return result;
    }

    private void deploy(Multimap<String, File> filesMap, GenericContext context, TaskContext taskContext,
                        File rootDir) throws IOException, NoSuchAlgorithmException {

        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(context.getSelectedServerId());
        String username = serverConfigManager.substituteVariables(context.getUsername());
        if (StringUtils.isBlank(username)) {
            username = serverConfigManager.substituteVariables(serverConfig.getUsername());
        }
        String password = serverConfigManager.substituteVariables(context.getPassword());
        if (StringUtils.isBlank(password)) {
            password = serverConfigManager.substituteVariables(serverConfig.getPassword());
        }
        String serverUrl = serverConfigManager.substituteVariables(serverConfig.getUrl());
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(serverUrl, username, password, new BambooBuildInfoLog(log));
        try {
            BuildContext buildContext = taskContext.getBuildContext();
            Build build = buildInfoHelper.extractBuildInfo(buildContext, taskContext.getBuildLogger(), context, username);
            Set<DeployDetails> details = buildInfoHelper.createDeployDetailsAndAddToBuildInfo(build, filesMap,
                    rootDir, buildContext, context);
            for (DeployDetails detail : details) {
                StringBuilder deploymentPathBuilder = new StringBuilder(serverUrl);
                deploymentPathBuilder.append("/").append(detail.getTargetRepository());
                if (!detail.getArtifactPath().startsWith("/")) {
                    deploymentPathBuilder.append("/");
                }
                deploymentPathBuilder.append(detail.getArtifactPath());
                logger.addBuildLogEntry(("Deploying artifact: " + deploymentPathBuilder.toString()));
                client.deployArtifact(detail);
            }
            if (context.isPublishBuildInfo()) {
                String url = serverUrl + "/api/build";
                logger.addBuildLogEntry("Deploying build info to: " + url);
                client.sendBuildInfo(build);
                buildContext.getBuildResult().getCustomBuildData().put(BUILD_RESULT_SELECTED_SERVER_PARAM, serverUrl);
            }
        } finally {
            client.shutdown();
        }
    }
}
