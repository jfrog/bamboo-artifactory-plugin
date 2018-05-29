package org.jfrog.bamboo.capability;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.ExecutablePathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class GradleCapabilityHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {
    private static final String GRADLE_HOME_POSIX = "/usr/share/gradle/";
    private static final String GRADLE_EXECUTABLE_NAME = "gradle";

    @NotNull
    @Override
    protected String getExecutableName() {
        return ExecutablePathUtils.makeBatchIfOnWindows(GRADLE_EXECUTABLE_NAME);
    }

    @Nullable
    @Override
    protected String getEnvHome() {
        return "GRADLE_HOME";
    }

    @NotNull
    @Override
    protected String getPosixHome() {
        return GRADLE_HOME_POSIX;
    }

    @NotNull
    @Override
    protected List<String> getPosixHomes() {
        String posixHome = this.getPosixHome();
        return posixHome != null ? Collections.singletonList(posixHome) : Collections.emptyList();
    }

    @Override
    @NotNull
    protected String getCapabilityKey() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".gradle.Gradle";
    }
}
