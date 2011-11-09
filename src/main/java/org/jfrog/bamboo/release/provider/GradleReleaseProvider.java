package org.jfrog.bamboo.release.provider;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.jfrog.bamboo.context.AbstractBuildContext;
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

    protected GradleReleaseProvider(AbstractBuildContext buildContext, BuildContext plan, PlanKey planKey,
            BuildLogger buildLogger) {
        super(buildContext, plan, planKey, buildLogger);
    }

    public boolean transformDescriptor(Map<String, String> conf, boolean release, String planKey)
            throws RepositoryException, IOException, InterruptedException {
        File rootDir = getSourceDir(planKey);
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
