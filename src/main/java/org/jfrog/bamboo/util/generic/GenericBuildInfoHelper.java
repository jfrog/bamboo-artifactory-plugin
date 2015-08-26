package org.jfrog.bamboo.util.generic;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.util.BuildUtils;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.trigger.DependencyTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.ManualBuildTriggerReason;
import com.atlassian.bamboo.v2.build.trigger.TriggerReason;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.builder.BaseBuildInfoHelper;
import org.jfrog.bamboo.configuration.BuildParamsOverrideManager;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.TaskUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author Tomer Cohen
 */
public class GenericBuildInfoHelper extends BaseBuildInfoHelper {
    private static final Logger log = Logger.getLogger(GenericBuildInfoHelper.class);
    private final Map<String, String> env;
    private final String vcsRevision;
    private final String vcsUrl;

    public GenericBuildInfoHelper(Map<String, String> env, String vcsRevision, String vcsUrl) {
        this.env = env;
        this.vcsRevision = vcsRevision;
        this.vcsUrl = vcsUrl;
    }

    public Build extractBuildInfo(BuildContext buildContext, BuildLogger buildLogger, GenericContext context, String username) {
        String url = determineBambooBaseUrl();
        StringBuilder summaryUrl = new StringBuilder(url);
        if (!url.endsWith("/")) {
            summaryUrl.append("/");
        }
        String buildUrl = summaryUrl.append("browse/").
                append(EscapeChars.forURL(buildContext.getBuildResultKey())).toString();
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
                .agent(new Agent("Bamboo", BuildUtils.getVersionAndBuild())).artifactoryPrincipal(username)
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
        if (context.isIncludeEnvVars()) {
            Map<String, String> bambooProps = filterAndGetGlobalVariables();
            bambooProps.putAll(env);
            bambooProps = TaskUtils.getEscapedEnvMap(bambooProps);
            IncludeExcludePatterns patterns = new IncludeExcludePatterns(context.getEnvVarsIncludePatterns(),
                    context.getEnvVarsExcludePatterns());
            for (Map.Entry<String, String> prop : bambooProps.entrySet()) {
                String varKey = prop.getKey();
                if (PatternMatcher.pathConflicts(varKey, patterns)) {
                    continue;
                }
                builder.addProperty(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + varKey, prop.getValue());
            }
        }
        return builder.build();
    }

    private List<Artifact> convertDeployDetailsToArtifacts(Set<DeployDetails> details) {
        List<Artifact> result = Lists.newArrayList();
        for (DeployDetails detail : details) {
            String ext = FilenameUtils.getExtension(detail.getFile().getName());
            Artifact artifact = new ArtifactBuilder(detail.getFile().getName()).md5(detail.getMd5())
                    .sha1(detail.getSha1()).type(ext).build();
            result.add(artifact);
        }
        return result;
    }

    private String getTriggeringUserNameRecursively(BuildContext context) {
        String principal = null;
        TriggerReason triggerReason = context.getTriggerReason();
        if (triggerReason instanceof ManualBuildTriggerReason) {
            principal = ((ManualBuildTriggerReason) triggerReason).getUserName();

            if (StringUtils.isBlank(principal)) {

                BuildContext parentContext = context.getParentBuildContext();
                if (parentContext != null) {
                    principal = getTriggeringUserNameRecursively(parentContext);
                }
            }
        }
        return principal;
    }

    public Set<DeployDetails> createDeployDetailsAndAddToBuildInfo(Build build, Multimap<String, File> filesMap, BuildContext buildContext,
                                                                   GenericContext genericContext)
            throws IOException, NoSuchAlgorithmException {
        Set<DeployDetails> details = Sets.newHashSet();
        Map<String, String> dynamicPropertyMap = getDynamicPropertyMap(build);
        String repoKey = overrideParam(genericContext.getRepoKey(), BuildParamsOverrideManager.OVERRIDE_ARTIFACTORY_DEPLOY_REPO);
        for (Map.Entry<String, File> entry : filesMap.entries()) {
            details.addAll(buildDeployDetailsFromFileSet(entry, repoKey, dynamicPropertyMap));
        }
        List<Artifact> artifacts = convertDeployDetailsToArtifacts(details);
        ModuleBuilder moduleBuilder =
                new ModuleBuilder().id(buildContext.getPlanName() + ":" + buildContext.getBuildNumber())
                        .artifacts(artifacts);
        build.setModules(Lists.newArrayList(moduleBuilder.build()));
        return details;
    }

    private Map<String, String> getDynamicPropertyMap(Build build) {
        Properties dynamicProperties = BuildInfoExtractorUtils.filterDynamicProperties(
                build.getProperties(), BuildInfoExtractorUtils.MATRIX_PARAM_PREDICATE);
        Properties prefixLessDynamicProperties = BuildInfoExtractorUtils.stripPrefixFromProperties(dynamicProperties,
                ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX);
        return Maps.fromProperties(prefixLessDynamicProperties);
    }

    private Set<DeployDetails> buildDeployDetailsFromFileSet(Map.Entry<String, File> fileEntry, String targetRepository,
                                                             Map<String, String> propertyMap) throws IOException,
            NoSuchAlgorithmException {
        Set<DeployDetails> result = Sets.newHashSet();
        String targetPath = fileEntry.getKey();
        File artifactFile = fileEntry.getValue();
        String path = PublishedItemsHelper.calculateTargetPath(targetPath, artifactFile);
        path = StringUtils.replace(path, "//", "/");

        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "SHA1", "MD5");
        DeployDetails.Builder deployDetails = new DeployDetails.Builder().file(artifactFile).md5(checksums.get("MD5"))
                .sha1(checksums.get("SHA1")).targetRepository(targetRepository).artifactPath(path);
        addCommonProperties(deployDetails);
        deployDetails.addProperties(propertyMap);
        result.add(deployDetails.build());
        return result;
    }

    private void addCommonProperties(DeployDetails.Builder details) {
        details.addProperty(BuildInfoFields.BUILD_NAME, context.getPlanName());
        details.addProperty(BuildInfoFields.BUILD_NUMBER, String.valueOf(context.getBuildNumber()));
        if (StringUtils.isNotBlank(vcsRevision)) {
            details.addProperty(BuildInfoFields.VCS_REVISION, vcsRevision);
        }
        if (StringUtils.isNotBlank(vcsUrl)) {
            details.addProperty(BuildInfoFields.VCS_URL, vcsUrl);
        }

        String buildTimeStampVal = context.getBuildResult().getCustomBuildData().get("buildTimeStamp");
        long buildTimeStamp = System.currentTimeMillis();
        if (StringUtils.isNotBlank(buildTimeStampVal)) {
            buildTimeStamp = new DateTime(buildTimeStampVal).getMillis();
        }
        String buildTimeStampString = String.valueOf(buildTimeStamp);
        details.addProperty(BuildInfoFields.BUILD_TIMESTAMP, buildTimeStampString);
        addBuildParentProperties(details, context.getTriggerReason());
    }

    private void addBuildParentProperties(DeployDetails.Builder details, TriggerReason triggerReason) {
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
                details.addProperty(BuildInfoFields.BUILD_PARENT_NAME, parentBuildName);
                details.addProperty(BuildInfoFields.BUILD_PARENT_NUMBER, triggeringBuildNumber);
            }
        }
    }
}
