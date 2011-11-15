package org.jfrog.bamboo.release.provider;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;
import org.jfrog.bamboo.release.scm.AbstractScmCoordinator;
import org.jfrog.bamboo.release.scm.ScmCoordinator;
import org.jfrog.bamboo.util.version.ScmHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Tomer Cohen
 */
public abstract class AbstractReleaseProvider implements ReleaseProvider {
    private static final Logger log = Logger.getLogger(AbstractReleaseProvider.class);

    private boolean isReleaseEnabled;
    protected ScmCoordinator coordinator;
    protected final AbstractBuildContext buildContext;
    protected final BuildLogger buildLogger;
    protected final BuildContext context;

    protected AbstractReleaseProvider(AbstractBuildContext buildContext, BuildContext context,
            BuildLogger buildLogger) {
        this.context = context;
        this.buildContext = buildContext;
        this.buildLogger = buildLogger;
        this.isReleaseEnabled = buildContext.releaseManagementContext.isReleaseMgmtEnabled();
        this.coordinator = AbstractScmCoordinator.createScmCoordinator(context,
                getTaskConfiguration(context.getBuildDefinition()), buildLogger);
    }

    protected abstract Map<? extends String, ? extends String> getTaskConfiguration(BuildDefinition definition);

    public static ReleaseProvider createReleaseProvider(AbstractBuildContext buildContext, BuildContext context,
            BuildLogger buildLogger) {
        if (buildContext instanceof GradleBuildContext) {
            return new GradleReleaseProvider(buildContext, context, buildLogger);
        }
        if (buildContext instanceof Maven3BuildContext) {
            return new MavenReleaseProvider(buildContext, context, buildLogger);
        }
        return null;
    }

    public void prepare() throws IOException {
        if (isReleaseEnabled) {
            try {
                coordinator.prepare();
            } catch (Exception e) {
                failBuild(e);
                throw new IOException(e);
            }
        }
    }

    public void afterDevelopmentVersionChange(boolean modified) throws IOException {
        try {
            coordinator.afterDevelopmentVersionChange(modified);
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    public void beforeReleaseVersionChange() throws IOException, InterruptedException {
        try {
            coordinator.beforeReleaseVersionChange();
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    public void afterReleaseVersionChange(boolean modified) throws IOException {
        try {
            coordinator.afterReleaseVersionChange(modified);
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    public void afterSuccessfulReleaseVersionBuild() throws IOException {
        try {
            coordinator.afterSuccessfulReleaseVersionBuild();
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    public void beforeDevelopmentVersionChange() throws IOException {
        try {
            coordinator.beforeDevelopmentVersionChange();
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    public void buildCompleted(BuildContext buildContext) throws IOException {
        try {
            coordinator.buildCompleted(buildContext);
        } catch (Exception e) {
            failBuild(e);
            throw new IOException(e);
        }
    }

    private void failBuild(Exception e) {
        CurrentBuildResult result = context.getBuildResult();
        result.addBuildErrors(Arrays.asList(e.getMessage()));
        result.setBuildState(BuildState.FAILED);
    }

    public String getCurrentCheckoutBranch() {
        return coordinator.getCheckoutBranch();
    }

    public void setCurrentCheckoutBranch(String checkoutBranch) {
        coordinator.setCheckoutBranch(checkoutBranch);
    }

    public String getCurrentWorkingBranch() {
        return coordinator.getCurrentWorkingBranch();
    }

    public void setCurrentWorkingBranch(String currentWorkingBranch) {
        coordinator.setCurrentWorkingBranch(currentWorkingBranch);
    }

    public void setBaseCommitIsh(String commitIsh) {
        coordinator.setCommitIsh(commitIsh);
    }

    public String getBaseCommitIsh() {
        return coordinator.getCommitIsh();
    }

    public void setReleaseBranchCreated(boolean releaseBranchCreated) {
        coordinator.setReleaseBranchCreated(releaseBranchCreated);
    }

    public boolean isReleaseBranchCreated() {
        return coordinator.isReleaseBranchCreated();
    }

    protected Map<String, String> buildMapAccordingToStatus(Map<String, String> conf, boolean release) {
        List<ModuleVersionHolder> holders = ModuleVersionHolder.buildFromConf(conf);
        Map<String, String> result = Maps.newHashMap();
        for (ModuleVersionHolder holder : holders) {
            String value = release ? holder.getReleaseValue() : holder.getNextIntegValue();
            result.put(holder.getKey(), value);
        }
        return result;
    }

    @Nullable
    protected File getSourceDir() throws RepositoryException {
        return ScmHelper.getCheckoutDirectory(context);
    }

    protected void log(String message) {
        log.info(buildLogger.addBuildLogEntry("[RELEASE] " + message));
    }
}
