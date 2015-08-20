package org.jfrog.bamboo.bintray;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jfrog.bamboo.bintray.client.AQLEntry;
import org.jfrog.bamboo.bintray.client.GavcSearchEntry;
import org.jfrog.bamboo.bintray.client.JfClient;
import org.jfrog.bamboo.util.ActionLog;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills in Push to Bintray configuration values when using bintrayOsoPush plugin
 * <p>
 *
 * Spring migration class
 * @author Aviad Shikloshi
 */
public class BintrayPropertiesCollector {

    private static Logger log = Logger.getLogger(BintrayPropertiesCollector.class);

    private static final String GAV_REGEXP = "(.*):(.*)(?=:)";
    private static final String GROUP_PATTERN = "(.*:.*)(?=:)";

    public static final int GROUP = 0;
    public static final int ARTIFACT = 1;
    public static final int REPO_KEY = 4;

    private List<String> moduleIds;
    private ActionLog bintrayLog;

    public BintrayPropertiesCollector(Build buildInfo, ActionLog bintrayLog) {
        this.bintrayLog = bintrayLog;
        bintrayLog.logMessage("Adding modules to BintrayPropertiesCollector:");
        moduleIds = Lists.newArrayList();
        if (buildInfo != null) {
            for (Module module : buildInfo.getModules()) {
                String moduleId = module.getId();
                bintrayLog.logMessage(moduleId);
                moduleIds.add(moduleId);
            }
        }
    }

    /**
     * Get Bintray package name from the properties
     *
     * @param properties repository properties list
     * @return package name as it was extracted from the properties
     */
    public String getPackageNameFromProperties(List<AQLEntry> properties) {
        bintrayLog.logMessage("Getting package name by Artifactory properties.");
        // system ID is the key that its value is the Bintray repository  we are going to publish into
        String systemId = generateSystemId();
        bintrayLog.logMessage("SystemId was generated: " + systemId);
        for (AQLEntry artifactoryProp : properties) {
            String currentKey = artifactoryProp.getKey();
            if (currentKey.contains(systemId)) {
                String packageName = artifactoryProp.getValue();
                bintrayLog.logMessage("PackageName for Bintray is " + packageName);
                return packageName;
            }
        }
        bintrayLog.logError("Could not get Bintray package name from properties.");
        return StringUtils.EMPTY;
    }

    /**
     * @return Bintray package name
     */
    private String generateSystemId() {
        bintrayLog.logMessage("Generating systemId.");
        Pattern pattern = Pattern.compile(GROUP_PATTERN);
        List<String> occurrencesList = Lists.newArrayList();
        for (String moduleId : moduleIds) {
            Matcher m = pattern.matcher(moduleId);
            if (m.find()) {
                String s = m.group(1);
                bintrayLog.logMessage("Adding " + s + " as a potential systemId");
                occurrencesList.add(s);
            }
        }
        return mostFrequentInMap(countOccurrences(occurrencesList));
    }

    private String generatePackageNameByOccurrence() {
        List<String> occurrences = Lists.newArrayList();
        Pattern pattern = Pattern.compile(GAV_REGEXP);
        for (String moduleId : moduleIds) {
            Matcher m = pattern.matcher(moduleId);
            if (m.find()) {
                occurrences.add(m.group(1));
            }
        }
        return mostFrequentInMap(countOccurrences(occurrences));
    }

    /**
     * A way to get the repository key for the files just uploaded with the build
     */
    public String getRepoKeyByArtifactorySearch(JfClient jfClient) {
        bintrayLog.logMessage("Searching repository key by artifact GAVC search.");
        if (!moduleIds.isEmpty()) {
            String[] gavc = moduleIds.get(0).split(":");
            List<GavcSearchEntry> resultsList = jfClient.gavcSearch(gavc[GROUP], gavc[ARTIFACT]).getResults();
            if (!resultsList.isEmpty()) {
                String uriStr = resultsList.get(0).getUri();
                bintrayLog.logMessage("Using URI - " + uriStr + " to get repository key.");
                URI fileURI = URI.create(uriStr);
                String uriFullPath = fileURI.getPath();
                String[] pathComponents = uriFullPath.split("/");
                if (pathComponents.length > 6) {
                    String artifactoryKey = pathComponents[REPO_KEY];
                    bintrayLog.logMessage("Found repository key related to current build: " + artifactoryKey);
                    return artifactoryKey;
                }
                bintrayLog.logError("Could not parse path: " + uriFullPath);
            } else {
                bintrayLog.logError("GAVC search returned empty list for: " + Arrays.deepToString(gavc));
            }
        }
        bintrayLog.logError("Could not find repository key for this build.");
        return StringUtils.EMPTY;
    }

    private Map<String, Integer> countOccurrences(List<String> occurrencesList) {
        Map<String, Integer> counterMap = Maps.newHashMap();
        for (String s : occurrencesList) {
            if (counterMap.containsKey(s)) {
                Integer occurrences = counterMap.get(s);
                counterMap.put(s, ++occurrences);
            } else {
                counterMap.put(s, 0);
            }
        }
        return counterMap;
    }

    private String mostFrequentInMap(Map<String, Integer> occurrencesMap) {
        return Collections.max(
                occurrencesMap.entrySet(),
                new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                        return o1.getValue() > o2.getValue() ? 1 : -1;
                    }
                }).getKey();
    }

    // Collects the pre configured Environment variables /System properties to populate to configuration
    public String getFromSystem(String key) {
        String value;
        value = System.getenv("BINTRAY_" + key.toUpperCase());
        if (StringUtils.isBlank(value)) {
            value = System.getProperty("bintray." + key.toLowerCase());
        }
        if (StringUtils.isBlank(value)) {
            bintrayLog.logError("No value found for: " + key);
        } else {
            String logValue = key.toUpperCase().equals("PASSPHRASE") ? value.replaceAll("(?s).", "*") : value;
            bintrayLog.logMessage("Retrieved " + key + " value from system: " + logValue);
        }
        return value;
    }
}
