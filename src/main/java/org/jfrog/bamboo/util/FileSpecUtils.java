package org.jfrog.bamboo.util;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Bar Belity on 05/12/2019.
 */
public class FileSpecUtils {

    public static String getFileSpec(boolean isFileSpecInJobConfiguration, String jobConfigurationSpec, String filePathSpec, File workingDirectory, CustomVariableContext customVariableContext, BuildLogger buildLogger) throws IOException {
        String fileSpec = jobConfigurationSpec;
        if (isFileSpecInJobConfiguration) {
            buildLogger.addBuildLogEntry("Using task configuration spec");
            return fileSpec;
        }
        buildLogger.addBuildLogEntry("Using spec from file located at: " + filePathSpec);
        fileSpec = getSpecFromFile(workingDirectory, filePathSpec);
        fileSpec = customVariableContext.substituteString(fileSpec);

        return fileSpec;
    }

    public static void validateFileSpec(String fileSpec) {
        if (StringUtils.isBlank(fileSpec)) {
            throw new IllegalStateException("Artifactory Spec can't be empty");
        }
    }

    public static String getSpecFromFile(File sourceCodeDirectory, String specFilePath) throws IOException {
        Path path = Paths.get(specFilePath);
        File specFile = path.isAbsolute() ? path.toFile() : Paths.get(sourceCodeDirectory.getAbsolutePath(), specFilePath).toFile();

        try (FileInputStream fis = new FileInputStream(specFile)) {
            byte[] data = new byte[(int) specFile.length()];
            fis.read(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }
}
