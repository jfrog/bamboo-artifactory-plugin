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
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.PluginProperties;

import java.io.*;

/**
 * @author Noam Y. Tenne
 */
public class BuilderDependencyHelper implements Serializable {

    private AdministrationConfiguration administrationConfiguration;
    private AdministrationConfigurationAccessor administrationConfigurationAccessor;
    private String builderKey;

    public BuilderDependencyHelper(String builderKey) {
        this.builderKey = builderKey;
    }

    public String downloadDependenciesAndGetPath(File rootDir, AbstractBuildContext context, String dependencyKey)
            throws IOException {
        String pluginKey = PluginProperties.getPluginKey();
        String pluginDescriptorKey = PluginProperties.getPluginDescriptorKey();

        if (rootDir == null) {
            return null;
        }
        File rootDirParent = rootDir.getParentFile();

        //Search for older plugin dirs and remove if any exist
        for (File buildDirChild : rootDirParent.listFiles()) {
            String buildDirChildName = buildDirChild.getName();
            if (buildDirChildName.startsWith(pluginDescriptorKey) &&
                    (!buildDirChildName.equals(pluginKey) || buildDirChildName.endsWith("-SNAPSHOT"))) {
                FileUtils.deleteQuietly(buildDirChild);
                buildDirChild.delete();
            }
        }

        File pluginDir = new File(rootDirParent, pluginKey);
        File builderDependencyDir = new File(pluginDir, builderKey);
        if (builderDependencyDir.isDirectory()) {
            if (builderDependencyDir.list().length != 0) {
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
                downloadDependencies(dependencyBaseUrl, builderDependencyDir, dependencyKey);
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
    private String getBambooBaseUrl(AbstractBuildContext context) {
        if (administrationConfiguration != null) {
            return administrationConfiguration.getBaseUrl();
        } else if (administrationConfigurationAccessor != null) {
            return administrationConfigurationAccessor.getAdministrationConfiguration().getBaseUrl();
        } else if (StringUtils.isNotBlank(context.getBaseUrl())) {
            return context.getBaseUrl();
        }
        return null;
    }

    private void downloadDependencies(String dependencyBaseUrl, File builderDependencyDir, String dependencyKey)
            throws IOException {
        HttpClient client = new HttpClient();
        String dependencyFileName = PluginProperties.getPluginProperty(dependencyKey);
        String dependencyUrl = dependencyBaseUrl + dependencyFileName;
        GetMethod getMethod = new GetMethod(dependencyUrl);

        InputStream responseBodyAsStream = null;
        FileOutputStream fileOutputStream = null;

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

            responseBodyAsStream = getMethod.getResponseBodyAsStream();
            if (responseBodyAsStream == null) {
                throw new IOException("Requested dependency: " + dependencyUrl +
                        ", but received a null response stream.");
            }

            File file = new File(builderDependencyDir, dependencyFileName);
            if (!file.isFile()) {
                fileOutputStream = new FileOutputStream(file);
                IOUtils.copy(responseBodyAsStream, fileOutputStream);
            }
        } finally {
            getMethod.releaseConnection();
            IOUtils.closeQuietly(responseBodyAsStream);
            IOUtils.closeQuietly(fileOutputStream);
        }
    }
}
