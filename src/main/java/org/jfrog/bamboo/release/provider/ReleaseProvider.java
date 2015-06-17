package org.jfrog.bamboo.release.provider;

import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.BuildContext;

import java.io.IOException;
import java.util.Map;

/**
 * A release provider abstraction, that performs specific SCM tasks which are common to both Maven/Gradle, as well as
 * specific tasks which are to be performed for each type of build
 *
 * @author Tomer Cohen
 */
public interface ReleaseProvider {

    String CURRENT_CHECKOUT_BRANCH = "currentCheckoutBranch";
    String CURRENT_WORKING_BRANCH = "currentWorkingBranch";
    String BASE_COMMIT_ISH = "baseCommitIsh";
    String RELEASE_BRANCH_CREATED = "releaseBranchCreated";
    String MODULE_VERSION_CONFIGURATION = "moduleVersionConfiguration";
    String CFG_USE_EXISTING_VERSION = "useExistingVersion";
    String MODIFIED_FILES_FOR_RELEASE = "modifiedFilesForReleaseVersion";
    String CFG_VERSION_PER_MODULE = "versionPerModule";
    String CFG_ONE_VERSION = "oneVersionAllModules";
    String CURRENT_CHANGE_LIST_ID = "currentChangeListId";

    /**
     * Initial event of the build. Prepare the provider, set up the SCM client.
     */
    void prepare() throws IOException;

    /**
     * Event that is called after a change has been done in the descriptor/property file, if the file has been modified
     * an SCM operation of commit will occur.
     *
     * @param modified Flag to determine whether a modification has occurred within the descriptor/property file.
     */
    void afterDevelopmentVersionChange(boolean modified) throws IOException;

    /**
     * Event that is called before a change is taking place in the descriptor/property file towards a <b>release</b>
     * version. This will take place after the build has completed successfully.
     */
    void beforeReleaseVersionChange() throws IOException, InterruptedException;

    /**
     * Transform the descriptor's version according to the configuration and the release flag.
     *
     * @param conf    The configuration of the Module->Version to change.
     * @param release Flag to indicate to transform the versions into a release version or a snapshot version.
     * @return True if a change has taken place in the descriptor, false otherwise.
     */
    boolean transformDescriptor(Map<String, String> conf, boolean release) throws RepositoryException, IOException,
            InterruptedException;

    /**
     * Event that is called <b>after</b> the release version has been changed in the descriptor/property file. If the
     * file has been modified an SCM operation of commit will occur.
     *
     * @param modified
     */
    void afterReleaseVersionChange(boolean modified) throws IOException;

    /**
     * Event that is called after the the successful release of a build. This will trigger the creation of a tag if it
     * set by the user.
     */
    void afterSuccessfulReleaseVersionBuild() throws IOException;

    /**
     * Event this is called <b>before</b>the change of a descriptor/property file.
     */
    void beforeDevelopmentVersionChange() throws IOException;

    /**
     * Event that is called after the build has completed, this method will determine if the build has completed
     * successfully in accordance to the build context. If the build failed an SCM revert will occur resetting the
     * workspace back to its original state.
     *
     * @param buildContext The build context of the build that is running.
     */
    void buildCompleted(BuildContext buildContext) throws IOException;

    /**
     * @return The current checkout branch that the the build runs on.
     */
    String getCurrentCheckoutBranch();

    /**
     * Set the current checkout branch that the release works on. <p> <b>NOTE:</b> This shouldn't be used directly, this
     * is here due to the fact that the same instance of the {@link org.jfrog.bamboo.release.scm.ScmCoordinator} cannot
     * be used by the pre-build action and the post build action, so this property is set to be shared</p>
     *
     * @param checkoutBranch The current checkout branch.
     */
    void setCurrentCheckoutBranch(String checkoutBranch);

    /**
     * @return The current working branch that the build runs on.
     */
    public String getCurrentWorkingBranch();

    /**
     * Set the current working branch that the release works on. <p> <b>NOTE:</b> This shouldn't be used directly, this
     * is here due to the fact that the same instance of the {@link org.jfrog.bamboo.release.scm.ScmCoordinator} cannot
     * be used by the pre-build action and the post build action, so this property is set to be shared</p>
     *
     * @param currentWorkingBranch The current working branch
     */
    public void setCurrentWorkingBranch(String currentWorkingBranch);

    /**
     * Set the current base commit hash that the release works on. <p> <b>NOTE:</b> This shouldn't be used directly,
     * this is here due to the fact that the same instance of the {@link org.jfrog.bamboo.release.scm.ScmCoordinator}
     * cannot be used by the pre-build action and the post build action, so this property is set to be shared</p>
     *
     * @param commitIsh The current base commit hash
     */
    public void setBaseCommitIsh(String commitIsh);

    /**
     * @return The current commit hash.
     */
    public String getBaseCommitIsh();

    /**
     * @return True if the release branch was created.
     */
    public boolean isReleaseBranchCreated();

    public void setReleaseBranchCreated(boolean releaseBranchCreated);

    public int getCurrentChangeListId();

    public void setCurrentChangeListId(int changeListId);

    void reloadFromConfig(Map<String, String> configuration);
}
