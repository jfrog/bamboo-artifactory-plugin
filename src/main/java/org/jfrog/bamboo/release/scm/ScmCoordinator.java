/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.release.scm;

import java.io.File;
import java.io.IOException;

/**
 * Base interface for specific scm coordinators.
 *
 * @author Yossi Shaul
 */
public interface ScmCoordinator {
    /**
     * Called immediately after the coordinator is created.
     */
    void prepare() throws IOException;

    /**
     * Called before changing to release version.
     */
    void beforeReleaseVersionChange() throws IOException;

    /**
     * Called after a change to release version.
     *
     * @param modified
     */
    void afterReleaseVersionChange(boolean modified) throws IOException;

    /**
     * Called after a successful release build.
     */
    void afterSuccessfulReleaseVersionBuild() throws IOException, InterruptedException;

    /**
     * Called before changing to next development version.
     */
    void beforeDevelopmentVersionChange() throws IOException;

    /**
     * Called after a change to the next development version.
     *
     * @param modified
     */
    void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException;

    /**
     * Called after the build has completed and the result was finalized.
     *
     * @param buildContext
     */
    void buildCompleted(com.atlassian.bamboo.v2.build.BuildContext buildContext)
            throws IOException, InterruptedException;

    String getRemoteUrlForPom();

    /**
     * @return The checkout branch of the current working copy.
     */
    String getCheckoutBranch();

    /**
     * Set the checkout branch of the current working copy.<p> <b>NOTE:</b> This is here since the instance of the
     * {@code ScmCoordinator} is not saved from the pre-build action and the post build action, and the post build
     * action needs some information from the pre-build. This method should only be used <b>ONCE</b> for this
     * scenario.</p>
     *
     * @param checkoutBranch The current checkout branch
     */
    void setCheckoutBranch(String checkoutBranch);

    /**
     * @return The checkout working of the current working copy.
     */
    public String getCurrentWorkingBranch();

    /**
     * Set the working branch of the current working copy.<p> <b>NOTE:</b> This is here since the instance of the {@code
     * ScmCoordinator} is not saved from the pre-build action and the post build action, and the post build action needs
     * some information from the pre-build. This method should only be used <b>ONCE</b> for this scenario.</p>
     *
     * @param currentWorkingBranch The current working branch.
     */
    public void setCurrentWorkingBranch(String currentWorkingBranch);

    public void setCommitIsh(String commitIsh);

    public String getCommitIsh();

    public boolean isReleaseBranchCreated();

    public void setReleaseBranchCreated(boolean releaseBranchCreated);

    boolean isSubversion();

    /**
     * Called before a file is modified.
     *
     * @param file The file that is about to be modified.
     */
    void edit(File file) throws IOException, InterruptedException;

    int getCurrentChangeListId();

    void setCurrentChangeListId(int currentChangeListId);
}
