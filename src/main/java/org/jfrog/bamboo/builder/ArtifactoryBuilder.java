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

import com.atlassian.bamboo.build.BuildLoggerManager;

import java.io.Serializable;

/**
 * @author Noam Y. Tenne
 */
public interface ArtifactoryBuilder extends Serializable {

    long getArtifactoryServerId();

    boolean isHaveBuilderSpecificActivationCommand();

    String getBuilderSpecificActivationCommand();

    String getDeployableRepo();

    String getDeployerUsername();

    String getDeployerPassword();

    boolean isPublishArtifacts();

    String getDeployIncludePatterns();

    String getDeployExcludePatterns();

    boolean isRunLicenseChecks();

    String getLicenseViolationRecipients();

    String getLimitChecksToScopes();

    boolean isIncludePublishedArtifacts();

    boolean isDisableAutoLicenseDiscovery();

    BuildLoggerManager getBuildLoggerManager();
}
