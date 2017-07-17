package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.storage.StorageLocationService;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.dispatcher.Parameter;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;
import org.jfrog.bamboo.release.action.ReleasePromotionAction;
import org.jfrog.bamboo.release.provider.ReleaseProvider;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author Tomer Cohen
 */
public class MavenVersionHelper extends VersionHelper {
    private static final Logger log = Logger.getLogger(MavenVersionHelper.class);

    private final CapabilityContext capabilityContext;

    protected MavenVersionHelper(AbstractBuildContext context, CapabilityContext capabilityContext) {
        super(context);
        this.capabilityContext = capabilityContext;
    }

    @Override
    public List<ModuleVersionHolder> filterPropertiesForRelease(Plan plan, int latestBuildNumberWithBi)
            throws RepositoryException, IOException {
        List<ModuleVersionHolder> result = Lists.newArrayList();
        final StorageLocationService storageLocationService = (StorageLocationService) ContainerManager.getComponent("storageLocationService");
        File directory = storageLocationService.getDefaultArtifactDirectoryBuilder().getBuildDirectory(PlanKeys.getPlanResultKey(plan.getKey(), latestBuildNumberWithBi));
        File buildInfoFile = new File(directory, "buildInfo/build-info.json.zip");
        if (buildInfoFile.exists()) {
            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(buildInfoFile)));
                String buildInfoString = CharStreams.toString(reader);
                Build build = BuildInfoExtractorUtils.jsonStringToBuildInfo(buildInfoString);
                List<Module> modules = build.getModules();
                for (Module module : modules) {
                    String id = module.getId();
                    String[] split = StringUtils.split(id, ":");
                    ModuleVersionHolder holder = new ModuleVersionHolder(split[0] + ":" + split[1], split[2]);
                    holder.setReleaseValue(calculateReleaseVersion(holder.getOriginalValue()));
                    holder.setNextIntegValue(calculateNextVersion(holder.getReleaseValue()));
                    result.add(holder);
                }
            } finally {
                Closeables.closeQuietly(reader);
            }
        }
        return result;
    }

    @Override
    public void addVersionFieldsToConfiguration(Map parameters, Map<String, String> configuration,
                                                String versionConfiguration, Map<String, String> taskConfiguration) {
        if (versionConfiguration.equals(ReleaseProvider.CFG_USE_EXISTING_VERSION)) {
            return;
        }
        if (versionConfiguration.equals(ReleaseProvider.CFG_ONE_VERSION)) {
            addGlobalVersion(parameters, configuration);
        }
        if (versionConfiguration.equals(ReleaseProvider.CFG_VERSION_PER_MODULE)) {
            addPerModuleVersioning(parameters, configuration);
        }
    }

    private void addPerModuleVersioning(Map parameters, Map<String, String> configuration) {
        int index = 0;
        Parameter moduleKeysParams = ((Parameter) parameters.get(ReleasePromotionAction.MODULE_KEY));
        if (moduleKeysParams != null) {
            for (String key : moduleKeysParams.getMultipleValues()) {
                configuration.put(ReleasePromotionAction.MODULE_KEY + "." + index, key);
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
        Parameter nextIntegrationKeysParams = ((Parameter) parameters.get(ReleasePromotionAction.NEXT_INTEG_KEY));
        if (nextIntegrationKeysParams != null) {
            String[] nextIntegrationKeys = nextIntegrationKeysParams.getMultipleValues();
            for (index = 1; index < nextIntegrationKeys.length; index++) {
                String key = nextIntegrationKeys[index];
                configuration.put(ReleasePromotionAction.NEXT_INTEG_KEY + "." + (index - 1), key);
            }
        }

        Parameter releaseValueKeysParams = ((Parameter) parameters.get(ReleasePromotionAction.RELEASE_VALUE_KEY));
        if (releaseValueKeysParams != null) {
            String[] releaseValueKeys = releaseValueKeysParams.getMultipleValues();
            for (index = 1; index < releaseValueKeys.length; index++) {
                String key = releaseValueKeys[index];
                configuration.put(ReleasePromotionAction.RELEASE_VALUE_KEY + "." + (index - 1), key);
            }
        }
    }

    private void addGlobalVersion(Map parameters, Map<String, String> configuration) {
        int index = 0;
        Parameter moduleKeysParams = ((Parameter) parameters.get(ReleasePromotionAction.MODULE_KEY));
        if (moduleKeysParams != null) {
            Parameter releaseValueKeysParam = ((Parameter) parameters.get(ReleasePromotionAction.RELEASE_VALUE_KEY));
            Parameter nextIntegrationKeysParam = ((Parameter) parameters.get(ReleasePromotionAction.NEXT_INTEG_KEY));
            for (String key : moduleKeysParams.getMultipleValues()) {
                configuration.put(ReleasePromotionAction.MODULE_KEY + "." + index, key);
                configuration.put(ReleasePromotionAction.RELEASE_VALUE_KEY + "." + index, releaseValueKeysParam.getValue());
                configuration.put(ReleasePromotionAction.NEXT_INTEG_KEY + "." + index, nextIntegrationKeysParam.getValue());
                index++;
            }
        }
    }
}
