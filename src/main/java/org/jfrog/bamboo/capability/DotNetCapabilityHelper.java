package org.jfrog.bamboo.capability;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Created by Bar Belity on 14/10/2020.
 */
public class DotNetCapabilityHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {
    private static final String DOTNET_HOME_POSIX = "/usr/share/dotnet/";
    private static final String DOTNET_EXECUTABLE_NAME = "dotnet";

    @NotNull
    @Override
    protected String getExecutableName() {
        return DOTNET_EXECUTABLE_NAME;
    }

    @NotNull
    @Override
    protected List<String> getPosixHomes() {
        return Collections.singletonList(DOTNET_HOME_POSIX);
    }

    @Override
    @NotNull
    protected String getCapabilityKey() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".dotnet.Dotnet";
    }
}
