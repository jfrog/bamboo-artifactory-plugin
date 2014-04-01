package org.jfrog.bamboo.release.scm.git;

/**
 * Taken from Atlassian Bamboo Git Plugin, in use by the release management for performing Git operations
 *
 * @author Shay Yaakov
 * @see <a href="https://github.com/atlassian/bamboo-git-plugin" >bamboo-git-plugin</a>
 */
public enum GitAuthenticationType {
    NONE,
    PASSWORD,
    SSH_KEYPAIR,
    SHARED_CREDENTIALS
}