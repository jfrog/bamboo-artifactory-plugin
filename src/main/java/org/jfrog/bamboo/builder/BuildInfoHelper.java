package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.util.BuildUtils;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.trigger.DependencyTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.BuildInfoLog;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.bamboo.util.version.VcsHelper;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.IOException;
import java.util.*;

/**
 * @author Tomer Cohen
 */
public class BuildInfoHelper extends BaseBuildInfoHelper {
    private static final Logger log = Logger.getLogger(BuildInfoHelper.class);
    private final Map<String, String> env;
    private final String vcsRevision;
    private final String vcsUrl;
    private ServerConfig serverConfig;

    private BuildInfoHelper(Map<String, String> env, String vcsRevision, String vcsUrl) {
        this.env = env;
        this.vcsRevision = vcsRevision;
        this.vcsUrl = vcsUrl;
    }

    private BuildInfoBuilder extractBuilder(BuildContext buildContext, BuildLogger buildLogger) {
        String url = determineBambooBaseUrl();
        StringBuilder summaryUrl = new StringBuilder(url);
        if (!url.endsWith("/")) {
            summaryUrl.append("/");
        }
        String buildUrl = summaryUrl.append("browse/").
                append(EscapeChars.forFormSubmission(buildContext.getPlanResultKey().getKey())).toString();
        DateTime start = new DateTime(buildContext.getBuildResult().getCustomBuildData().get("buildTimeStamp"));
        DateTime end = new DateTime();
        long duration = -1;
        if (start.isBefore(end)) {
            duration = new Interval(start, end).toDurationMillis();
        } else {
            log.warn(buildLogger.addErrorLogEntry("Agent machine time is lower than the server machine time, please synchronize them."));
        }

        BuildInfoBuilder builder = new BuildInfoBuilder(buildContext.getPlanName())
                .number(String.valueOf(buildContext.getBuildNumber())).type(BuildType.GENERIC)
                .agent(new Agent("Bamboo", BuildUtils.getVersionAndBuild())).artifactoryPrincipal(serverConfig.getUsername())
                .startedDate(new Date()).durationMillis(duration).url(buildUrl);
        if (StringUtils.isNotBlank(vcsRevision)) {
            builder.vcsRevision(vcsRevision);
        }

        if (StringUtils.isNotBlank(vcsUrl)) {
            builder.vcsUrl(vcsUrl);
        }

        String principal = getTriggeringUserNameRecursively(buildContext);
        if (StringUtils.isBlank(principal)) {
            principal = "auto";
        }
        builder.principal(principal);
        return builder;
    }

    public List<Artifact> convertDeployDetailsToArtifacts(Set<DeployDetails> details) {
        List<Artifact> result = Lists.newArrayList();
        for (DeployDetails detail : details) {
            String ext = FilenameUtils.getExtension(detail.getFile().getName());
            Artifact artifact = new ArtifactBuilder(detail.getFile().getName()).md5(detail.getMd5())
                    .sha1(detail.getSha1()).type(ext).build();
            result.add(artifact);
        }
        return result;
    }

    public Map<String, String> getDynamicPropertyMap(Build build) {
        Map<String, String> filteredPropertyMap = new HashMap<>();
        if (build.getProperties() != null) {
            for (Map.Entry<Object, Object> entry : build.getProperties().entrySet()) {
                String key = entry.getKey().toString();
                if (StringUtils.startsWith(key, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX)) {
                    filteredPropertyMap.put(
                            StringUtils.removeStart(key, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX),
                            (String) entry.getValue());
                }
            }
        }
        return filteredPropertyMap;
    }

    public BuildInfoBuilder getBuilder(TaskContext taskContext) {
        BuildContext buildContext = taskContext.getBuildContext();
        return extractBuilder(buildContext, taskContext.getBuildLogger());
    }

