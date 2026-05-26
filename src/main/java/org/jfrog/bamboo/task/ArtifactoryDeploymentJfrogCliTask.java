package org.jfrog.bamboo.task;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.context.JfrogCliDeploymentContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deployment-project variant of the JFrog CLI task.
 *
 * <p>Allows running any {@code jf} CLI command from a Bamboo Deployment Project environment.
 * The selected JFrog server configuration (URL, credentials) is injected into the process
 * environment via the standard JFrog CLI environment variables so that the user-supplied
 * command does not need to embed credentials.
 *
 * <p>The {@code jf} binary must be pre-installed and available on the build agent's PATH.
 */
public class ArtifactoryDeploymentJfrogCliTask extends ArtifactoryDeploymentTaskType {

    /** Standard JFrog CLI environment variables for server authentication. */
    private static final String JF_ENV_URL = "JF_URL";
    private static final String JF_ENV_USER = "JF_USER";
    private static final String JF_ENV_PASSWORD = "JF_PASSWORD";

    private JfrogCliDeploymentContext cliContext;
    private ServerConfig serverConfig;

    @Override
    protected void initTask(@NotNull CommonTaskContext context) throws TaskException {
        super.initTask(context);
        cliContext = new JfrogCliDeploymentContext(context.getConfigurationMap());

        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        serverConfig = serverConfigManager.getServerConfigById(cliContext.getSelectedServerId());
        if (serverConfig == null) {
            throw new TaskException(
                    "JFrog CLI Deployment task: no Artifactory server is configured. " +
                    "Please select a server in the task configuration.");
        }
    }

    @NotNull
    @Override
    protected TaskResult runTask(@NotNull DeploymentTaskContext deploymentTaskContext) {
        String jfCommand = cliContext.getJfCommand();
        if (StringUtils.isBlank(jfCommand)) {
            buildInfoLog.error("JFrog CLI Deployment task: the 'JFrog CLI command' field is empty.");
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }

        try {
            int exitCode = executeJfCommand(jfCommand, deploymentTaskContext.getWorkingDirectory());
            if (exitCode != 0) {
                buildInfoLog.error("JFrog CLI command exited with code " + exitCode);
                return TaskResultBuilder.newBuilder(deploymentTaskContext).failed().build();
            }
            return TaskResultBuilder.newBuilder(deploymentTaskContext).success().build();
        } catch (InterruptedException e) {
            // Restore interrupt status so callers / Bamboo can react to the cancellation.
            Thread.currentThread().interrupt();
            buildInfoLog.error("JFrog CLI command was interrupted: " + e.getMessage(), e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        } catch (IOException e) {
            buildInfoLog.error("Exception while running JFrog CLI command: " + e.getMessage(), e);
            return TaskResultBuilder.newBuilder(deploymentTaskContext).failedWithError().build();
        }
    }

    /**
     * Executes the user-supplied jf CLI command as a subprocess.
     *
     * <p>Auth is injected through environment variables so the user never has to embed
     * credentials in the command string.
     */
    private int executeJfCommand(String jfCommand, File workingDir)
            throws IOException, InterruptedException {

        List<String> commandParts = buildCommandParts(jfCommand);
        buildInfoLog.info("Running: " + String.join(" ", commandParts));

        ProcessBuilder pb = new ProcessBuilder(commandParts);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        Map<String, String> env = new HashMap<>(pb.environment());
        env.put(JF_ENV_URL, serverConfig.getUrl());
        if (StringUtils.isNotBlank(serverConfig.getUsername())) {
            env.put(JF_ENV_USER, serverConfig.getUsername());
        }
        if (StringUtils.isNotBlank(serverConfig.getPassword())) {
            env.put(JF_ENV_PASSWORD, serverConfig.getPassword());
        }
        pb.environment().putAll(env);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.addBuildLogEntry(line);
            }
        }
        return process.waitFor();
    }

    /**
     * Splits the raw command string into tokens, prepending the "jf" binary.
     * The user enters the command WITHOUT the leading "jf", e.g. "rt dl --spec=spec.json".
     * If the user already prefixed "jf", the binary is still only added once.
     */
    private List<String> buildCommandParts(String jfCommand) {
        List<String> parts = new ArrayList<>();
        parts.add("jf");

        String trimmed = jfCommand.trim();
        // Strip redundant leading "jf" if the user included it
        if (trimmed.startsWith("jf ")) {
            trimmed = trimmed.substring(3).trim();
        }

        // Simple whitespace split; respects quoted args via shell on each OS
        for (String token : trimmed.split("\\s+")) {
            if (!token.isEmpty()) {
                parts.add(token);
            }
        }
        return parts;
    }

    @Override
    protected ServerConfig getUsageServerConfig() {
        return serverConfig;
    }

    @Override
    protected String getTaskUsageName() {
        return "deployment_jfrog_cli";
    }
}
