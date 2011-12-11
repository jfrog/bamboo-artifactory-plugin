package org.jfrog.bamboo.release.provider;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.util.TaskDefinitionHelper;
import org.jfrog.build.extractor.release.PropertiesTransformer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Release provider that performs operations specific for a Gradle type build.
 *
 * @author Tomer Cohen
 */
public class GradleReleaseProvider extends AbstractReleaseProvider {

    protected GradleReleaseProvider(AbstractBuildContext internalContext, BuildContext context,
            BuildLogger buildLogger) {
        super(internalContext, context, buildLogger);
    }

    @Override
    protected Map<? extends String, ? extends String> getTaskConfiguration(BuildDefinition definition) {
        return TaskDefinitionHelper.findGradleDefinition(definition.getTaskDefinitions()).getConfiguration();
    }

    @Override
    public boolean transformDescriptor(Map<String, String> conf, boolean release)
            throws RepositoryException, IOException, InterruptedException {
        File rootDir = getSourceDir();
        if (rootDir == null) {
            return false;
        }
        Map<String, String> map = buildMapAccordingToStatus(conf, release);
        File fileToTransform = new File(rootDir, "gradle.properties");
        String transformMessage = release ? "release" : "next development";
        log("Transforming: " + fileToTransform.getAbsolutePath() + " to " + transformMessage);
        PropertiesTransformer transformer = new PropertiesTransformer(fileToTransform, map);
        return transformer.transform();
    }
}
