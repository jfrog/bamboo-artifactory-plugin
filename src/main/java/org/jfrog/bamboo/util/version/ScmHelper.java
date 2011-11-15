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
        RepositoryDefinition repoDef = getFirstRepoDef(buildContext);
        if (repoDef != null) {
            String checkoutLocation = buildContext.getCheckoutLocation().get(repoDef.getId());
            if (StringUtils.isNotBlank(checkoutLocation)) {
                return new File(checkoutLocation);
            }
        }
        return null;
    }

    @Nullable
    public static String getRevisionKey(BuildContext buildContext) {
        RepositoryDefinition repoDef = getFirstRepoDef(buildContext);
        if (repoDef != null) {
            return buildContext.getBuildChanges().getVcsRevisionKey(repoDef.getId());
        }

        return null;
    }

    @Nullable
    public static Repository getRepository(BuildContext buildContext) {
        RepositoryDefinition repoDef = getFirstRepoDef(buildContext);
        if (repoDef != null) {
            return repoDef.getRepository();
        }
        return null;
    }

    private static RepositoryDefinition getFirstRepoDef(BuildContext buildContext) {
        Iterator<RepositoryDefinition> repoDefs = buildContext.getRepositoryDefinitions().iterator();
        if (repoDefs.hasNext()) {
            return repoDefs.next();
        }
        return repoDefs.next();
    }
}
