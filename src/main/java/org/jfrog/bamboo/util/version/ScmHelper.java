package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryDefinition;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Iterator;

/**
 * @author Noam Y. Tenne
 */
public abstract class ScmHelper {

    @Nullable
    public static File getCheckoutDirectory(BuildContext buildContext) {
        for (RepositoryDefinition repoDef : buildContext.getRepositoryDefinitions()) {
            String checkoutLocation = buildContext.getCheckoutLocation().get(repoDef.getId());
            if (StringUtils.isNotBlank(checkoutLocation)) {
                return new File(checkoutLocation);
            }
        }
        return null;
    }

    @Nullable
    public static String getRevisionKey(BuildContext buildContext) {
        for (RepositoryDefinition repoDef : buildContext.getRepositoryDefinitions()) {
            String key = buildContext.getBuildChanges().getVcsRevisionKey(repoDef.getId());
            if (StringUtils.isNotBlank(key)) {
                return key;
            }
        }
        return null;
    }

    @Nullable
    public static Repository getRepository(BuildContext buildContext) {
        Iterator<Long> repoIdIterator = buildContext.getRelevantRepositoryIds().iterator();
        if (repoIdIterator.hasNext()) {
            long repoId = repoIdIterator.next().longValue();
            Iterable<RepositoryDefinition> repositoryDefinitions = buildContext.getRepositoryDefinitions();
            for (RepositoryDefinition repositoryDefinition : repositoryDefinitions) {
                if (repositoryDefinition.getId() == repoId) {
                    return repositoryDefinition.getRepository();
                }
            }
        }
        return null;
    }
}
