package org.jfrog.bamboo.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.jfrog.build.api.BuildInfoConfigProperties;

import java.util.List;
import java.util.Map;

/**
 * Utility class that serves as a helper for common operations of a task.
 *
 * @author Tomer Cohen
 */
public abstract class TaskUtils {

    private static final char PROPERTIES_DELIMITER = ';';
    private static final char KEY_VALUE_SEPARATOR = '=';

    private TaskUtils() {
        throw new IllegalAccessError();
    }

    /**
     * Get an escaped version of the environment map that is to be passed onwards to the extractors. Bamboo escapes the
     * key of the property and replaces all '.' into '_' as well as adds the "bamboo" prefixHence a conversion back is
     * needed.
     *
     * @param env The original environment map.
     * @return The escaped environment map.
     */
    public static Map<String, String> getEscapedEnvMap(Map<String, String> env) {
        Map<String, String> result = Maps.newHashMap();
        if (env != null) {
            for (Map.Entry<String, String> entry : env.entrySet()) {
                String escaped = entry.getKey().replace('_', '.');
                escaped = StringUtils.removeStart(escaped, "bamboo.");
                result.put(escaped, entry.getValue());
            }
        }
        return result;
    }

    /**
     * Append the path of the build info properties file as a system property to the list of arguments that is given to
     * the build (as a -D param).
     */
    public static void appendBuildInfoPropertiesArgument(List<String> arguments, String buildInfoPropertiesFile) {
        if ((arguments != null) && StringUtils.isNotBlank(buildInfoPropertiesFile)) {
            arguments.add(Commandline.quoteArgument("-D" + BuildInfoConfigProperties.PROP_PROPS_FILE + "=" +
                    buildInfoPropertiesFile));
        }
    }

    /**
     * Create Multimap that represent build/deployment matrix param to attach uploaded artifacts
     *
     * @param propertiesInput String that separated by semicolon to parse into map
     * @return Multimap that represents the deployment properties, empty map if no properties attaches
     */
    public static ArrayListMultimap<String, String> extractMatrixParamFromString(String propertiesInput) {
        ArrayListMultimap<String, String> matrixParams = ArrayListMultimap.create();
        String[] matrixParamString = StringUtils.split(propertiesInput, PROPERTIES_DELIMITER);
        for (String s : matrixParamString) {
            String[] keyValueArr = StringUtils.split(s, KEY_VALUE_SEPARATOR);
            boolean validProperty = keyValueArr.length == 2;
            if (validProperty) {
                // No whitespace allowed in key
                String formatKey = keyValueArr[0].replace(" ", StringUtils.EMPTY);
                matrixParams.put(formatKey, keyValueArr[1].trim());
            }
        }
        return matrixParams;
    }
}
