package org.jfrog.bamboo.configuration;

import org.jfrog.bamboo.builder.BambooUtilsHelper;

import java.util.Map;

/**
 * Created by user on 07/01/2015.
 */
public class ConfigurationHelper {
    public static BuildJdkOverride getBuildJdkOverride(String planKey) {
        Map<String, String> variables = BambooUtilsHelper.getInstance().getAllVariables(planKey);

        BuildJdkOverride override = new BuildJdkOverride();
        override.setOverride(Boolean.valueOf(variables.get(BuildJdkOverride.SHOULD_OVERRIDE_JDK_KEY)));
        String envVar = variables.get(BuildJdkOverride.OVERRIDE_JDK_ENV_VAR_KEY);
        override.setOverrideWithEnvVarName(envVar == null ? "JAVA_HOME" : envVar);

        return override;
    }
}
