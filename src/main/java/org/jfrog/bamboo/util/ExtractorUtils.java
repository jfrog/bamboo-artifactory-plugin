package org.jfrog.bamboo.util;

import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.ReadOnlyCapabilitySet;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.types.Commandline;
import org.jfrog.bamboo.context.AbstractBuildContext;
import org.jfrog.build.api.BuildInfoConfigProperties;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Utility class that serves as a helper for common operations of an extractor.
 *
 * @author Tomer Cohen
 */
public abstract class ExtractorUtils {

    public static final String JDK_LABEL_KEY = "system.jdk.";
    public static final boolean IS_WINDOWS = SystemUtils.IS_OS_WINDOWS;

    private ExtractorUtils() {
        throw new IllegalAccessError();
    }

    /**
     * Get an escaped version of the environment map that is to be passed onwards to the extractors. Bamboo escapes the
     * key of the property and replaces all '.' into '_' as well as adds the "bamboo" prefixHence a conversion back is
     * needed.
     *
     * @param env The original environment map.
     * @return The escaped environment map.
     */
    public static Map<String, String> getEscapedEnvMap(Map<String, String> env) {
        Map<String, String> result = Maps.newHashMap();
        if (env == null || env.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String escaped = entry.getKey().replace('_', '.');
            escaped = StringUtils.removeStart(escaped, "bamboo.");
            result.put(escaped, entry.getValue());
        }
        return result;
    }

    /**
     * Get the path to the Java home that was defined for the build.
     *
     * @param context           The build context that is defined for the current build environment.
     * @param capabilityContext The capability context of the build.
     * @return The path to the Java home.
     */
    public static String getMavenHome(AbstractBuildContext context, CapabilityContext capabilityContext) {
        String jdkHome;
        String jdkCapabilityKey = (new StringBuilder()).append(JDK_LABEL_KEY).append(context.getJdkLabel()).toString();
        Capability capability = capabilityContext.getCapabilitySet().getCapability(jdkCapabilityKey);
        if (capability != null) {
            jdkHome = capability.getValue();
        } else {
            return null;
        }
        if (StringUtils.isBlank(jdkHome)) {
            return null;
        }
        StringBuilder binPathBuilder = getPathBuilder(jdkHome);
        if (IS_WINDOWS) {
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
    public static String getJavaHome(AbstractBuildContext context, CapabilityContext capabilityContext) {
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
        if (IS_WINDOWS) {
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
     * Returns a general usage path {@link StringBuilder} ending with a file-system separator
     *
     * @param homePath Base home path
     * @return String builder
     */
    public static StringBuilder getPathBuilder(String homePath) {
        StringBuilder confPathBuilder = new StringBuilder(homePath);
        if (!homePath.endsWith(File.separator)) {
            confPathBuilder.append(File.separator);
        }
        return confPathBuilder;
    }

    /**
     * @return The canonical path for a path.
     */
    public static String getCanonicalPath(String path) {
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

    /**
     * Append the path of the build info properties file as a system property to the list of arguments that is given to
     * the build (as a -D param).
     */
    public static void appendBuildInfoPropertiesArgument(List<String> arguments, String buildInfoPropertiesFile) {
        arguments.add(Commandline.quoteArgument("-D" + BuildInfoConfigProperties.PROP_PROPS_FILE + "=" +
                buildInfoPropertiesFile));
    }
}
