package org.jfrog.bamboo.release.action;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Holder object that holds information about module versions. It will hold the key or module depending on the type of
 * build, the value that is there now, and whether this is a release prop (relevant for a Gradle build). It will also
 * store the new value that is to be replaced.
 *
 * @author Tomer Cohen
 */
public class ModuleVersionHolder implements Serializable {

    private final String key;
    private final String originalValue;
    private boolean releaseProp;
    private String releaseValue;
    private String nextIntegValue;

    public ModuleVersionHolder(String key, String originalValue, boolean releaseProp) {
        this.key = key;
        this.originalValue = originalValue;
        this.releaseProp = releaseProp;
        this.releaseValue = "";
        this.nextIntegValue = "";
    }

    public ModuleVersionHolder(String key, String originalValue) {
        this(key, originalValue, false);
    }

    public String getKey() {
        return key;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public boolean isReleaseProp() {
        return releaseProp;
    }

    public void setReleaseProp(boolean releaseProp) {
        this.releaseProp = releaseProp;
    }

    public String getReleaseValue() {
        return releaseValue;
    }

    public void setReleaseValue(String releaseValue) {
        this.releaseValue = releaseValue;
    }

    public String getNextIntegValue() {
        if (releaseProp) {
            return originalValue;
        }
        return nextIntegValue;
    }

    public void setNextIntegValue(String nextIntegValue) {
        this.nextIntegValue = nextIntegValue;
    }

    public static List<ModuleVersionHolder> buildFromConf(Map<String, String> conf) {
        List<ModuleVersionHolder> result = Lists.newArrayList();
        int size = getNumberOfModules(conf);
        for (int i = 0; i < size; i++) {
            String moduleKey = conf.get(ReleaseAndPromotionAction.MODULE_KEY + "." + i);
            String currentValue = conf.get(ReleaseAndPromotionAction.CURRENT_VALUE_KEY + "." + i);
            String integValue = conf.get(ReleaseAndPromotionAction.NEXT_INTEG_KEY + "." + i);
            String releaseValue = conf.get(ReleaseAndPromotionAction.RELEASE_VALUE_KEY + "." + i);
            String releaseProp = conf.get(ReleaseAndPromotionAction.RELEASE_PROP_KEY + "." + i);
            boolean isReleaseProp = Boolean.parseBoolean(releaseProp);
            ModuleVersionHolder holder = new ModuleVersionHolder(moduleKey, currentValue, isReleaseProp);
            holder.setReleaseValue(releaseValue);
            holder.setNextIntegValue(integValue);
            result.add(holder);
        }
        return result;
    }

    private static int getNumberOfModules(Map<String, String> conf) {
        return Maps.filterKeys(conf, new Predicate<String>() {
            public boolean apply(String input) {
                return StringUtils.startsWith(input, ReleaseAndPromotionAction.MODULE_KEY);
            }
        }).size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ModuleVersionHolder holder = (ModuleVersionHolder) o;

        return key.equals(holder.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
