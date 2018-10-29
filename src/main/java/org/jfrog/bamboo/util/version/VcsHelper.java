package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public abstract class VcsHelper {

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
    public static String[] getVcsUrls(BuildContext buildContext) {
        int repoSize = buildContext.getRelevantRepositoryIds().size();
        List<String> urls = new ArrayList<>();
        for (int i = 1; i <= repoSize; i++) {
            String vcsUrl = buildContext.getCurrentResult().getCustomBuildData().get("planRepository." + i + ".repositoryUrl");
            /*for Perforce*/
            if (StringUtils.isBlank(vcsUrl) && StringUtils.isNotBlank(
                            buildContext.getCurrentResult().getCustomBuildData().get("planRepository." + i + ".depot"))) {
                vcsUrl = "depot:" +
                        buildContext.getCurrentResult().getCustomBuildData().get("planRepository." + i + ".depot") +
                        ",client:" +
                        buildContext.getCurrentResult().getCustomBuildData().get("planRepository." + i + ".client") +
                        ",port:" +
                        buildContext.getCurrentResult().getCustomBuildData().get("planRepository." + i + ".port");
            }
            if (StringUtils.isNotBlank(vcsUrl)) {
                urls.add(vcsUrl);
            }
        }
        return urls.toArray(new String[0]);
    }
}
