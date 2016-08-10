package org.jfrog.bamboo.bintray.client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jfrog.bamboo.admin.BintrayConfiguration;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.bintray.MavenSyncHelper;
import org.jfrog.bamboo.util.HttpUtils;

import java.util.Map;

/**
 * HttpClient for Bintray Rest calls
 *
 * @author Aviad Shikloshi
 */
public class BintrayClient {

    public static final String CLOSE_REPO_PARAM = "1";

    private HttpClient client = new DefaultHttpClient();
    private BintrayConfiguration bintrayConfig;

    public BintrayClient(BintrayConfiguration bintrayConfig) {
        this.bintrayConfig = bintrayConfig;
    }

    /**
     * Sync your build from Bintray to Maven Central
     */
    public String mavenCentralSync(String subject, String repo, String packageName, String version) {
        try {
            String apiUri = MavenSyncHelper.getMavenSyncEndpoint() + HttpUtils.encodePath(subject) + "/" + HttpUtils.encodePath(repo) + "/" +
                    HttpUtils.encodePath(packageName) + "/versions/" + HttpUtils.encodePath(version);
            MavenCentralSyncModel model = new MavenCentralSyncModel(bintrayConfig.getSonatypeOssUsername(),
                    bintrayConfig.getSonatypeOssPassword(), CLOSE_REPO_PARAM);
            HttpPost mavenSyncRequest = new HttpPost(apiUri);
            mavenSyncRequest.setHeader(HttpUtils.createAuthorizationHeader(bintrayConfig.getBintrayUsername(),
                    bintrayConfig.getBintrayApiKey()));
            String jsonString = HttpUtils.jsonStringToObject(model);
            mavenSyncRequest.setEntity(new StringEntity(jsonString));
            HttpResponse response = this.client.execute(mavenSyncRequest);
            return IOUtils.toString(response.getEntity().getContent());
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to sync with Sonatype OSS.", e);
        }
    }

    public Map<String, Object> getBintrayJsonFileLocation(ServerConfig artifactoryConfig, String buildName, String buildNumber) {
        String searchJson = MavenSyncHelper.createArtifactBuildSearchQuery(buildName, buildNumber);
        String apiUri = artifactoryConfig.getUrl() + "/api/search/buildArtifacts";
        HttpPost buildArtifactSearch = new HttpPost(apiUri);
        buildArtifactSearch.setHeader(HttpUtils.createAuthorizationHeader(artifactoryConfig.getUsername(), artifactoryConfig.getPassword()));
        buildArtifactSearch.setHeader("Content-Type", "application/json");
        try {
            buildArtifactSearch.setEntity(new StringEntity(searchJson));
            HttpResponse response = this.client.execute(buildArtifactSearch);
            String responseBody = IOUtils.toString(response.getEntity().getContent(), CharEncoding.UTF_8);
            return HttpUtils.objectFromJsonString(responseBody, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to get bintray-info.json file.", e);
        }
    }

    public Map<String, Object> downloadBintrayInfoDescriptor(ServerConfig artifactoryConfig, String fileUrl) {
        HttpGet downloadRequest = new HttpGet(fileUrl);
        downloadRequest.setHeader(HttpUtils.createAuthorizationHeader(artifactoryConfig.getUsername(), artifactoryConfig.getPassword()));
        try {
            HttpResponse fileResponse = client.execute(downloadRequest);
            String jsonDescriptorString = IOUtils.toString(fileResponse.getEntity().getContent(), CharEncoding.UTF_8);
            return HttpUtils.objectFromJsonString(jsonDescriptorString, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not download bintray-config.json file.", e);
        }

    }

}
