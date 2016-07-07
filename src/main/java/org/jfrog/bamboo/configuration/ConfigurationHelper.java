package org.jfrog.bamboo.configuration;

import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfrog.bamboo.util.ConstantValues;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.ADMIN_CONFIG_SERVLET_CONTEXT_NAME;

/**
 * A helper class to be used for the Artifactory tasks configuration.
 */
public class ConfigurationHelper implements Serializable {

    public static final String DEFAULT_JDK = "JAVA_HOME";

    private static ConfigurationHelper instance = new ConfigurationHelper();
    private AdministrationConfigurationAccessor administrationConfigurationAccessor;
    private HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

    private ConfigurationHelper() {
        ContainerManager.autowireComponent(this);
    }

    public static ConfigurationHelper getInstance() {
        return instance;
    }

    public void setAdministrationConfigurationAccessor(AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }

    public Map<String, String> getAllVariables(PlanKey planKey) {
        HashMap<String, PlanKey> params = Maps.newHashMap();
        params.put(ConstantValues.PLAN_KEY_PARAM, planKey);
        String requestUrl = prepareRequestUrl(ADMIN_CONFIG_SERVLET_CONTEXT_NAME, params);
        GetMethod getMethod = new GetMethod(requestUrl);
        InputStream responseStream = null;
        try {
            executeMethod(requestUrl, getMethod);

            JsonFactory jsonFactory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper();
            jsonFactory.setCodec(mapper);

            responseStream = getMethod.getResponseBodyAsStream();
            if (responseStream == null) {
                return Maps.newHashMap();
            }

            JsonParser parser = jsonFactory.createJsonParser(responseStream);
            return parser.readValueAs(Map.class);
        } catch (IOException e) {
            String message = "Failed while invoking URL " + requestUrl + " to get Bamboo variables. " + e.getMessage();
            throw new RuntimeException(message, e);
        } finally {
            getMethod.releaseConnection();
            IOUtils.closeQuietly(responseStream);
        }
    }

    private String prepareRequestUrl(String servletName, Map<String, PlanKey> params) {
        String bambooBaseUrl = administrationConfigurationAccessor.getAdministrationConfiguration().getBaseUrl();
        StringBuilder builder = new StringBuilder(bambooBaseUrl);
        if (!bambooBaseUrl.endsWith("/")) {
            builder.append("/");
        }
        StringBuilder requestUrlBuilder = builder.append("plugins/servlet/").append(servletName);
        if (params.size() != 0) {
            requestUrlBuilder.append("?");

            for (Map.Entry<String, PlanKey> param : params.entrySet()) {
                if (!requestUrlBuilder.toString().endsWith("?")) {
                    requestUrlBuilder.append("&");
                }
                requestUrlBuilder.append(param.getKey()).append("=").append(EscapeChars.forFormSubmission(param.getValue().getKey()));
            }
        }

        return requestUrlBuilder.toString();
    }

    /**
     * Executes the given HTTP method
     *
     * @param requestUrl Full request URL
     * @param getMethod  HTTP GET method
     */
    private void executeMethod(String requestUrl, GetMethod getMethod) throws IOException {
        int responseCode = httpClient.executeMethod(getMethod);
        if (responseCode == HttpStatus.SC_NOT_FOUND) {
            throw new IOException("Unable to find requested resource: " + requestUrl);
        } else if (responseCode != HttpStatus.SC_OK) {
            throw new IOException("Failed to retrieve requested resource: " + requestUrl + ". Response code: " +
                    responseCode + ", Message: " + getMethod.getStatusText());
        }
    }
}
