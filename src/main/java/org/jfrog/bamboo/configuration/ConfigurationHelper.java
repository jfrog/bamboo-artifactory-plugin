package org.jfrog.bamboo.configuration;

import com.atlassian.spring.container.ContainerManager;
import org.jfrog.bamboo.builder.BambooUtilsHelper;
import org.jfrog.bamboo.util.ConstantValues;

import java.util.Map;

/**
 * A helper class to be used for the Artifactory tasks configuration.
 */
public class ConfigurationHelper {
    public static BuildJdkOverride getBuildJdkOverride(String planKey) {
        BambooUtilsHelper helper = (BambooUtilsHelper) ContainerManager.getComponent(
                ConstantValues.ARTIFACTORY_BAMBOO_UTILS_HELPER_KEY);
        Map<String, String> variables = helper.getAllVariables(planKey);

        BuildJdkOverride override = new BuildJdkOverride();
        override.setOverride(Boolean.valueOf(variables.get(BuildJdkOverride.SHOULD_OVERRIDE_JDK_KEY)));
        String envVar = variables.get(BuildJdkOverride.OVERRIDE_JDK_ENV_VAR_KEY);
        override.setOverrideWithEnvVarName(envVar == null ? "JAVA_HOME" : envVar);

        return override;
    }
}
