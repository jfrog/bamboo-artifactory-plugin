package org.jfrog.bamboo.release.scm.svn;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.repository.svn.SVNClientManagerFactory;
import com.atlassian.bamboo.repository.svn.SvnRepository;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.utils.SystemProperty;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.PlanRepositoryDefinition;
import com.atlassian.spring.container.ContainerManager;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.release.scm.AbstractScmCoordinator;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.IOException;
import java.util.Map;

/**
 * @author Tomer Cohen
 */
public class SubversionCoordinator extends AbstractScmCoordinator {
    private SubversionManager scmManager;
    private final Map<String, String> configuration;
    private boolean tagCreated;
    private EncryptionService encryptionService = (EncryptionService) ContainerManager.getComponent("encryptionService");

    public SubversionCoordinator(BuildContext context, PlanRepositoryDefinition repository, Map<String, String> configuration,
                                 BuildLogger buildLogger, CustomVariableContext customVariableContext, CredentialsAccessor credentialsAccessor) {
        super(context, repository, buildLogger, customVariableContext, credentialsAccessor);
        this.configuration = configuration;
    }

    @Override
    public void prepare() throws IOException {
        scmManager = new SubversionManager(context, repository, getClientManager(), buildLogger, customVariableContext);
    }

    @Override
    public void beforeReleaseVersionChange() throws IOException {
    }

    @Override
    public void afterSuccessfulReleaseVersionBuild() throws IOException {
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        boolean createTag = context.releaseManagementContext.isCreateVcsTag();
        if (createTag) {
            String tagUrl = context.releaseManagementContext.getTagUrl();
            String tagComment = context.releaseManagementContext.getTagComment();
            try {
                scmManager.createTag(tagUrl, tagComment);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
            tagCreated = true;
        }
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        super.afterDevelopmentVersionChange(modified);
        if (modified) {
            String comment = context.releaseManagementContext.getNextDevelopmentComment();
            scmManager.commitWorkingCopy(comment);
        }
    }

    @Override
    public void buildCompleted(BuildContext buildContext) throws IOException, InterruptedException {
        AbstractBuildContext context = AbstractBuildContext.createContextFromMap(configuration);
        CurrentBuildResult result = buildContext.getBuildResult();
        if (!BuildState.SUCCESS.equals(result.getBuildState())) {
            // build has failed, make sure to delete the tag and revert the working copy
            scmManager.safeRevertWorkingCopy();
            if (tagCreated) {
                String tagUrl = context.releaseManagementContext.getTagUrl();
                String tagComment = context.releaseManagementContext.getTagComment();
                scmManager.safeRevertTag(tagUrl, tagComment);
            }
        }
    }

    @Override
    public String getRemoteUrlForPom() {
        return scmManager.getRemoteUrl();
    }

    @Override
    public boolean isSubversion() {
        return true;
    }

    private SVNClientManager getClientManager() {
        try {
            String userName = repository.getVcsLocation().getConfiguration().get("repository.svn.username");
            String password = encryptionService.decrypt(repository.getVcsLocation().getConfiguration().get("repository.svn.userPassword"));
            SVNClientManagerFactory clientFactory = new SVNClientManagerFactory();
            ISVNAuthenticationManager authManager =
                    SVNWCUtil.createDefaultAuthenticationManager(null, userName, password, SystemProperty.SVN_CACHE_CREDENTIALS.getValue(false));
            return clientFactory.getSVNClientManager(SvnRepository.DEFAULT_SVN_OPTIONS, authManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
