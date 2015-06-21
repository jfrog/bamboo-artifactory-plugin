package org.jfrog.bamboo.util.deployment;

import com.atlassian.bamboo.task.TaskDefinition;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds artifacts we want to upload to Artifactory when using Bamboo deployment task
 *
 * @author Aviad Shikloshi
 */
public class FilesCollector {

    private static final String PATH_KEY = "localPath";

    private String projectRoot;
    private List<String> subDirectories;

    private Map<String, Set<File>> collectedFiles;

    public FilesCollector(String projectRoot, TaskDefinition taskDefinition) {
        this.projectRoot = projectRoot;
        this.subDirectories = Lists.newArrayList();
        getSubDirectories(taskDefinition);
    }

    public Map<String, Set<File>> getCollectedFiles() {
        if (collectedFiles == null) {
            collectFiles();
        }
        return this.collectedFiles;
    }

    /**
     * Collect all artifacts
     */
    private void collectFiles() {

        this.collectedFiles = Maps.newHashMap();

        File dir = new File(projectRoot);
        File[] preconfiguredDirectories = dir.listFiles(new PreConfiguredDirectoriesFilter(this.subDirectories));

        for (File subDir : preconfiguredDirectories) {
            Iterator<File> files = FileUtils.iterateFiles(subDir, null, true);
            Set<File> currentPathFiles = Sets.newHashSet();
            this.collectedFiles.put(subDir.getAbsolutePath().substring(this.projectRoot.length() + 1), currentPathFiles);
            while (files.hasNext()) {
                File current = files.next();
                if (current.isFile()) {
                    currentPathFiles.add(current);
                }
            }
        }

        File[] otherFiles = dir.listFiles(new RootDirFilesFilter(this.projectRoot, this.subDirectories));
        Set<File> rootDirFiles = Sets.newHashSet();
        this.collectedFiles.put("", rootDirFiles);
        for (File otherFile : otherFiles) {
            if (otherFile.isFile()) {
                rootDirFiles.add(otherFile);
            }
        }
    }

    /**
     * Get all sub directories user specified in the ArtifactDownload task
     *
     * @param artifactsDownloadRtd configuration for the ArtifactDownload task
     */
    private void getSubDirectories(TaskDefinition artifactsDownloadRtd) {
        if (artifactsDownloadRtd != null) {
            Map<String, String> downloadTaskConfiguration = artifactsDownloadRtd.getConfiguration();
            for (String s : downloadTaskConfiguration.keySet()) {
                // Each PATH_KEY_{number} in ArtifactoryDownload configuration map holds the directory path as the value,
                // we want to collect all of the sub directories.
                if (s.startsWith(PATH_KEY)) {
                    subDirectories.add(downloadTaskConfiguration.get(s));
                }
            }
        }
    }

    /**
     * Accepts only the folders that was downloaded directly to the projects root directory
     */
    private class RootDirFilesFilter implements FileFilter {

        private String rootDirectory;
        private List<String> subDirs;

        public RootDirFilesFilter(String rootDirectory, List<String> subDirs) {
            this.rootDirectory = rootDirectory;
            this.subDirs = subDirs;
        }

        @Override
        public boolean accept(File pathname) {
            String substring = pathname.getAbsolutePath().substring(rootDirectory.length() + 1);
            return !subDirs.contains(substring);
        }
    }

    /**
     * Accepts directories that was specified in the Artifact Download configuration task
     */
    private class PreConfiguredDirectoriesFilter implements FileFilter {

        private List<String> subDirs;

        public PreConfiguredDirectoriesFilter(List<String> subDirs) {
            this.subDirs = subDirs;
        }

        @Override
        public boolean accept(File pathname) {
            boolean accept = false;
            if (pathname.isDirectory()) {
                for (String subDir : subDirs) {
                    if (StringUtils.endsWith(pathname.getAbsolutePath(), subDir)) {
                        accept = true;
                        break;
                    }
                }
            }
            return accept;
        }
    }
}