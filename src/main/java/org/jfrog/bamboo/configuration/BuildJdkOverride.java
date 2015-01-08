package org.jfrog.bamboo.configuration;

/**
 * It is possible to control the value of the Build JDK defined for Artifactory Gradle, Maven and Ivy tasks
 * by defining specific Bamboo variables.
 * This class is used to represent the task JDK override details.
 */
public class BuildJdkOverride {
    /**
     * The Bamboo variable name used to indicate whether the task Build JDK should be override. The Bamboo
     * variable value should be true or false.
     */
    public static final String SHOULD_OVERRIDE_JDK_KEY = "artifactory.task.override.jdk";
    /**
     * The bamboo variable name used to indicate the environment variable name that contains the path to the JDK.
     * If this variable is not defined, the path is taken from the JAVA_HOME environment variable.
     */
    public static final String OVERRIDE_JDK_ENV_VAR_KEY = "artifactory.task.override.jdk.env.var";

    /**
     * Should the build JDK should be overridden with the value defined in an environment variable.
     */
    private boolean override;
    /**
     * The name of environment variable containing the path to the build JDK.
     */
    private String overrideWithEnvVarName;

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

    public String getOverrideWithEnvVarName() {
        return overrideWithEnvVarName;
    }

    public void setOverrideWithEnvVarName(String overrideWithEnvVarName) {
        this.overrideWithEnvVarName = overrideWithEnvVarName;
    }
}
