package it.org.jfrog.bamboo.prehook;

import com.atlassian.core.util.FileUtils;
import it.org.jfrog.bamboo.IntegrationTestsBase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfrog.bamboo.capability.GradleCapabilityHelper;
import org.jfrog.bamboo.capability.Maven3CapabilityHelper;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static it.org.jfrog.bamboo.utils.Utils.*;

/**
 * Creates Bamboo remote agent before integration tests.
 * Stops the Bamboo remote agent after integration tests.
 *
 * @author yahavi
 */
public class RemoteAgent {
    private static final Logger log = LogManager.getLogger(RemoteAgent.class);

    private static final Path AGENT_INSTALLER = Paths.get("webapps", "bamboo", "WEB-INF", "classes", "agent-installer.jar");
    private static final String GRADLE_CAPABILITY_KEY = GradleCapabilityHelper.KEY + ".Gradle";
    private static final String MAVEN_CAPABILITY_KEY = Maven3CapabilityHelper.KEY + ".Maven";

    enum AgentCommand {
        START,
        STOP
    }

    static void startAgent() throws IOException, InterruptedException {
        Path agentHome = createAgentHome();
        createAgentWrapperConf(agentHome);
        createCapabilitiesProperties(agentHome);

        String javaExe = getJavaExe();
        CommandResults results = runAgent(javaExe, agentHome, AgentCommand.START);
        if (!results.isOk()) {
            throw new IOException("Remote agent failed to start: " + results.getErr());
        }
        log.info("Remote agent started at: " + agentHome.toAbsolutePath().toString());

        // Kill thread before the tomcat process stops
        Runtime.getRuntime().addShutdownHook(new Thread("Stop Bamboo remote agent") {
            @Override
            public void run() {
                try {
                    CommandResults results = runAgent(javaExe, agentHome, AgentCommand.STOP);
                    if (!results.isOk()) {
                        throw new IOException(results.getErr());
                    }
                    log.info("Remote agent stopped");
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException("Failed to stop agent", e);
                }
            }
        });
    }

    /**
     * Create the remote agent home dir.
     *
     * @return remote agent home dir path
     * @throws IOException in case of create directory failed.
     */
    private static Path createAgentHome() throws IOException {
        File agentHomeDir = new File("bamboo-agent-home");
        FileUtils.recursiveDelete(agentHomeDir);
        return Files.createDirectory(agentHomeDir.toPath());
    }

    /**
     * Create the wrapper.conf file of the agent.
     * This file contains agent configuration such as the agent home and the Java heap size.
     *
     * @param agentHome - The agent home
     * @throws IOException if one of the resources are missing
     */
    private static void createAgentWrapperConf(Path agentHome) throws IOException {
        StrSubstitutor wrapperConfSubstitutor = new StrSubstitutor(new HashMap<String, String>() {{
            put("BAMBOO_AGENT_HOME", agentHome.toAbsolutePath().toString());
            put("JAVA_INIT_MEMORY_MB", "512");
            put("JAVA_MAX_MEMORY_MB", "1024");
        }});
        Path agentConfDir = Files.createDirectory(agentHome.resolve("conf"));
        try (InputStream is = IntegrationTestsBase.class.getClassLoader().getResourceAsStream("bamboo-agent-wrapper.conf")) {
            if (is == null) {
                throw new IOException("Couldn't find bamboo-agent-wrapper.conf");
            }
            String wrapperConf = IOUtils.toString(is, StandardCharsets.UTF_8);
            Files.write(agentConfDir.resolve("wrapper.conf"), wrapperConfSubstitutor.replace(wrapperConf).getBytes());
        }
    }

    /**
     * Create the bamboo-capabilities.properties file of the agent.
     * This file contains the agent capabilities.
     *
     * @param agentHome - The agent home
     * @throws IOException if one of the resources are missing
     */
    private static void createCapabilitiesProperties(Path agentHome) throws IOException {
        Path agentBinDir = Files.createDirectory(agentHome.resolve("bin"));
        Properties bambooCapabilitiesProps = new Properties();
        bambooCapabilitiesProps.setProperty(MAVEN_CAPABILITY_KEY, MAVEN_HOME);
        bambooCapabilitiesProps.setProperty(GRADLE_CAPABILITY_KEY, GRADLE_HOME);
        try (OutputStream os = new FileOutputStream(agentBinDir.resolve("bamboo-capabilities.properties").toFile())) {
            bambooCapabilitiesProps.store(os, "Bamboo remote agent capabilities");
        }
    }

    /**
     * Get absolute path to Java executable.
     *
     * @return path to Java executable
     */
    private static String getJavaExe() {
        String javaExe = Paths.get(System.getenv("JAVA_HOME"), "bin", "java").toAbsolutePath().toString();
        if (SystemUtils.IS_OS_WINDOWS) {
            javaExe += ".exe";
        }
        return javaExe;
    }

    /**
     * Start or stop Bamboo remote agent.
     *
     * @param javaExe   - Path to Java executable
     * @param agentHome - The agent home dir
     * @param command   - "start" or "stop"
     * @return the command results
     * @throws IOException          If there was an error during the execution
     * @throws InterruptedException If there was an error during the execution
     */
    private static CommandResults runAgent(String javaExe, Path agentHome, AgentCommand command) throws IOException, InterruptedException {
        CommandExecutor executor = new CommandExecutor(javaExe, System.getenv());
        List<String> args = new ArrayList<String>() {{
            add("-jar");
            add("-Dbamboo.home=" + agentHome.toAbsolutePath().toString());
            add(AGENT_INSTALLER.toAbsolutePath().toString());
            add(BAMBOO_TEST_URL + "/agentServer/");
            add(command.toString().toLowerCase());
        }};
        return executor.exeCommand(null, args, null, null);
    }
}
