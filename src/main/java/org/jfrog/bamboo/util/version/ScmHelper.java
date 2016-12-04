package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.vcs.configuration.PlanRepositoryDefinition;
import com.atlassian.bamboo.vcs.configuration.VcsLocationDefinitionImpl;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public abstract class ScmHelper {

    private static final String GITHUB_TYPE = "gh";
    private enum ScmTypes{
        GIT(Arrays.asList(
                "com.atlassian.bamboo.plugins.atlassian-bamboo-plugin-git:git",
                "com.atlassian.bamboo.plugins.atlassian-bamboo-plugin-git:gitv2"
        )),
        STASH(Arrays.asList(
                "com.atlassian.bamboo.plugins.stash.atlassian-bamboo-plugin-stash:stash-rep",
                "com.atlassian.bamboo.plugins.stash.atlassian-bamboo-plugin-stash:bbserver"
        )),
        GITHUB(Arrays.asList(
                "com.atlassian.bamboo.plugins.atlassian-bamboo-plugin-git:gh"
        )),
        SVN(Arrays.asList(
                "com.atlassian.bamboo.plugin.system.repository:svn",
                "com.atlassian.bamboo.plugin.system.repository:svnv2"
        )),
        PERFORCE(Arrays.asList(
                "com.atlassian.bamboo.plugin.system.repository:p4"
        ));

        private List<String> pluginKeys;

        ScmTypes(List<String> pluginKeys) {
            this.pluginKeys = pluginKeys;
        }
    }

    @Nullable
    public static File getCheckoutDirectory(BuildContext buildContext) {
        Iterator<Long> repoIdIterator = buildContext.getRelevantRepositoryIds().iterator();
        if (repoIdIterator.hasNext()) {
            long repoId = repoIdIterator.next();
            String checkoutLocation = buildContext.getCheckoutLocation().get(repoId);
            if (StringUtils.isNotBlank(checkoutLocation)) {
                return new File(checkoutLocation);
            }
        }
        return null;
    }

    @Nullable
    public static String getRevisionKey(BuildContext buildContext) {
        Iterator<Long> repoIdIterator = buildContext.getRelevantRepositoryIds().iterator();
        if (repoIdIterator.hasNext()) {
            long repoId = repoIdIterator.next();
            String key = buildContext.getBuildChanges().getVcsRevisionKey(repoId);
            if (StringUtils.isNotBlank(key)) {
                return key;
            }
        }
        return null;
    }

    @Nullable
    public static String getVcsUrl(BuildContext buildContext) {
        int repoSize = buildContext.getRelevantRepositoryIds().size();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= repoSize; i++) {
            String vcsUrl = buildContext.getCurrentResult().getCustomBuildData().get("planRepository." + i + ".repositoryUrl");
            /*for Perforce*/
            if (StringUtils.isBlank(vcsUrl))
                vcsUrl = buildContext.getCurrentResult().getCustomBuildData().get("custom.p4.port");
            if (StringUtils.isBlank(vcsUrl)) {
                String repositoryType = buildContext.getCurrentResult().getCustomBuildData().get("planRepository." + i + ".type");
                /*for GitHub*/
                if (repositoryType != null && repositoryType.equals(GITHUB_TYPE)) {
                    PlanRepositoryDefinition repository = getRepository(buildContext);
                    HierarchicalConfiguration hierarchicalConfiguration = ((VcsLocationDefinitionImpl) repository.getVcsLocation()).getConfigurationRef().get();
                    Object property = repository.getVcsLocation().getConfiguration().get("repository.github.repository");
                    if (property == null && hierarchicalConfiguration != null) {
                        property = hierarchicalConfiguration.getString("repository.github.repository");
                    }
                    vcsUrl = "https://github.com/" + property + ".git";
                }
            }

            if (StringUtils.isNotBlank(vcsUrl)) {
                if (i != 1) {
                    sb.append(";" + vcsUrl);
                } else {
                    sb.append(vcsUrl);
                }
            }
        }

        return sb.toString();
    }

    @Nullable
    public static PlanRepositoryDefinition getRepository(BuildContext buildContext) {
        Iterator<Long> repoIdIterator = buildContext.getRelevantRepositoryIds().iterator();
        if (repoIdIterator.hasNext()) {
            long repoId = repoIdIterator.next();
            Map<Long, PlanRepositoryDefinition> repositoryDefinitionMap = buildContext.getVcsRepositoryMap();
            return repositoryDefinitionMap.get(repoId);
        }
        return null;
    }

    /**
     * @return Whether this repository is a git repository.
     * GitHub and Stash has the same behaviour like Git.
     */
    public static boolean isGitBase(PlanRepositoryDefinition repository) {
        return ScmTypes.GIT.pluginKeys.contains(repository.getPluginKey()) ||
                ScmTypes.GITHUB.pluginKeys.contains(repository.getPluginKey()) ||
                ScmTypes.STASH.pluginKeys.contains(repository.getPluginKey());
    }

    public static boolean isGit(PlanRepositoryDefinition repository) {
        return ScmTypes.GIT.pluginKeys.contains(repository.getPluginKey());
    }

    public static boolean isStash(PlanRepositoryDefinition repository) {
        return ScmTypes.STASH.pluginKeys.contains(repository.getPluginKey());
    }

    public static boolean isGithub(PlanRepositoryDefinition repository) {
        return ScmTypes.GITHUB.pluginKeys.contains(repository.getPluginKey());
    }

    public static boolean isSvn(PlanRepositoryDefinition repository) {
        return  ScmTypes.SVN.pluginKeys.contains(repository.getPluginKey());
    }

    public static boolean isPerforce(PlanRepositoryDefinition repository) {
        return  ScmTypes.PERFORCE.pluginKeys.contains(repository.getPluginKey());
    }
}
