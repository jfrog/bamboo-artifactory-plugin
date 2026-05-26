package org.jfrog.bamboo.context;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * Configuration context for the JFrog CLI Deployment task.
 * Holds the server selection and the free-form jf CLI command entered by the user.
 */
public class JfrogCliDeploymentContext {

    public static final String PREFIX = "artifactory.jfrogCli.";
    public static final String SERVER_ID = PREFIX + "serverId";
    public static final String JF_COMMAND = PREFIX + "command";

    private static final Set<String> FIELDS_TO_COPY = Set.of(SERVER_ID, JF_COMMAND);

    private final Map<String, String> env;

    public JfrogCliDeploymentContext(Map<String, String> env) {
        this.env = env;
    }

    public long getSelectedServerId() {
        String serverId = env.get(SERVER_ID);
        if (StringUtils.isBlank(serverId)) {
            return -1;
        }
        try {
            return Long.parseLong(serverId);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getJfCommand() {
        return StringUtils.trimToEmpty(env.get(JF_COMMAND));
    }

    public static Set<String> getFieldsToCopy() {
        return FIELDS_TO_COPY;
    }
}