    @NotNull
    public Build getBuild(@NotNull TaskContext taskContext, GenericContext genericContext) {
        BuildInfoBuilder builder = getBuilder(taskContext);
        builder = addEnvVarsToBuildInfo(genericContext, builder);
        Build build = builder.build();
        build.setBuildAgent(new BuildAgent("Generic"));
        return build;
    }

    public BuildInfoBuilder addEnvVarsToBuildInfo(GenericContext genericContext, BuildInfoBuilder builder) {
        if (genericContext.isIncludeEnvVars()) {
            Map<String, String> props = Maps.newHashMap(TaskUtils.getEscapedEnvMap(env));
            props.putAll(getBuildInfoConfigPropertiesFileParams(props.get(BuildInfoConfigProperties.PROP_PROPS_FILE)));
            IncludeExcludePatterns patterns = new IncludeExcludePatterns(genericContext.getEnvVarsIncludePatterns(),
                    genericContext.getEnvVarsExcludePatterns());
            for (Map.Entry<String, String> prop : props.entrySet()) {
                String varKey = prop.getKey();
                if (PatternMatcher.pathConflicts(varKey, patterns)) {
                    continue;
                }
                // Global/task variables which starts with "artifactory.deploy" and "buildInfo.property" must preserve their prefix.
                if (!StringUtils.startsWith(varKey, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX) && !StringUtils.startsWith(varKey, BuildInfoProperties.BUILD_INFO_PROP_PREFIX)) {
                    varKey = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + varKey;
                }
                builder.addProperty(varKey, prop.getValue());
            }
        }
        return builder;
    }

    public void addCommonProperties(Map<String, String> propertyMap) {
        propertyMap.put(BuildInfoFields.BUILD_NAME, context.getPlanName());
        propertyMap.put(BuildInfoFields.BUILD_NUMBER, String.valueOf(context.getBuildNumber()));
        if (StringUtils.isNotBlank(vcsRevision)) {
            propertyMap.put(BuildInfoFields.VCS_REVISION, vcsRevision);
        }
        if (StringUtils.isNotBlank(vcsUrl)) {
            propertyMap.put(BuildInfoFields.VCS_URL, vcsUrl);
        }

        String buildTimeStampVal = context.getBuildResult().getCustomBuildData().get("buildTimeStamp");
        long buildTimeStamp = System.currentTimeMillis();
        if (StringUtils.isNotBlank(buildTimeStampVal)) {
            buildTimeStamp = new DateTime(buildTimeStampVal).getMillis();
        }
        String buildTimeStampString = String.valueOf(buildTimeStamp);
        propertyMap.put(BuildInfoFields.BUILD_TIMESTAMP, buildTimeStampString);
        addBuildParentProperties(propertyMap, context.getTriggerReason());
    }

    private void addBuildParentProperties(Map<String, String> propertyMap, TriggerReason triggerReason) {
        if (triggerReason instanceof DependencyTriggerReason) {
            String triggeringBuildResultKey = ((DependencyTriggerReason) triggerReason).getTriggeringBuildResultKey();
            if (StringUtils.isNotBlank(triggeringBuildResultKey) &&
                    (StringUtils.split(triggeringBuildResultKey, "-").length == 3)) {
                String triggeringBuildKey =
                        triggeringBuildResultKey.substring(0, triggeringBuildResultKey.lastIndexOf("-"));
                String triggeringBuildNumber =
                        triggeringBuildResultKey.substring(triggeringBuildResultKey.lastIndexOf("-") + 1);
                String parentBuildName = getBuildName(triggeringBuildKey);
                if (StringUtils.isBlank(parentBuildName)) {
                    log.error("Received a null build parent name.");
                }
                propertyMap.put(BuildInfoFields.BUILD_PARENT_NAME, parentBuildName);
                propertyMap.put(BuildInfoFields.BUILD_PARENT_NUMBER, triggeringBuildNumber);
            }
        }
    }

