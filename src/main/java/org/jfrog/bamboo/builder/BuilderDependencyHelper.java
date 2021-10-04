/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.context.PackageManagersContext;
import org.jfrog.bamboo.util.PluginProperties;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public class BuilderDependencyHelper implements Serializable {

    private AdministrationConfiguration administrationConfiguration;
    private AdministrationConfigurationAccessor administrationConfigurationAccessor;
    private final String builderKey;

    public BuilderDependencyHelper(String builderKey) {
        this.builderKey = builderKey;
    }

    public String downloadDependenciesAndGetPath(File bambooTemp, String planKey, PackageManagersContext context, String dependencyName)
            throws IOException {
        String pluginKey = PluginProperties.getPluginKey();
        String pluginDescriptorKey = PluginProperties.getPluginDescriptorKey();

        if (bambooTemp == null) {
            return null;
        }
        File planTemp = new File(bambooTemp, planKey);
        if (!planTemp.exists()) {
            planTemp.mkdir();
        }

        //Search for older plugin dirs and remove if any exist
        File[] planTempFiles = planTemp.listFiles();
        if (null !=  planTempFiles) {
            for (File buildDirChild : planTempFiles) {
                String buildDirChildName = buildDirChild.getName();
                if (buildDirChildName.startsWith(pluginDescriptorKey) &&
                        (!buildDirChildName.equals(pluginKey) || buildDirChildName.endsWith("-SNAPSHOT"))) {
                    FileUtils.deleteQuietly(buildDirChild);
                    buildDirChild.delete();
                }
            }
        }

        File pluginDir = new File(planTemp, pluginKey);
        File builderDependencyDir = new File(pluginDir, builderKey);
        if (builderDependencyDir.isDirectory()) {
            // Validates extractor existence
            List<String> files = Arrays.asList(builderDependencyDir.list());
            if (!files.isEmpty() && files.contains(dependencyName)) {
                return builderDependencyDir.getCanonicalPath();
            }
        } else {
            builderDependencyDir.mkdirs();
        }

        String bambooBaseUrl = getBambooBaseUrl(context);
        bambooBaseUrl = StringUtils.stripEnd(bambooBaseUrl, "/");
        if (StringUtils.isNotBlank(bambooBaseUrl)) {
            StringBuilder builder = new StringBuilder(bambooBaseUrl);
            if (!bambooBaseUrl.endsWith("/")) {
                builder.append("/");
            }
            String dependencyBaseUrl = builder.append("download/resources/")
                    .append(pluginDescriptorKey).append("/builder/dependencies/").toString();
            try {
                downloadDependencies(dependencyBaseUrl, builderDependencyDir, dependencyName);
                return builderDependencyDir.getCanonicalPath();
            } catch (IOException ioe) {
                FileUtils.deleteDirectory(builderDependencyDir);
                throw ioe;
            }
        }

        return null;
    }

    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }

    public void setAdministrationConfigurationAccessor(
            AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }

    /**
     * Returns the base URL of this Bamboo instance.<br> This method is needed since we must download dependencies from
     * the Bamboo server.<br> The URL can generally be found in {@link com.atlassian.bamboo.configuration.AdministrationConfiguration},
     *
     * @param context
     * @return Bamboo base URL if found. Null if running in an un-recognized type of agent.
     */
    private String getBambooBaseUrl(PackageManagersContext context) {
        if (administrationConfiguration != null) {
            return administrationConfiguration.getBaseUrl();
        } else if (administrationConfigurationAccessor != null) {
            return administrationConfigurationAccessor.getAdministrationConfiguration().getBaseUrl();
        } else if (StringUtils.isNotBlank(context.getBaseUrl())) {
            return context.getBaseUrl();
        }
        return null;
    }

    private void downloadDependencies(String dependencyBaseUrl, File builderDependencyDir, String dependencyFileName)
            throws IOException {
        HttpClient client = new HttpClient();
        String dependencyUrl = dependencyBaseUrl + dependencyFileName;
        GetMethod getMethod = new GetMethod(dependencyUrl);

        try {
            int responseStatus;
            try {
                responseStatus = client.executeMethod(getMethod);
            } catch (IOException e) {
                throw new IOException("Failed while invoking URL: " + dependencyUrl + "  " + e.getMessage());
            }
            if (responseStatus == HttpStatus.SC_NOT_FOUND) {
                throw new IOException("Unable to find required dependency: " + dependencyUrl);
            } else if (responseStatus != HttpStatus.SC_OK) {
                throw new IOException("Error while requesting required dependency: " + dependencyUrl + ". Status: "
                        + responseStatus + ", Message: " + getMethod.getStatusText());
            }

            try (InputStream responseBodyAsStream = getMethod.getResponseBodyAsStream()) {
                if (responseBodyAsStream == null) {
                    throw new IOException("Requested dependency: " + dependencyUrl + ", but received a null response stream.");
                }

                File file = new File(builderDependencyDir, dependencyFileName);
                if (file.isFile()) {
                    return;
                }
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    IOUtils.copy(responseBodyAsStream, fileOutputStream);
                }
            }
        } finally {
            getMethod.releaseConnection();
        }
    }
}
