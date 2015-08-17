package org.jfrog.bamboo.bintray.client;

import com.google.gson.Gson;
import com.sun.syndication.io.impl.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.build.api.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * HttpClient for Artifactory and Bintray Rest calls
 *
 * @author Aviad Shikloshi
 */
public class JfHttpClient implements JfClient {

    private static final Logger log = Logger.getLogger(JfHttpClient.class);
    private static final String REPO_PROPERTIES_QUERY = "properties.find({ \"item.repo\": { \"$eq\": \"%s\"} })";
    private static final String AQL_SEARCH_API = "/api/search/aql";

    private HttpClient client = new DefaultHttpClient();
    private Gson gson = new Gson();

    private String artifactoryUrl;
    private String artifactoryUsername;
    private String artifactoryPassword;
    private String bintrayUsername;
    private String bintrayApiKey;

    public JfHttpClient(String artifactoryUrl, String artifactoryUsername,
                        String artifactoryPassword, String bintrayUsername, String bintrayApiKey) {
        this.artifactoryUrl = artifactoryUrl;
        this.artifactoryUsername = artifactoryUsername;
        this.artifactoryPassword = artifactoryPassword;
        this.bintrayUsername = bintrayUsername;
        this.bintrayApiKey = bintrayApiKey;
    }

    @Override
    public Build getBuildInfo(String buildName, String buildNumber) {
        try {
            String apiUri = this.artifactoryUrl + "/api/build/" + encodePath(buildName) + "/" + encodePath(buildNumber);
            HttpGet getBuildInfoRequest = new HttpGet(apiUri);
            getBuildInfoRequest.setHeader("Content-Type", "application/json");
            getBuildInfoRequest.setHeader(createAuthorizationHeader(artifactoryUsername, artifactoryPassword));
            HttpResponse response = this.client.execute(getBuildInfoRequest);
            return handleResponse(response, BuildInfo.class).getBuildInfo();
        } catch (Exception e) {
            log.error("Error while requesting Build info for build name: " + buildName);
            throw new RuntimeException("Error while requesting Build info for build.", e);
        }
    }

    @Override
    public String mavenCentralSync(MavenCentralSyncModel model, String subject, String repo, String packageName, String version) {
        try {
            String apiUri = String.format(ConstantValues.MAVEN_SYNC_URL, encodePath(subject), encodePath(repo),
                    encodePath(packageName), encodePath(version));
            HttpPost mavenSyncRequest = new HttpPost(apiUri);
            mavenSyncRequest.setHeader(createAuthorizationHeader(bintrayUsername, bintrayApiKey));
            String jsonString = jsonStringToObject(model);
            mavenSyncRequest.setEntity(new StringEntity(jsonString));
            HttpResponse response = this.client.execute(mavenSyncRequest);
            return IOUtils.toString(response.getEntity().getContent());
        } catch (Exception e) {
            log.error("Error while requesting repository properties.");
            throw new RuntimeException("Error while requesting repository properties.", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<Map>> getUserPluginInfo() {
        try {
            String apiUri = artifactoryUrl + "/api/plugins";
            HttpGet getPlugins = new HttpGet(apiUri);
            getPlugins.setHeader(createAuthorizationHeader(artifactoryUsername, artifactoryPassword));
            HttpResponse getResponse = client.execute(getPlugins);
            return handleResponse(getResponse, Map.class);
        } catch (Exception e) {
            log.error("Failed to obtain user plugin information.");
            throw new RuntimeException("Failed to obtain user plugin information", e);
        }
    }

    @Override
    public ArtifactorySearchResults getPropertiesForRepository(String repoKey) {
        try {
            String uri = artifactoryUrl + AQL_SEARCH_API;
            HttpPost getProperties = new HttpPost(uri);
            getProperties.setHeader(createAuthorizationHeader(artifactoryUsername, artifactoryPassword));
            getProperties.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
            StringEntity query = new StringEntity(String.format(REPO_PROPERTIES_QUERY, repoKey));
            getProperties.setEntity(query);
            HttpResponse propertiesResponse = client.execute(getProperties);
            return handleResponse(propertiesResponse, ArtifactorySearchResults.class);
        } catch (Exception e) {
            log.error("Failed to get properties for repository " + repoKey);
            throw new RuntimeException("Failed to obtain user plugin information", e);
        }
    }

    @Override
    public GavcSearchResults gavcSearch(String group, String artifact) {
        try {
            URIBuilder uriBuilder = new URIBuilder(artifactoryUrl + "/api/search/gavc");
            uriBuilder.addParameter("g", group);
            if (StringUtils.isNotBlank(artifact)) {
                uriBuilder.setParameter("a", artifact);
            }
            HttpGet gavcSearch = new HttpGet(uriBuilder.build());
            HttpResponse searchResult = client.execute(gavcSearch);
            return handleResponse(searchResult, GavcSearchResults.class);
        } catch (Exception e) {
            log.error("Failed to retrieve searches for group: " + group + " and artifact: " + artifact);
            throw new RuntimeException("Failed to obtain user plugin information", e);
        }
    }

    /**
     * Create basic authentication header
     */
    private Header createAuthorizationHeader(String username, String password) {
        String authRawString = username + ":" + password;
        String encoded = Base64.encode(authRawString);
        String auth = "Basic " + encoded;
        return new BasicHeader("Authorization", auth);
    }

    /**
     * Parse response to appropriate object
     */
    private <T> T handleResponse(HttpResponse response, Class<T> toClass) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            InputStream content = response.getEntity().getContent();
            String jsonString = IOUtils.toString(content);
            return objectFromJsonString(jsonString, toClass);
        }
        throw new RuntimeException("Request to Artifactory failed.");
    }

    /**
     * Convert a String represented json and convert it to an object of type T
     */
    private <T> T objectFromJsonString(String jsonString, Class<T> toClass) {
        return gson.fromJson(jsonString, toClass);
    }

    /**
     * Convert Object to JSON String
     */
    private <T> String jsonStringToObject(T object) {
        return gson.toJson(object);
    }

    private String encodePath(String string) throws UnsupportedEncodingException {
        return URLEncoder.encode(string, CharEncoding.UTF_8).replaceAll("\\+", "%20");
    }
}
