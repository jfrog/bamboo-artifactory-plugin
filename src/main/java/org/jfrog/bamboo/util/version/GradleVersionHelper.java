package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.storage.StorageLocationService;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;
import org.jfrog.bamboo.release.action.ReleaseAndPromotionAction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tomer Cohen
 */
public class GradleVersionHelper extends VersionHelper {


    protected GradleVersionHelper(AbstractBuildContext context) {
        super(context);
    }

    @Override
    public List<ModuleVersionHolder> filterPropertiesForRelease(Plan plan, int latestBuildNumberWithBi)
            throws RepositoryException, IOException {
        List<ModuleVersionHolder> result = Lists.newArrayList();
        final StorageLocationService storageLocationService = (StorageLocationService) ContainerManager.getComponent("storageLocationService");
        File directory = storageLocationService.getDefaultArtifactDirectoryBuilder().getBuildDirectory(PlanKeys.getPlanResultKey(plan.getKey(), latestBuildNumberWithBi));
        File gradlePropertiesFile = new File(directory, "gradle/gradle.properties");
        if (gradlePropertiesFile.exists()) {
            Properties props = new Properties();
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(gradlePropertiesFile);
                props.load(stream);
            } finally {
                Closeables.closeQuietly(stream);
            }
            result = context.releaseManagementContext.filterPropsForRelease(Maps.fromProperties(props));
        }
        for (ModuleVersionHolder holder : result) {
            holder.setReleaseValue(calculateReleaseVersion(holder.getOriginalValue()));
            if (!holder.isReleaseProp()) {
                holder.setNextIntegValue(calculateNextVersion(holder.getReleaseValue()));
            }
        }
        return result;
    }

    @Override
    public void addVersionFieldsToConfiguration(Map parameters, Map<String, String> configuration,
                                                String useCurrentVersion, Map<String, String> taskConfiguration) {

        if (Boolean.valueOf(useCurrentVersion)) {
            return;
        }

        GradleBuildContext buildContext = new GradleBuildContext(taskConfiguration);
        String releaseProps = buildContext.getReleaseProps();
        String[] split = StringUtils.split(releaseProps, ", ");
        int index = 0;
        String[] moduleKeys = (String[]) parameters.get(ReleaseAndPromotionAction.MODULE_KEY);
        if (moduleKeys != null) {
            for (String key : moduleKeys) {
                configuration.put(ReleaseAndPromotionAction.MODULE_KEY + "." + index, key);
                configuration.put(ReleaseAndPromotionAction.RELEASE_PROP_KEY + "." + index,
                        String.valueOf(isReleaseProp(split, key)));
                index++;
            }
        }
        index = 0;
        String[] originalValues = (String[]) parameters.get(ReleaseAndPromotionAction.CURRENT_VALUE_KEY);
        if (originalValues != null) {
            for (String key : originalValues) {
                configuration.put(ReleaseAndPromotionAction.CURRENT_VALUE_KEY + "." + index, key);
                index++;
            }
        }
        index = 0;
        String[] nextIntegrationKeys = (String[]) parameters.get(ReleaseAndPromotionAction.NEXT_INTEG_KEY);
        if (nextIntegrationKeys != null) {
            for (String key : nextIntegrationKeys) {
                configuration.put(ReleaseAndPromotionAction.NEXT_INTEG_KEY + "." + index, key);
                configuration.put(ReleaseAndPromotionAction.RELEASE_PROP_KEY + "." + index, "false");
                index++;
            }
        }
        index = 0;
        String[] releaseValueKeys = (String[]) parameters.get(ReleaseAndPromotionAction.RELEASE_VALUE_KEY);
        if (releaseValueKeys != null) {
            for (String key : releaseValueKeys) {
                configuration.put(ReleaseAndPromotionAction.RELEASE_VALUE_KEY + "." + index, key);
                index++;
            }
        }
    }

    private boolean isReleaseProp(String[] releaseProps, String prop) {
        for (String releaseProp : releaseProps) {
            if (StringUtils.equals(releaseProp, prop)) {
                return true;
            }
        }
        return false;
    }
}
