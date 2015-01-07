package org.jfrog.bamboo.configuration;

public class BuildJdkOverride {
    public static final String SHOULD_OVERRIDE_JDK_KEY = "artifactory.task.override.jdk";
    public static final String OVERRIDE_JDK_ENV_VAR_KEY = "artifactory.task.override.jdk.env.var";

    private boolean override;
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
