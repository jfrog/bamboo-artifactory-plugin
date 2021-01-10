package it.org.jfrog.bamboo.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.jfrog.testing.IntegrationTestsHelper.*;
import static org.jfrog.bamboo.configuration.BuildParamsOverrideManager.*;

/**
 * @author yahavi
 */
public class Utils {
    private static final String ENCODED_AUTH = "Basic " + Base64.encodeBase64String("admin:admin".getBytes(StandardCharsets.ISO_8859_1));
    public static final String BAMBOO_TEST_URL = "http://localhost:6990/bamboo";

    // Environment variables names
    public static final String GRADLE_HOME_ENV = "GRADLE_HOME";
    public static final String MAVEN_HOME_ENV = "MAVEN_HOME";

    // Environment variables values
    public static final String GRADLE_HOME = System.getenv(GRADLE_HOME_ENV);
    public static final String MAVEN_HOME = System.getenv(MAVEN_HOME_ENV);

    /**
     * Create overriding plan variables. The Bamboo Artifactory plugin uses them in order to override
     * configured Artifactory URL, username, password and repositories.
     *
     * @param localRepoKey - Local test repository key
     * @param jcenter      - JCenter repository key
     * @return overriding plan variables
     */
    public static Map<String, String> createOverrideVars(String localRepoKey, String jcenter) {
        return new HashMap<String, String>() {{
            put(OVERRIDE_ARTIFACTORY_DEPLOYER_URL, ARTIFACTORY_URL);
            put(OVERRIDE_ARTIFACTORY_DEPLOYER_USERNAME, ARTIFACTORY_USERNAME);
            put(OVERRIDE_ARTIFACTORY_DEPLOYER_PASSWORD, ARTIFACTORY_PASSWORD);
            put(OVERRIDE_ARTIFACTORY_DEPLOY_REPO, localRepoKey);
            put(OVERRIDE_ARTIFACTORY_RESOLVER_URL, ARTIFACTORY_URL);
            put(OVERRIDE_ARTIFACTORY_RESOLVER_USERNAME, ARTIFACTORY_USERNAME);
            put(OVERRIDE_ARTIFACTORY_RESOLVER_PASSWORD, ARTIFACTORY_PASSWORD);
            put(OVERRIDE_ARTIFACTORY_RESOLVE_REPO, jcenter);
        }};
    }

    /**
     * Create common plan environment variables.
     *
     * @return plan environment variables
     */
    public static Map<String, String> createEnv() {
        return new HashMap<String, String>() {{
            // The collect env methodology should ignore this variable:
            put("DONT_COLLECT", "FOO");

            // The collect env methodology should add this variable:
            put("COLLECT", "BAR");
        }};
    }

    /**
     * Download the build log after test failure.
     *
     * @param log         - The tests logger
     * @param buildNumber - The build number of the test plan
     * @param planKey     - The test plan key
     * @return the build log or empty string if error occurred.
     */
    public static String downloadBuildLog(Logger log, int buildNumber, String planKey) {
        String url = String.format("%s/download/%s-JOB1/build_logs/%s-JOB1-%s.log", BAMBOO_TEST_URL, planKey, planKey, buildNumber);
        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, ENCODED_AUTH);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity);
            }
        } catch (IOException e) {
            log.error("Couldn't read build log: " + ExceptionUtils.getRootCauseMessage(e));
        }
        return "";
    }
}
