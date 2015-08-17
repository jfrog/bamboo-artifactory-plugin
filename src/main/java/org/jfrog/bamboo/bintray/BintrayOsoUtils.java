package org.jfrog.bamboo.bintray;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.jfrog.bamboo.bintray.client.ArtifactoryResponseEntry;
import org.jfrog.bamboo.bintray.client.JfClient;
import org.jfrog.build.api.Build;

import java.util.List;
import java.util.Map;

/**
 * Helper class to handle special promote to Bintray and MavenCentral
 *
 * @author Aviad Shikloshi
 */
public class BintrayOsoUtils {

    private static final String NEXUS_PUSH_PLUGIN_NAME = "bintrayOsoPush";

    /**
     * Check if PushToBintray should use pre generated fields by checking if
     * bintrayOsoPush user plugin is present in Artifactory
     */
    public static boolean shouldUseOsoUserPlugin(JfClient jfClient) {
        try {
            Map<String, List<Map>> userPluginInfo = jfClient.getUserPluginInfo();
            if (!userPluginInfo.containsKey("promotions")) {
                PushToBintrayAction.log.error("No special promotion modes enabled: no selected Artifactory server Id");
                return false;
            }
            List<Map> executionPlugins = userPluginInfo.get("promotions");
            Map promotePlugin = Iterables.find(executionPlugins, new Predicate<Map>() {
                @Override
                public boolean apply(Map pluginInfo) {
                    if ((pluginInfo != null) && pluginInfo.containsKey("name")) {
                        String pluginName = pluginInfo.get("name").toString();
                        return NEXUS_PUSH_PLUGIN_NAME.equals(pluginName);
                    }
                    return false;
                }
            });
            return promotePlugin != null;
        } catch (Exception e) {
            PushToBintrayAction.log.error("Could not retrieve bintrayOsoPush plugin information.");
            return false;
        }
    }

    /**
     * Generate fields for Push to Bintray
     */
    public static void collectPushToBintrayProperties(PushToBintrayAction ptbAction,
                                                      Map<String, String> buildConfigMap) {
        try {
            JfClient jfClient = ptbAction.getJfClient();
            Build currentBuildInfo = getBuildInfo(jfClient);

            BintrayPropertiesCollector bintrayPropsCollector = new BintrayPropertiesCollector(currentBuildInfo);
            String repoKey = bintrayPropsCollector.getRepoKeyByArtifactorySearch(jfClient);
            List<ArtifactoryResponseEntry> props = jfClient.getPropertiesForRepository(repoKey).getResults();
            ptbAction.setPackageName(bintrayPropsCollector.getPackageNameFromProperties(props));
            ptbAction.setSubject(bintrayPropsCollector.getFromSystem("subject"));
            ptbAction.setRepository(bintrayPropsCollector.getFromSystem("repository"));
            ptbAction.setSignMethod(bintrayPropsCollector.getFromSystem("signMethod"));
            ptbAction.setVcsUrl(bintrayPropsCollector.getFromSystem("vcsurl"));
            ptbAction.setLicenses(bintrayPropsCollector.getFromSystem("licenses"));
            ptbAction.setGpgPassphrase(bintrayPropsCollector.getFromSystem("passphrase"));
            ptbAction.setOverrideDescriptorFile(true);
            ptbAction.setMavenSync(true);

            populateDefaultValuesFromActionValues(ptbAction, buildConfigMap);

        } catch (Exception e) {
            PushToBintrayAction.log.error("Error while collecting Push to Bintray values.", e);
        }
    }

    // Adding the values collected to the Default job config fields
    private static void populateDefaultValuesFromActionValues(PushToBintrayAction ptbAction, Map<String, String> buildConfigMap) {
        buildConfigMap.put("bintray.subject", ptbAction.getSubject());
        buildConfigMap.put("bintray.packageName", ptbAction.getPackageName());
        buildConfigMap.put("bintray.repository", ptbAction.getRepository());
        buildConfigMap.put("bintray.vcsUrl", ptbAction.getVcsUrl());
        buildConfigMap.put("bintray.licenses", ptbAction.getLicenses());
        buildConfigMap.put("bintray.signMethod", ptbAction.getSignMethod());
        buildConfigMap.put("bintray.gpgPassphrase", ptbAction.getGpgPassphrase());
        buildConfigMap.put("bintrayConfiguration", "true");
    }

    private static Build getBuildInfo(JfClient jfClient) {
        return jfClient.getBuildInfo(PushToBintrayAction.context.getBuildKey(),
                String.valueOf(PushToBintrayAction.context.getBuildNumber()));
    }
}