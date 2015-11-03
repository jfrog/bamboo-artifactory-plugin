package org.jfrog.bamboo.bintray.client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jfrog.bamboo.admin.BintrayConfig;
import org.jfrog.bamboo.util.ConstantValues;
import org.jfrog.bamboo.util.HttpUtils;

/**
 * HttpClient for Bintray Rest calls
 *
 * @author Aviad Shikloshi
 */
public class BintrayClient {

    private HttpClient client = new DefaultHttpClient();
    private BintrayConfig bintrayConfig;

    public BintrayClient(BintrayConfig bintrayConfig) {
        this.bintrayConfig = bintrayConfig;
    }

    /**
     * Sync your build from Bintray to Maven Central
     */
    public MavenSyncResponse mavenCentralSync(String subject, String repo, String packageName, String version) {
        try {
            String apiUri = getMavenSyncEndpoint() + HttpUtils.encodePath(subject) + "/" + HttpUtils.encodePath(repo) + "/" +
                    HttpUtils.encodePath(packageName) + "/versions/" + HttpUtils.encodePath(version);
            MavenCentralSyncModel model = new MavenCentralSyncModel(bintrayConfig.getSonatypeOssUsername(),
                    bintrayConfig.getSonatypeOssPassword(), "1");
            HttpPost mavenSyncRequest = new HttpPost(apiUri);
            mavenSyncRequest.setHeader(HttpUtils.createAuthorizationHeader(bintrayConfig.getBintrayUsername(),
                    bintrayConfig.getBintrayApiKey()));
            String jsonString = HttpUtils.jsonStringToObject(model);
            mavenSyncRequest.setEntity(new StringEntity(jsonString));
            HttpResponse response = this.client.execute(mavenSyncRequest);
            String responseBody = IOUtils.toString(response.getEntity().getContent(), CharEncoding.UTF_8);
            return HttpUtils.objectFromJsonString(responseBody, MavenSyncResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to sync with Sonatype OSS.", e);
        }
    }

    private String getMavenSyncEndpoint() {
        String bintrayUrl = System.getenv("BAMBOO_BINTRAY_URL");
        if (StringUtils.isEmpty(bintrayUrl)) {
            bintrayUrl = ConstantValues.BINTRAY_URL;
        }
        return bintrayUrl + "/maven_central_sync/";
    }

}
