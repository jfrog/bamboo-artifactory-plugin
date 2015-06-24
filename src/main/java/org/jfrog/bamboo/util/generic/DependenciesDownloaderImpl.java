package org.jfrog.bamboo.util.generic;

import com.atlassian.core.util.FileUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lior Hasson
 */
public class DependenciesDownloaderImpl implements DependenciesDownloader {
    private ArtifactoryDependenciesClient client;
    private Log log;
    private File workingDirectory;

    public DependenciesDownloaderImpl(ArtifactoryDependenciesClient client, File workingDirectory, Log log) {
        this.client = client;
        this.workingDirectory = workingDirectory;
        this.log = log;
    }

    @Override
    public ArtifactoryDependenciesClient getClient() {
        return client;
    }

    @Override
    public List<Dependency> download(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(this, log);
        return helper.downloadDependencies(downloadableArtifacts);
    }

    @Override
    public String getTargetDir(String targetDir, String relativeDir) throws IOException {
        return FilenameUtils.concat(workingDirectory.getPath(), FilenameUtils.concat(targetDir, relativeDir));
    }

    @Override
    public Map<String, String> saveDownloadedFile(InputStream is, String filePath) throws IOException {
        try {
            File newFile = new File(filePath);
            FileUtils.copyFile(is, newFile, true);

            return FileChecksumCalculator.calculateChecksums(newFile, "md5", "sha1");
        } catch (Exception e) {
            log.warn("Caught exception while saving dependency file" + e.getLocalizedMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }

        return null;
    }

    @Override
    public boolean isFileExistsLocally(String filePath, String md5, String sha1) throws IOException {
        File localFile = new File(filePath);
        if (!localFile.exists()) {
            return false;
        }

        // If it's a folder return true since we don't care about it, not going to download a folder anyway
        if (localFile.isDirectory()) {
            return true;
        }

        try {
            Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(localFile, "md5", "sha1");

            return checksumsMap != null &&
                    StringUtils.isNotBlank(md5) && StringUtils.equals(md5, checksumsMap.get("md5")) &&
                    StringUtils.isNotBlank(sha1) && StringUtils.equals(sha1, checksumsMap.get("sha1"));

        } catch (NoSuchAlgorithmException e) {
            log.warn("Could not find checksum algorithm: " + e.getLocalizedMessage());
        }

        return false;
    }

    @Override
    public void removeUnusedArtifactsFromLocal(Set<String> allResolvesFiles, Set<String> forDeletionFiles) throws IOException {
        try {
            for (String resolvedFile : forDeletionFiles) {
                File resolvedFileParent = org.apache.commons.io.FileUtils.getFile(resolvedFile).getParentFile();

                File[] fileSiblings = resolvedFileParent.listFiles();
                if (!(fileSiblings == null || fileSiblings.length == 0)) {

                    for (File sibling : fileSiblings) {
                        if (!isResolvedOrParentOfResolvedFile(allResolvesFiles, sibling.getPath())) {
                            log.info("Deleted unresolved file '" + sibling.getPath() + "'");
                            sibling.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Caught interrupted exception: " + e.getLocalizedMessage());
        }
    }

    private boolean isResolvedOrParentOfResolvedFile(Set<String> resolvedFiles, final String path) {
        return Iterables.any(resolvedFiles, new Predicate<String>() {
            public boolean apply(String filePath) {
                return StringUtils.equals(filePath, path) || StringUtils.startsWith(filePath, path);
            }
        });
    }
}
