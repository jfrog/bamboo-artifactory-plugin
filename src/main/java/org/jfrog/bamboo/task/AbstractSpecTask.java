package org.jfrog.bamboo.task;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.context.GenericContext;
import org.jfrog.bamboo.util.TaskUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Dima Nevelev on 25/09/2018.
 */
public abstract class AbstractSpecTask {

    protected CustomVariableContext customVariableContext;
    protected String fileSpec;
    private BuildLogger buildLogger;

    public AbstractSpecTask(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    protected File getWorkingDirectory(@NotNull CommonTaskContext context) {
        return context.getWorkingDirectory();
    }

    protected String getJobConfigurationSpec(@NotNull CommonTaskContext context) {
        return new GenericContext(context.getConfigurationMap()).getJobConfigurationSpec();
    }

    protected String getFilePathSpec(@NotNull CommonTaskContext context) {
        return new GenericContext(context.getConfigurationMap()).getFilePathSpec();
    }

    protected Boolean isFileSpecInJobConfiguration(@NotNull CommonTaskContext context) {
        return new GenericContext(context.getConfigurationMap()).isFileSpecInJobConfiguration();
    }

    protected void initFileSpec(@NotNull CommonTaskContext context) throws IOException {
        buildLogger = context.getBuildLogger();
        setFileSpec(context);
        buildLogger.addBuildLogEntry("Spec: " + fileSpec);
        validateFileSpec();
    }

    private void setFileSpec(@NotNull CommonTaskContext context) throws IOException {
        if (isFileSpecInJobConfiguration(context)) {
            buildLogger.addBuildLogEntry("Using task configuration spec");
            fileSpec = getJobConfigurationSpec(context);
            return;
        }
        String specFileLocation = getFilePathSpec(context);
        buildLogger.addBuildLogEntry("Using spec from file located at: " + specFileLocation);
        fileSpec = TaskUtils.getSpecFromFile(getWorkingDirectory(context), specFileLocation);
        fileSpec = customVariableContext.substituteString(fileSpec);
    }

    protected void validateFileSpec() {
        if (StringUtils.isBlank(fileSpec)) {
            throw new IllegalStateException("Artifactory Spec can't be empty");
        }
    }
}
