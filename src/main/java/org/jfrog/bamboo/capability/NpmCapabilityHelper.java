package org.jfrog.bamboo.capability;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class NpmCapabilityHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {
    private static final String NPM_HOME_POSIX = "/usr/share/npm/";
    private static final String NPM_EXECUTABLE_NAME = "npm";

    @NotNull
    @Override
    protected String getExecutableName() {
        return NPM_EXECUTABLE_NAME;
    }

    @NotNull
    @Override
    protected List<String> getPosixHomes() {
        return Collections.singletonList(NPM_HOME_POSIX);
    }

    @Override
    @NotNull
    protected String getCapabilityKey() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".npm.Npm";
    }
}
