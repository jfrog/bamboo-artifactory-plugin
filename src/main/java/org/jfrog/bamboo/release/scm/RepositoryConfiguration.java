package org.jfrog.bamboo.release.scm;

import com.atlassian.bamboo.vcs.configuration.PlanRepositoryDefinition;
import com.atlassian.bamboo.vcs.configuration.VcsLocationDefinitionImpl;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Created by diman on 22/11/2016.
 *
 * The way VCS data is retrieved is not consistent when using using different VCS types.
 * This class simplifies accessing VCS data.
 *
 */
public class RepositoryConfiguration {
    private HierarchicalConfiguration hierarchicalConfiguration;
    private Map<String, String> map;

    public RepositoryConfiguration(PlanRepositoryDefinition repository) {
        if (!repository.getVcsLocation().getConfiguration().isEmpty()) {
            map = repository.getVcsLocation().getConfiguration();
        }
        if (null != ((VcsLocationDefinitionImpl) repository.getVcsLocation()).getConfigurationRef()) {
            hierarchicalConfiguration = ((VcsLocationDefinitionImpl) repository.getVcsLocation()).getConfigurationRef().get();
        }
    }

    public String getString(String key, String defaultValue) {
        if (map != null && map.get(key) != null) {
            return map.get(key);
        }

        if (hierarchicalConfiguration != null) {
            return hierarchicalConfiguration.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public Long getLong(String key, Long defaultValue) {
        String value = this.getString(key);
        Long aLong = defaultValue;
        try {
            aLong = Long.parseLong(value);
        } catch (NumberFormatException ignored){
        }
        return aLong;
    }

    public Boolean getBoolean(String key, boolean defaultValue) {
        String value = this.getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return BooleanUtils.toBoolean(value);
    }
}