    private static Module createModule(TaskContext taskContext, List<Artifact> artifacts, List<Dependency> dependencies, Map<String, String> properties) throws IOException {
        ModuleBuilder moduleBuilder =
                new ModuleBuilder().id(taskContext.getBuildContext().getPlanName() + ":" + taskContext.getBuildContext().getBuildNumber())
                        .artifacts(artifacts).dependencies(dependencies);
        return moduleBuilder.build();
    }

    public ArtifactoryBuildInfoClientBuilder getClientBuilder(BuildLogger buildLogger, Logger logger) {
        BuildInfoLog bambooBuildInfoLog = new BuildInfoLog(logger, buildLogger);
        ArtifactoryBuildInfoClientBuilder clientBuilder = TaskUtils.getArtifactoryBuildInfoClientBuilder(serverConfig, bambooBuildInfoLog);
        return clientBuilder;
    }

    private static BuildInfoHelper createBuildInfoHelperBase(TaskContext taskContext, BuildContext buildContext, EnvironmentVariableAccessor environmentVariableAccessor,
                                                             BuildParamsOverrideManager buildParamsOverrideManager, ServerConfig serverConfig) {
        Map<String, String> env = Maps.newHashMap();
        env.putAll(environmentVariableAccessor.getEnvironment(taskContext));
        env.putAll(environmentVariableAccessor.getEnvironment());
        String vcsRevision = VcsHelper.getRevisionKey(buildContext);
        if (StringUtils.isBlank(vcsRevision)) {
            vcsRevision = "";
        }

        String[] vcsUrls = VcsHelper.getVcsUrls(buildContext);
        String vcsUrl = vcsUrls.length > 0 ? vcsUrls[0] : "";
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }

        BuildInfoHelper buildInfoHelper = new BuildInfoHelper(env, vcsRevision, vcsUrl);
        buildInfoHelper.init(buildParamsOverrideManager, buildContext);
        return buildInfoHelper;
    }

    public static BuildInfoHelper createDeployBuildInfoHelper(TaskContext taskContext, BuildContext buildContext, EnvironmentVariableAccessor environmentVariableAccessor,
                                                              long selectedServerId, String username, String password, BuildParamsOverrideManager buildParamsOverrideManager) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(selectedServerId);
        BuildInfoHelper buildInfoHelper = createBuildInfoHelperBase(taskContext, buildContext, environmentVariableAccessor, buildParamsOverrideManager, selectedServerConfig);

        buildInfoHelper.serverConfig = TaskUtils.getDeploymentServerConfig(username, password, serverConfigManager,
                selectedServerConfig, buildParamsOverrideManager);

        return buildInfoHelper;
    }

    public static BuildInfoHelper createResolveBuildInfoHelper(TaskContext taskContext, BuildContext buildContext, EnvironmentVariableAccessor environmentVariableAccessor,
                                                        long selectedServerId, String username, String password, BuildParamsOverrideManager buildParamsOverrideManager) {
        ServerConfigManager serverConfigManager = ServerConfigManager.getInstance();
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(selectedServerId);
        BuildInfoHelper buildInfoHelper = createBuildInfoHelperBase(taskContext, buildContext, environmentVariableAccessor, buildParamsOverrideManager, selectedServerConfig);

        buildInfoHelper.serverConfig = TaskUtils.getResolutionServerConfig(username, password, serverConfigManager,
                selectedServerConfig, buildParamsOverrideManager);

        return buildInfoHelper;
    }

    public Build addBuildInfoParams(TaskContext taskContext, Build build, Map<String, String> buildProperties, List<Artifact> artifacts, List<Dependency> dependencies, List<BuildDependency> buildDependencies) throws IOException {
        Module module = BuildInfoHelper.createModule(taskContext, artifacts, dependencies, buildProperties);
        build.setBuildDependencies(buildDependencies);
        build.setModules(Lists.newArrayList(module));
        return build;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }
}
