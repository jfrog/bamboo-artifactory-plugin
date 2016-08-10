package org.jfrog.bamboo.util;

import org.apache.commons.beanutils.BeanUtils;

import java.util.Map;

/**
 * @author mamo
 */
public abstract class BeanUtilsHelper {

    public static void populateWithPrefix(Object object, Map<String, String> env, String prefix) {
        try {
            for (Map.Entry<String, String> entry : Utils.filterMapKeysByPrefix(env, prefix).entrySet()) {
                BeanUtils.setProperty(object, entry.getKey().substring(prefix.length()), entry.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not set populate bean properties." , e);
        }
    }
}
