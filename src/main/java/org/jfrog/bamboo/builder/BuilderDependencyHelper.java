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
import com.atlassian.bamboo.configuration.AdministrationConfigurationManager;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.PluginUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public class BuilderDependencyHelper implements Serializable {

    private AdministrationConfiguration administrationConfiguration;
    private AdministrationConfigurationManager administrationConfigurationManager;
    private String builderKey;

    public BuilderDependencyHelper(String builderKey) {
        this.builderKey = builderKey;
    }

    public String downloadDependenciesAndGetPath(File buildDir, AbstractBuildContext context, String... dependencyKeys)
            throws IOException {
        String pluginKey = PluginUtils.getPluginKey();
        String pluginDescriptorKey = PluginUtils.getPluginDescriptorKey();

        if (buildDir == null) {
            return null;
        }
        File buildDirParent = buildDir.getParentFile();

        //Search for older plugin dirs and remove if any exist
        for (File buildDirChild : buildDirParent.listFiles()) {
            if (buildDirChild.getName().startsWith(pluginDescriptorKey) && !buildDirChild.getName().equals(pluginKey)) {
                FileUtils.deleteQuietly(buildDirChild);
                buildDirChild.delete();
            }
        }

        File pluginDir = new File(buildDirParent, pluginKey);
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
                downloadDependencies(dependencyBaseUrl, builderDependencyDir, dependencyKeys);
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
        } else if (administrationConfigurationManager != null) {
            return administrationConfigurationManager.getAdministrationConfiguration().getBaseUrl();
        } else if (StringUtils.isNotBlank(context.getBaseUrl())) {
            return context.getBaseUrl();
        }
        return null;
    }

    private void downloadDependencies(String dependencyBaseUrl, File builderDependencyDir, String... dependencyKeys)
            throws IOException {
        HttpClient client = new HttpClient();
        for (String dependencyKey : dependencyKeys) {
            String dependencyFileName = PluginUtils.getPluginProperty(dependencyKey);
            String dependencyUrl = dependencyBaseUrl + dependencyFileName;
            GetMethod getMethod = new GetMethod(dependencyUrl);

            InputStream responseBodyAsStream = null;
            FileOutputStream fileOutputStream = null;

            try {
                int responseStatus = client.executeMethod(getMethod);
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

    public void setAdministrationConfigurationManager(
            AdministrationConfigurationManager administrationConfigurationManager) {
        this.administrationConfigurationManager = administrationConfigurationManager;
    }
}
