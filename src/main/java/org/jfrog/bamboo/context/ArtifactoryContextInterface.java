package org.jfrog.bamboo.context;

/**
 * @author Alexei Vainshtein
 */
public interface ArtifactoryContextInterface {

    String getUsername();
    String getPassword();
    long getArtifactoryServerId();
    boolean isIncludeEnvVars();
    String getRepoKey();
    String getEnvVarsExcludePatterns();
    String getEnvVarsIncludePatterns();
    boolean isCaptureBuildInfo();
}
