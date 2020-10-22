package org.jfrog.bamboo.capability;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Created by Bar Belity on 11/10/2020.
 */
public class NugetCapabilityHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {
    private static final String NUGET_HOME_POSIX = "/usr/share/nuget/";
    private static final String NUGET_EXECUTABLE_NAME = "nuget";

    @NotNull
    @Override
    protected String getExecutableName() {
        return NUGET_EXECUTABLE_NAME;
    }

    @NotNull
    @Override
    protected List<String> getPosixHomes() {
        return Collections.singletonList(NUGET_HOME_POSIX);
    }

    @Override
    @NotNull
    protected String getCapabilityKey() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".nuget.Nuget";
    }
}
