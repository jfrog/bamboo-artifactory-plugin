package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.storage.StorageLocationService;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.dispatcher.Parameter;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;
import org.jfrog.bamboo.release.action.ReleasePromotionAction;

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


    protected GradleVersionHelper(PackageManagersContext context) {
        super(context);
    }

    @Override
    public List<ModuleVersionHolder> filterPropertiesForRelease(ImmutablePlan plan, int latestBuildNumberWithBi)
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
        Parameter moduleKeysParams = ((Parameter) parameters.get(ReleasePromotionAction.MODULE_KEY));
        if (moduleKeysParams != null) {
            for (String key : moduleKeysParams.getMultipleValues()) {
                configuration.put(ReleasePromotionAction.MODULE_KEY + "." + index, key);
                configuration.put(ReleasePromotionAction.RELEASE_PROP_KEY + "." + index,
                        String.valueOf(isReleaseProp(split, key)));
                index++;
            }
        }
        index = 0;
        Parameter originalValuesParams = ((Parameter) parameters.get(ReleasePromotionAction.CURRENT_VALUE_KEY));
        if (originalValuesParams != null) {
            for (String key : originalValuesParams.getMultipleValues()) {
                configuration.put(ReleasePromotionAction.CURRENT_VALUE_KEY + "." + index, key);
                index++;
            }
        }
        index = 0;
        Parameter nextIntegrationKeysParams = ((Parameter) parameters.get(ReleasePromotionAction.NEXT_INTEG_KEY));
        if (nextIntegrationKeysParams != null) {
            for (String key : nextIntegrationKeysParams.getMultipleValues()) {
                configuration.put(ReleasePromotionAction.NEXT_INTEG_KEY + "." + index, key);
                configuration.put(ReleasePromotionAction.RELEASE_PROP_KEY + "." + index, "false");
                index++;
            }
        }
        index = 0;
        Parameter releaseValueKeysParams = ((Parameter) parameters.get(ReleasePromotionAction.RELEASE_VALUE_KEY));
        if (releaseValueKeysParams != null) {
            for (String key : releaseValueKeysParams.getMultipleValues()) {
                configuration.put(ReleasePromotionAction.RELEASE_VALUE_KEY + "." + index, key);
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
