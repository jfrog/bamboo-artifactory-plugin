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
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;
import org.jfrog.bamboo.release.action.ReleaseAndPromotionAction;
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
        String[] moduleKeys = (String[]) parameters.get(ReleaseAndPromotionAction.MODULE_KEY);
        if (moduleKeys != null) {
            for (String key : moduleKeys) {
                configuration.put(ReleaseAndPromotionAction.MODULE_KEY + "." + index, key);
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
        String[] nextIntegrationKeys = (String[]) parameters.get(ReleaseAndPromotionAction.NEXT_INTEG_KEY);
        if (nextIntegrationKeys != null) {
            for (index = 1; index < nextIntegrationKeys.length; index++) {
                String key = nextIntegrationKeys[index];
                configuration.put(ReleaseAndPromotionAction.NEXT_INTEG_KEY + "." + (index - 1), key);
            }
        }

        String[] releaseValueKeys = (String[]) parameters.get(ReleaseAndPromotionAction.RELEASE_VALUE_KEY);
        if (releaseValueKeys != null) {
            for (index = 1; index < releaseValueKeys.length; index++) {
                String key = releaseValueKeys[index];
                configuration.put(ReleaseAndPromotionAction.RELEASE_VALUE_KEY + "." + (index - 1), key);
            }
        }
    }

    private void addGlobalVersion(Map parameters, Map<String, String> configuration) {
        int index = 0;
        String[] moduleKeys = (String[]) parameters.get(ReleaseAndPromotionAction.MODULE_KEY);
        if (moduleKeys != null) {
            String[] releaseValueKeys = (String[]) parameters.get(ReleaseAndPromotionAction.RELEASE_VALUE_KEY);
            String[] nextIntegrationKeys = (String[]) parameters.get(ReleaseAndPromotionAction.NEXT_INTEG_KEY);
            for (String key : moduleKeys) {
                configuration.put(ReleaseAndPromotionAction.MODULE_KEY + "." + index, key);
                configuration.put(ReleaseAndPromotionAction.RELEASE_VALUE_KEY + "." + index, releaseValueKeys[0]);
                configuration.put(ReleaseAndPromotionAction.NEXT_INTEG_KEY + "." + index, nextIntegrationKeys[0]);
                index++;
            }
        }
    }
}
