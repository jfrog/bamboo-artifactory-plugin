package org.jfrog.bamboo.util;

import com.google.common.collect.Iterables;
import org.apache.commons.beanutils.BeanUtils;

import java.util.Map;

import static com.atlassian.bamboo.utils.BambooPredicates.startsWith;

/**
 * @author mamo
 */
public abstract class BeanUtilsHelper {

    public static void populateWithPrefix(Object object, Map<String, String> env, String prefix) {
        try {
            for (String key : Iterables.filter(env.keySet(), startsWith(prefix))) {
                BeanUtils.setProperty(object, key.substring(prefix.length()), env.get(key));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not set populate bean properties." , e);
        }
    }
}
