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
