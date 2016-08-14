package org.jfrog.bamboo.util;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Tamirh on 26/07/2016.
 */
public class Utils {

    public static <V> Map<String, V> filterMapKeysByPrefix(Map<String, V> map, String prefix) {
        Map<String, V> result = new HashedMap();
        if (map == null) {
            return result;
        }
        for (Map.Entry<String, V> entry : map.entrySet()) {
            if (StringUtils.startsWith(entry.getKey(), prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }


}
