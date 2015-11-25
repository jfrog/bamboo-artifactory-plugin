package org.jfrog.bamboo.capability;

import com.atlassian.bamboo.command.SimpleExecuteStreamHandler;
import com.atlassian.bamboo.utils.SystemProperty;
import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.ExecutablePathUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Maven3CapabilityHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {
    private static final Logger log = Logger.getLogger(Maven3CapabilityHelper.class);
    private static final String MAVEN3_HOME_POSIX = "/usr/share/maven3/";
    private static final Pattern MAVEN_VERSION_3 = Pattern.compile("3\\.\\d+\\.\\d+");
    private static final String M2_EXECUTABLE_NAME = "mvn";
    private static final long GET_VERSION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final Pattern VERSION_PATTERN = Pattern.compile("Apache Maven (\\S+).*");


    @NotNull
    @Override
    protected String getExecutableName() {
        return ExecutablePathUtils.makeBatchIfOnWindows(M2_EXECUTABLE_NAME);
    }

    @Nullable
    @Override
    protected String getEnvHome() {
        return SystemProperty.MAVEN2_HOME.getValue();
    }

    @NotNull
    @Override
    protected String getPosixHome() {
        return MAVEN3_HOME_POSIX;
    }

    @Override
    @NotNull
    protected String getCapabilityKey() {
        return CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".maven.Artifactory Maven 3";
    }

    @Override
    protected Predicate<File> getValidityPredicate() {
        return new MavenVersionMatcher(MAVEN_VERSION_3);
    }

    public static class MavenVersionMatcher implements Predicate<File> {
        private final Pattern pattern;

        public MavenVersionMatcher(@NotNull Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean apply(@Nullable final File input) {
            if (input == null) {
                return false;
            }
            String mavenVersion = getMavenVersion(input);
            return mavenVersion != null && pattern.matcher(mavenVersion).matches();
        }

        /**
         * Parse output of "mvn --version". This method works only with Maven 2 and Maven 3
         *
         * @return text representation of Maven version number or null if not possible to obtain version number
         */
        @Nullable
        private static String getMavenVersion(final File mavenExecutable) {
            if (!mavenExecutable.isFile()) {
                log.info("Failed to get Maven version, file does not exists: " + mavenExecutable);
                return null;
            }

            Commandline commandline = new Commandline();
            if (SystemUtils.IS_OS_WINDOWS) {
                commandline.setExecutable("cmd.exe");
                commandline.addArguments(new String[]{"/c", mavenExecutable.getPath()});
            } else {
                commandline.setExecutable(mavenExecutable.getPath());
            }
            commandline.addArguments(new String[]{"--version"});

            List<String> commandStdOut = Lists.newArrayList();
            List<String> commandStdErr = Lists.newArrayList();

            ExecuteWatchdog watchdog = new ExecuteWatchdog(GET_VERSION_TIMEOUT);
            Execute execute =
                    new Execute(new SimpleExecuteStreamHandler(commandStdOut, commandStdErr, "MavenVersion"), watchdog);
            execute.setWorkingDirectory(SystemUtils.getJavaIoTmpDir());
            execute.setEnvironment(new String[]{"M2_HOME=" + mavenExecutable.getParentFile().getParent()});
            execute.setCommandline(commandline.getCommandline());

            try {
                int exitValue = execute.execute();
                if (!Execute.isFailure(exitValue)) {
                    for (String line : commandStdOut) {
                        Matcher matcher = VERSION_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            return matcher.group(1);
                        }
                    }
                    log.warn("Failed to get Maven version, unable to analyze output: \n" + StringUtils
                            .join(commandStdOut, "\n"));
                } else {
                    if (watchdog.killedProcess()) {
                        log.warn("Failed to get Maven version, command timed out");
                    } else {
                        log.warn("Failed to get Maven version, command failed: \n" +
                                StringUtils.join(commandStdErr, "\n"));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to execute command", e);
            }

            return null;
        }
    }
}
