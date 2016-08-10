package org.jfrog.bamboo.util.version;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.bamboo.context.GradleBuildContext;
import org.jfrog.bamboo.context.Maven3BuildContext;
import org.jfrog.bamboo.release.action.ModuleVersionHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Helper class that helps with extracting versions from property or pom files.
 *
 * @author Tomer Cohen
 */
public abstract class VersionHelper {

    protected AbstractBuildContext context;

    protected VersionHelper(AbstractBuildContext context) {
        this.context = context;
    }

    /**
     * Filter the version properties in preparation for release.
     */
    public abstract List<ModuleVersionHolder> filterPropertiesForRelease(Plan plan, int latestBuildNumberWithBi)
            throws RepositoryException, IOException;

    /**
     * Add version fields to the build's configuration
     */
    public abstract void addVersionFieldsToConfiguration(Map parameters, Map<String, String> configuration,
            String versionConfiguration, Map<String, String> taskConfiguration);

    public String calculateReleaseVersion(String fromVersion) {
        return fromVersion.replace("-SNAPSHOT", "");
    }

    /**
     * Calculates the next snapshot version based on the current release version
     *
     * @param fromVersion The version to bump to next development version
     * @return The next calculated development (snapshot) version
     */
    public String calculateNextVersion(String fromVersion) {
        // first turn it to release version
        fromVersion = calculateReleaseVersion(fromVersion);
        String nextVersion;
        int lastDotIndex = fromVersion.lastIndexOf('.');
        try {
            if (lastDotIndex != -1) {
                // probably a major minor version e.g., 2.1.1
                String minorVersionToken = fromVersion.substring(lastDotIndex + 1);
                String nextMinorVersion;
                int lastDashIndex = minorVersionToken.lastIndexOf('-');
                if (lastDashIndex != -1) {
                    // probably a minor-buildNum e.g., 2.1.1-4 (should change to 2.1.1-5)
                    String buildNumber = minorVersionToken.substring(lastDashIndex + 1);
                    int nextBuildNumber = Integer.parseInt(buildNumber) + 1;
                    nextMinorVersion = minorVersionToken.substring(0, lastDashIndex + 1) + nextBuildNumber;
                } else {
                    nextMinorVersion = Integer.parseInt(minorVersionToken) + 1 + "";
                }
                nextVersion = fromVersion.substring(0, lastDotIndex + 1) + nextMinorVersion;
            } else {
                // maybe it's just a major version; try to parse as an int
                int nextMajorVersion = Integer.parseInt(fromVersion) + 1;
                nextVersion = nextMajorVersion + "";
            }
        } catch (NumberFormatException e) {
            return fromVersion;
        }
        return nextVersion + "-SNAPSHOT";
    }

    public static VersionHelper getHelperAccordingToType(AbstractBuildContext context,
                                                         CapabilityContext capabilityContext) {
        if (context instanceof GradleBuildContext) {
            return new GradleVersionHelper(context);
        }
        if (context instanceof Maven3BuildContext) {
            return new MavenVersionHelper(context, capabilityContext);
        }
        return null;
    }
}
