package org.jfrog.bamboo.bintray;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.bintray.client.AQLEntry;
import org.jfrog.bamboo.bintray.client.AQLSearchResults;
import org.jfrog.bamboo.bintray.client.BuildInfo;
import org.jfrog.bamboo.bintray.client.GavcSearchResults;
import org.jfrog.bamboo.util.ActionLog;
import org.jfrog.bamboo.util.HttpUtils;
import org.jfrog.build.api.Build;

import java.util.List;
import java.util.Map;

/**
 * Helper class to handle special promote to Bintray and MavenCentral
 * <p>
 * Spring migration class
 *
 * @author Aviad Shikloshi
 */
public class BintrayOsoUtils {

    private static final Logger log = Logger.getLogger(BintrayOsoUtils.class);
    private static final String NEXUS_PUSH_PLUGIN_NAME = "bintrayOsoPush";
    private static final String AQL_SEARCH_API = "/api/search/aql";
    private static final String REPO_PROPERTIES_QUERY = "properties.find({ \"item.repo\": { \"$eq\": \"%s\"} })";
    private static final String GROUP_PARAM_KEY = "g";
    private static final String ARTIFACT_PARAM_KEY = "a";

    private static final HttpClient client = new DefaultHttpClient();

    /**
     * Check if PushToBintray should use pre generated fields by checking if
     * bintrayOsoPush user plugin is present in Artifactory
     */
    public static boolean isOsoPushPluginDeployed(ServerConfig serverConfig) {
        try {
            Map<String, List<Map>> userPluginInfo = BintrayOsoUtils.getUserPluginInfo(serverConfig);
            if (!userPluginInfo.containsKey("promotions")) {
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
            return false;
        }
    }

    /**
     * Generate fields for Push to Bintray
     */
    public static void collectPushToBintrayProperties(PushToBintrayAction ptbAction, ServerConfig serverConfig,
                                                      Map<String, String> buildConfigMap, ActionLog bintrayLog) {
        try {
            Build currentBuildInfo = getBuildInfo(serverConfig);

            BintrayPropertiesCollector bintrayPropsCollector = new BintrayPropertiesCollector(serverConfig, currentBuildInfo, bintrayLog);
            String repoKey = bintrayPropsCollector.getRepoKeyByArtifactorySearch();
            List<AQLEntry> props = BintrayOsoUtils.getPropertiesForRepository(serverConfig, repoKey).getResults();
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
            bintrayLog.logError("Error while collecting Push to Bintray values.", e);
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

    private static Build getBuildInfo(ServerConfig serverConfig) {
        return getBuildInfo(serverConfig, PushToBintrayAction.context.getBuildKey(),
                String.valueOf(PushToBintrayAction.context.getBuildNumber()));
    }

    /**
     * Retrieves repository properties
     *
     * @param repoKey Artifactory valid and exists key
     * @return all properties for the repoKey
     */
    public static AQLSearchResults getPropertiesForRepository(ServerConfig artifactoryServerConfig, String repoKey) {
        try {
            String uri = artifactoryServerConfig.getUrl() + AQL_SEARCH_API;
            HttpPost getProperties = new HttpPost(uri);
            getProperties.setHeader(HttpUtils.createAuthorizationHeader(artifactoryServerConfig.getUsername(),
                    artifactoryServerConfig.getPassword()));
            getProperties.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
            StringEntity query = new StringEntity(String.format(REPO_PROPERTIES_QUERY, repoKey));
            getProperties.setEntity(query);
            HttpResponse propertiesResponse = client.execute(getProperties);
            return HttpUtils.handleResponse(propertiesResponse, AQLSearchResults.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain user plugin information", e);
        }
    }

    /**
     * Get user plugins info from Artifactory
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<Map>> getUserPluginInfo(ServerConfig artifactoryServerConfig) {
        try {
            String apiUri = artifactoryServerConfig.getUrl() + "/api/plugins";
            HttpGet getPlugins = new HttpGet(apiUri);
            getPlugins.setHeader(HttpUtils.createAuthorizationHeader(artifactoryServerConfig.getUsername(),
                    artifactoryServerConfig.getPassword()));
            HttpResponse getResponse = client.execute(getPlugins);
            return HttpUtils.handleResponse(getResponse, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain user plugin information", e);
        }
    }

    /**
     * Search Artifactory by Group and Artifact
     */
    public static GavcSearchResults gavcSearch(ServerConfig artifactoryServerConfig, String group, String artifact) {
        try {
            URIBuilder uriBuilder = new URIBuilder(artifactoryServerConfig.getUrl() + "/api/search/gavc");
            uriBuilder.addParameter(GROUP_PARAM_KEY, group);
            if (StringUtils.isNotBlank(artifact)) {
                uriBuilder.setParameter(ARTIFACT_PARAM_KEY, artifact);
            }
            HttpGet gavcSearch = new HttpGet(uriBuilder.build());
            HttpResponse searchResult = client.execute(gavcSearch);
            return HttpUtils.handleResponse(searchResult, GavcSearchResults.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain user plugin information", e);
        }
    }

    /**
     * Get Build info for specific build
     *
     * @param buildName   Bamboo build name
     * @param buildNumber Bamboo build number
     * @return Build object
     */
    public static Build getBuildInfo(ServerConfig artifactoryServerConfig, String buildName, String buildNumber) {
        try {
            String apiUri = artifactoryServerConfig.getUrl() + "/api/build/" + HttpUtils.encodePath(buildName) + "/" + HttpUtils.encodePath(buildNumber);
            HttpGet getBuildInfoRequest = new HttpGet(apiUri);
            getBuildInfoRequest.setHeader("Content-Type", "application/json");
            getBuildInfoRequest.setHeader(HttpUtils.createAuthorizationHeader(artifactoryServerConfig.getUsername(),
                    artifactoryServerConfig.getPassword()));
            HttpResponse response = client.execute(getBuildInfoRequest);
            return HttpUtils.handleResponse(response, BuildInfo.class).getBuildInfo();
        } catch (Exception e) {
            throw new RuntimeException("Error while requesting Build info for build.", e);
        }
    }


}