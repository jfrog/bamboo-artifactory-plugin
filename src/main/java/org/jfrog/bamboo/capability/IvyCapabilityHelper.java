package org.jfrog.bamboo.capability;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.ExecutablePathUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Tomer Cohen
 */
public class IvyCapabilityHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {

    public static final String EXECUTABLE = "ant";
    public static final String HOME_ENV_VAR = "ANT_HOME";
    public static final String POSIX_HOME = "/usr/share/ant/";

    @NotNull
    @Override
    protected String getExecutableName() {
        return ExecutablePathUtils.makeBatchIfOnWindows(EXECUTABLE);
    }

    @Override
    protected String getEnvHome() {
        return HOME_ENV_VAR;
    }

    @Override
    protected String getPosixHome() {
        return POSIX_HOME;
    }

    @NotNull
    @Override
    protected String getCapabilityKey() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + "ivy.Ivy";
    }
}
