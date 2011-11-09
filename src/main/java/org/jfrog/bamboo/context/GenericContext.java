package org.jfrog.bamboo.context;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
public class GenericContext {
    public static final String SELECTED_SERVER_ID = "artifactory.generic.artifactoryServerId";
    public static final String REPO_KEY = "artifactory.generic.deployableRepo";
    public static final String USERNAME = "artifactory.generic.username";
    public static final String PASSWORD = "artifactory.generic.password";
    public static final String DEPLOY_PATTERN = "artifactory.generic.deployPattern";

    private final Map<String, String> env;

    public GenericContext(Map<String, String> env) {
        this.env = env;
    }

    public long getSelectedServerId() {
        String serverId = env.get(SELECTED_SERVER_ID);
        if (StringUtils.isBlank(serverId)) {
            return -1;
        }
        return Long.parseLong(serverId);
    }

    public String getRepoKey() {
        return env.get(REPO_KEY);
    }

    public String getUsername() {
        return env.get(USERNAME);
    }

    public String getPassword() {
        return env.get(PASSWORD);
    }

    public String getDeployPattern() {
        return env.get(DEPLOY_PATTERN);
    }

    public static Set<String> getFieldsToCopy() {
        return Sets.newHashSet(SELECTED_SERVER_ID, REPO_KEY, USERNAME, PASSWORD, DEPLOY_PATTERN);
    }
}
