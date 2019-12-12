package org.jfrog.bamboo.util;

import com.google.common.base.Strings;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

/**
 * Created by Bar Belity on 09/12/2019.
 */
public class ProxyUtils {

    private static final String KEY_HTTP_PROXY_HOST = "http.proxyHost";
    private static final String KEY_HTTP_PROXY_PORT = "http.proxyPort";
    private static final String KEY_HTTP_PROXY_USER = "http.proxyUser";
    private static final String KEY_HTTP_PROXY_PASSWORD = "http.proxyPassword";
    private static final String KEY_HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";
    private static final String KEY_HTTP_PROXY_PORT_DEFAULT = "80";

    public static void setProxyConfigurationToArtifactoryClientBase(String artifactoryUrl, ArtifactoryBaseClient client) {
        ProxyConfiguration proxyConfiguration = getProxyConfiguration(artifactoryUrl);
        if (proxyConfiguration != null) {
            client.setProxyConfiguration(proxyConfiguration);
        }
    }

    public static void setProxyConfigurationToArtifactoryClientBuilderBase(String artifactoryUrl, ArtifactoryClientBuilderBase builder) {
        ProxyConfiguration proxyConfiguration = getProxyConfiguration(artifactoryUrl);
        if (proxyConfiguration != null) {
            builder.setProxyConfiguration(proxyConfiguration);
        }
    }

    public static void setProxyConfigurationToArtifactoryClientConfig(String artifactoryUrl, ArtifactoryClientConfiguration clientConf) {
        ProxyConfiguration proxyConfiguration = getProxyConfiguration(artifactoryUrl);
        if (proxyConfiguration != null) {
            clientConf.proxy.setHost(proxyConfiguration.host);
            clientConf.proxy.setPort(proxyConfiguration.port);
            clientConf.proxy.setUsername(proxyConfiguration.username);
            clientConf.proxy.setPassword(proxyConfiguration.host);
        }
    }

    /**
     * Create ProxyConfiguration from system variables passed to Bamboo.
     * @param artifactoryUrl
     * @return ProxyConfiguration object, null if proxy settings undefined or Artifactory should not be proxied.
     * @throws IllegalArgumentException
     */
    public static ProxyConfiguration getProxyConfiguration(String artifactoryUrl) throws IllegalArgumentException {

        String proxyHost = System.getProperty(KEY_HTTP_PROXY_HOST);
        if (Strings.isNullOrEmpty(proxyHost)) {
            return null;
        }

        if (isNonProxyHost(artifactoryUrl, System.getProperty(KEY_HTTP_NON_PROXY_HOSTS))) {
            return null;
        }

        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.host = proxyHost;

        String proxyPort = System.getProperty(KEY_HTTP_PROXY_PORT);
        if (Strings.isNullOrEmpty(proxyPort)) {
            proxyPort = KEY_HTTP_PROXY_PORT_DEFAULT;
        }

        try {
            proxyConfiguration.port = Integer.parseInt(proxyPort);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Provided http.proxyPort is invalid", ex);
        }

        proxyConfiguration.username = System.getProperty(KEY_HTTP_PROXY_USER);
        proxyConfiguration.password = System.getProperty(KEY_HTTP_PROXY_PASSWORD);

        return proxyConfiguration;
    }

    /**
     * Checks whether a certain host should not be proxied.
     * Host name is extracted from the provided url.
     * @param url URL to check for proxying.
     * @param nonProxyHosts List of hosts to exclude from proxying.
     * @return true of provided host should not be proxied.
     */
    private static boolean isNonProxyHost(String url, String nonProxyHosts)
    {
        if (url != null && nonProxyHosts != null && nonProxyHosts.length() > 0)
        {
            String host;
            try {
                URI artifactoryUri = new URI(url);
                host = artifactoryUri.getHost();
            } catch (URISyntaxException ex) {
                throw new RuntimeException("Failed checking url for non-proxy-host", ex);
            }

            for (StringTokenizer tokenizer = new StringTokenizer(nonProxyHosts, "|"); tokenizer.hasMoreTokens();)
            {
                String pattern = tokenizer.nextToken();
                pattern = pattern.replace(".", "\\.").replace("*", ".*");
                if (host.matches(pattern))
                {
                    return true;
                }
            }
        }

        return false;
    }
}
