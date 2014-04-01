package org.jfrog.bamboo.release.provider;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.build.extractor.maven.reader.ModuleName;
import org.jfrog.build.extractor.maven.reader.ProjectReader;
import org.jfrog.build.extractor.maven.transformer.PomTransformer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Release provider that performs operations specific for a Maven type build.
 *
 * @author Tomer Cohen
 */
public class MavenReleaseProvider extends AbstractReleaseProvider {

    protected MavenReleaseProvider(AbstractBuildContext buildContext, BuildContext buildDefinition,
                                   BuildLogger buildLogger, CustomVariableContext customVariableContext, CredentialsAccessor credentialsAccessor) {
        super(buildContext, buildDefinition, buildLogger, customVariableContext, credentialsAccessor);
    }

    @Override
    protected Map<? extends String, ? extends String> getTaskConfiguration(BuildDefinition definition) {
        return TaskDefinitionHelper.findMavenDefinition(definition.getTaskDefinitions()).getConfiguration();
    }

    @Override
    public boolean transformDescriptor(Map<String, String> conf, boolean release)
            throws IOException, InterruptedException, RepositoryException {
        String moduleVersionConf = conf.get(MODULE_VERSION_CONFIGURATION);
        if (StringUtils.isBlank(moduleVersionConf)) {
            return false;
        }
        if (CFG_USE_EXISTING_VERSION.equals(moduleVersionConf)) {
            return false;
        }
        File rootDir = getSourceDir();
        if (rootDir == null) {
            return false;
        }
        Map<String, String> map = buildMapAccordingToStatus(conf, release);
        Map<ModuleName, String> buildVersionByModule = buildVersionByModule(map);
        File pom = getRootPom(rootDir);
        ProjectReader reader = new ProjectReader(pom);
        boolean changed = false;
        Map<ModuleName, File> modules = reader.read();
        for (Map.Entry<ModuleName, File> entry : modules.entrySet()) {
            String transformMessage = release ? "release" : "next development";
            log("Transforming: " + entry.getValue().getAbsolutePath() + " to " + transformMessage);
            coordinator.edit(entry.getValue());
            PomTransformer transformer = new PomTransformer(entry.getKey(), buildVersionByModule, getScmUrl(release),
                    release);
            changed |= transformer.transform(entry.getValue());
        }
        return changed;
    }

    private String getScmUrl(boolean release) {
        if (coordinator.isSubversion()) {
            if (release) {
                if (buildContext.releaseManagementContext.isCreateVcsTag()) {
                    return buildContext.releaseManagementContext.getTagUrl();
                }
            } else {
                if (buildContext.releaseManagementContext.isCreateVcsTag()) {
                    return coordinator.getRemoteUrlForPom();
                }
            }
        }
        return null;
    }

    private Map<ModuleName, String> buildVersionByModule(Map<String, String> moduleNames) {
        Map<ModuleName, String> result = Maps.newHashMap();
        for (Map.Entry<String, String> entry : moduleNames.entrySet()) {
            String[] groupIdArtifactId = StringUtils.split(entry.getKey(), ":");
            result.put(new ModuleName(groupIdArtifactId[0], groupIdArtifactId[1]), entry.getValue());
        }
        return result;
    }

    private File getRootPom(File rootDir) {
        String subDirectory = ((Maven3BuildContext) buildContext).getWorkingSubDirectory();
        File rootPomFile;
        if (StringUtils.isNotBlank(subDirectory)) {
            rootPomFile = new File(rootDir, subDirectory + "/pom.xml");
        } else {
            rootPomFile = new File(rootDir, "pom.xml");
        }
        return rootPomFile;
    }
}
