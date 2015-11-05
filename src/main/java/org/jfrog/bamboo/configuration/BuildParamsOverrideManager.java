package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * It is possible to control values of some build variables defined for Artifactory Gradle, Maven, Ivy and Generic tasks
 * by defining specific Bamboo variables for each variable
 * <p>
 * This class is used to mange overriding the supported parameters.
 */
public class BuildParamsOverrideManager {

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
    public static final String OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME = "artifactory.override.deployer.username";
    public static final String OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD = "artifactory.override.deployer.password";
    public static final String OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME = "artifactory.override.resolver.username";
    public static final String OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD = "artifactory.override.resolver.password";
    public static final String OVERRIDE_ARTIFACTORY_RESOLVE_REPO = "artifactory.override.resolve.repo";
    public static final String OVERRIDE_ARTIFACTORY_DEPLOY_REPO = "artifactory.override.deploy.repo";

    private static List<String> overrideKeys = Lists.newArrayList(
            OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME,
            OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD,
            OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME,
            OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD,
            OVERRIDE_ARTIFACTORY_RESOLVE_REPO,
            OVERRIDE_ARTIFACTORY_DEPLOY_REPO,
            OVERRIDE_JDK_ENV_VAR_KEY,
            SHOULD_OVERRIDE_JDK_KEY
    );


    private Map<String, String> overrideParams;

    public BuildParamsOverrideManager() {
        overrideParams = Maps.newHashMap();
    }

    public BuildParamsOverrideManager(CustomVariableContext customVariableContext) {
        this();
        initOverrideMapWithContext(customVariableContext);
    }

    /**
     * Takes a build context and init all potential override parameters with the values in context
     *
     * @param customVariableContext the current build variable context
     */
    private void initOverrideMapWithContext(CustomVariableContext customVariableContext) {
        Map<String, VariableDefinitionContext> variableContexts = customVariableContext.getVariableContexts();
        for (String overrideKey : overrideKeys) {
            if (variableContexts.containsKey(overrideKey)) {
                this.addOverrideParam(overrideKey, variableContexts.get(overrideKey).getValue());
            }
        }
    }

    public void addOverrideParam(String key, String value) {
        this.overrideParams.put(key, value);
    }

    public String getOverrideValue(String key) {
        if (overrideParams.containsKey(key)) {
            return this.overrideParams.get(key);
        }
        return StringUtils.EMPTY;
    }
}