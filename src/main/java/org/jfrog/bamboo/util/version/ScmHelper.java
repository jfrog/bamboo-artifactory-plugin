package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public abstract class ScmHelper {

    private static final String GITHUB_TYPE = "gh";

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
                    Repository repository = getRepository(buildContext);
                    Object property;
                    if (repository != null) {
                        property = repository.toConfiguration().getProperty("repository.github.repository");
                        vcsUrl = "https://github.com/" + property + ".git";
                    }
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
    public static Repository getRepository(BuildContext buildContext) {
        Iterator<Long> repoIdIterator = buildContext.getRelevantRepositoryIds().iterator();
        if (repoIdIterator.hasNext()) {
            long repoId = repoIdIterator.next();
            Map<Long, RepositoryDefinition> repositoryDefinitionMap = buildContext.getRepositoryDefinitionMap();
            RepositoryDefinition repositoryDefinition = repositoryDefinitionMap.get(repoId);
            if (repositoryDefinition != null) {
                return repositoryDefinition.getRepository();
            }
        }
        return null;
    }
}
