package org.jfrog.bamboo.bintray;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.bamboo.util.ConstantValues;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Aviad Shikloshi
 */
public class MavenSyncHelper {

    /**
     * Update the Push to Bintray context when using MavenCentralSync with descriptor file
     */
    public static void updateBintrayActionContext(PushToBintrayAction action, Map<String, Object> bintrayJsonMap) {
        Map<String, String> aPackage = (Map<String, String>) bintrayJsonMap.get("package");
        action.setPackageName(aPackage.get("name"));
        action.setRepository(aPackage.get("repo"));
        action.setSubject(aPackage.get("subject"));
        Map<String, String> version = (Map<String, String>) bintrayJsonMap.get("version");
        action.setVersion(version.get("name"));
    }

    /**
     * Searching for the *bintray-info.json* descriptor file which is an artifact attached to the build
     */
    public static String getBintrayDescriptorFileUrl(Map<String, Object> response) {
        ArrayList<Object> results = (ArrayList<Object>) response.get("results");

        if (results.size() == 0) {
            throw new IllegalStateException("No Bintray descriptor files were find in the build.");
        }

        Map<String, String> jsonUrl = (Map<String, String>) results.get(0);
        return jsonUrl.get("downloadUri");
    }

    /**
     * Create the body for the query we are sending to Artifactory fo find the *build-json.info* file
     */
    public static String createArtifactBuildSearchQuery(String buildName, String buildNumber) {
        return "{\n" +
                "    \"buildName\": \"" +  buildName + "\",\n" +
                "    \"buildNumber\":  \"" + buildNumber + "\",\n" +
                "    \"mappings\": [\n" +
                "        {\n" +
                "            \"input\": \"(?i)[\\\\s\\\\S]*bintray-info[\\\\s\\\\S]*.json\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    public static String getMavenSyncEndpoint() {
        String bintrayUrl = System.getenv("BAMBOO_BINTRAY_URL");
        if (StringUtils.isEmpty(bintrayUrl)) {
            bintrayUrl = ConstantValues.BINTRAY_URL;
        }
        return bintrayUrl + "/maven_central_sync/";
    }

}
