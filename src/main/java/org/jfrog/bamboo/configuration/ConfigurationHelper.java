package org.jfrog.bamboo.configuration;

import com.atlassian.spring.container.ContainerManager;
import org.jfrog.bamboo.builder.BambooUtilsHelper;
import org.jfrog.bamboo.util.ConstantValues;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.Map;

/**
 * A helper class to be used for the Artifactory tasks configuration.
 */
public class ConfigurationHelper {
    public static BuildJdkOverride getBuildJdkOverride(String planKey) {
        BambooUtilsHelper helper = getBambooUtilsHelper();
        Map<String, String> variables = helper.getAllVariables(planKey);

        BuildJdkOverride override = new BuildJdkOverride();
        override.setOverride(Boolean.valueOf(variables.get(BuildJdkOverride.SHOULD_OVERRIDE_JDK_KEY)));
        String envVar = variables.get(BuildJdkOverride.OVERRIDE_JDK_ENV_VAR_KEY);
        override.setOverrideWithEnvVarName(envVar == null ? "JAVA_HOME" : envVar);

        return override;
    }

    public static BambooUtilsHelper getBambooUtilsHelper() {
        RuntimeException exception = null;
        for(int i=0; i < 120; i++) {
            try {
                BambooUtilsHelper helper = (BambooUtilsHelper) ContainerManager.getComponent(
                        ConstantValues.ARTIFACTORY_BAMBOO_UTILS_HELPER_KEY);
                return helper;
            } catch (NoSuchBeanDefinitionException e) {
                exception = e;
                // The container might not have been initialized yet.
                // Wait for it to be ready.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1.getMessage());
                }
            }
        }

        throw exception;
    }
}
