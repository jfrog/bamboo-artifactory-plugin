package org.jfrog.bamboo.bintray;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jfrog.bamboo.bintray.client.ArtifactoryResponseEntry;
import org.jfrog.bamboo.bintray.client.GavcSearchEntry;
import org.jfrog.bamboo.bintray.client.JfClient;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills in Push to Bintray configuration values when using bintrayOsoPush plugin
 *
 * @author Aviad Shikloshi
 */
public class BintrayPropertiesCollector {

    private static final String GAV_REGEXP = "(.*):(.*)(?=:)";
    private static final String GROUP_PATTERN = "(.*:.*)(?=:)";
    public static final int GROUP = 0;
    public static final int ARTIFACT = 1;
    public static final int REPO_KEY = 4;

    private List<String> moduleIds;

    public BintrayPropertiesCollector(Build buildInfo) {
        moduleIds = Lists.newArrayList();
        if (buildInfo != null) {
            for (Module module : buildInfo.getModules()) {
                moduleIds.add(module.getId());
            }
        }
    }

    /**
     * Get Bintray package name from the properties
     *
     * @param properties repository properties list
     * @return package name as it was extracted from the properties
     */
    public String getPackageNameFromProperties(List<ArtifactoryResponseEntry> properties) {
        // system ID is the key that its value is the Bintray repository  we are going to publish into
        String systemId = generateSystemId();
        for (ArtifactoryResponseEntry artifactoryProp : properties) {
            String currentKey = artifactoryProp.getKey();
            if (currentKey.contains(systemId)) {
                return artifactoryProp.getValue();
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * @return Bintray package name
     */
    private String generateSystemId() {
        Pattern pattern = Pattern.compile(GROUP_PATTERN);
        List<String> occurrencesList = Lists.newArrayList();
        for (String moduleId : moduleIds) {
            Matcher m = pattern.matcher(moduleId);
            if (m.find()) {
                occurrencesList.add(m.group(1));
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
        if (!moduleIds.isEmpty()) {
            String[] gavc = moduleIds.get(0).split(":");
            List<GavcSearchEntry> resultsList = jfClient.gavcSearch(gavc[GROUP], gavc[ARTIFACT]).getResults();
            if (!resultsList.isEmpty()) {
                String uriStr = resultsList.get(0).getUri();
                URI fileURI = URI.create(uriStr);
                String uriFullPath = fileURI.getPath();
                String[] pathComponents = uriFullPath.split("/");
                if (pathComponents.length > 6) {
                    return pathComponents[REPO_KEY];
                }
            }
        }
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
        return value;
    }


}
