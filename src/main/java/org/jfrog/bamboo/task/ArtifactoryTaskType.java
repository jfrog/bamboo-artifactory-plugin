package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.atlassian.utils.process.ExternalProcess;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.jfrog.bamboo.context.AbstractBuildContext;

import java.io.File;
import java.io.IOException;

/**
 * Common super type for all tasks
 *
 * @author Tomer Cohen
 */
public abstract class ArtifactoryTaskType implements TaskType {

    protected static final String JDK_LABEL_KEY = "system.jdk.";
    private final TestCollationService testCollationService;

    protected ArtifactoryTaskType(TestCollationService testCollationService) {
        this.testCollationService = testCollationService;
    }

    /**
     * Get the build logger that will print messages to Bamboo's log from the context.
     *
     * @param taskContext The task context.
     * @return The build logger.
     */
    public BuildLogger getBuildLogger(TaskContext taskContext) {
        return taskContext.getBuildLogger();
    }

    /**
     * Get the executable to run for the build based upon the build's context.
     *
     * @param buildContext The build context.
     * @return The path to the executable to run for the build
     * @throws TaskException Thrown if the path to the executable defined in the build's {@link
     *                       com.atlassian.bamboo.v2.build.agent.capability.Capability} does not exist.
     */
    public abstract String getExecutable(AbstractBuildContext buildContext) throws TaskException;

    public TaskResult collectTestResults(AbstractBuildContext buildContext, TaskContext taskContext,
            ExternalProcess process) {
        TaskResultBuilder builder = TaskResultBuilder.create(taskContext).checkReturnCode(process);
        if (buildContext.isTestChecked() && buildContext.getTestDirectory() != null) {
            testCollationService.collateTestResults(taskContext, buildContext.getTestDirectory());
            builder.checkTestFailures();
        }
        return builder.build();
    }


    /**
     * Get the path to the Java execute that was defined for the build.
     *
     * @param context           The build context that is defined for the current build environment.
     * @param capabilityContext The capability context of the build.
     * @return The path to the Java home.
     */
    public String getJavaExe(AbstractBuildContext context, CapabilityContext capabilityContext) {
        StringBuilder binPathBuilder = new StringBuilder(getJavaHome(context, capabilityContext));
        if (SystemUtils.IS_OS_WINDOWS) {
            binPathBuilder.append("bin").append(File.separator).append("java.exe");
        } else {
            // IBM's AIX JDK has different locations
            String aixJdkLocation = "jre" + File.separator + "sh" + File.separator + "java";
            File aixJdk = new File(binPathBuilder.toString() + aixJdkLocation);
            if (aixJdk.isFile()) {
                binPathBuilder.append(aixJdkLocation);
            } else {
                binPathBuilder.append("bin").append(File.separator).append("java");
            }
        }
        String binPath = binPathBuilder.toString();
        binPath = getCanonicalPath(binPath);
        return binPath;
    }

    /**
     * Get the path to the Java home that was defined for the build.
     *
     * @param context           The build context that is defined for the current build environment.
     * @param capabilityContext The capability context of the build.
     * @return The path to the Java home.
     */
    protected String getJavaHome(AbstractBuildContext context, CapabilityContext capabilityContext) {
        String jdkHome;
        String jdkCapabilityKey = (new StringBuilder()).append(JDK_LABEL_KEY).append(context.getJdkLabel()).toString();
        ReadOnlyCapabilitySet capabilitySet = capabilityContext.getCapabilitySet();
        if (capabilitySet == null) {
            return null;
        }
        Capability capability = capabilitySet.getCapability(jdkCapabilityKey);
        if (capability != null) {
            jdkHome = capability.getValue();
        } else {
            return null;
        }
        if (StringUtils.isBlank(jdkHome)) {
            return null;
        }
        StringBuilder binPathBuilder = getPathBuilder(jdkHome);

        return binPathBuilder.toString();
    }

    /**
     * Returns a {@link StringBuilder} starting with a given base path and ending with a file-system separator
     *
     * @param basePath Base path
     * @return String builder
     */
    public StringBuilder getPathBuilder(String basePath) {
        StringBuilder confPathBuilder = new StringBuilder(basePath);
        if (!basePath.endsWith(File.separator)) {
            confPathBuilder.append(File.separator);
        }
        return confPathBuilder;
    }

    /**
     * @return The canonical path for a path.
     */
    public String getCanonicalPath(String path) {
        if (StringUtils.contains(path, " ")) {
            try {
                File f = new File(path);
                path = f.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException("IO Exception trying to get canonical path of item: " + path, e);
            }
        }
        return path;
    }
}
