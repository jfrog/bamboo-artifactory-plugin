package org.jfrog.bamboo.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author Noam Y. Tenne
 */
public class TaskUtilsTest {

    @Test
    public void testGetEscapedEnvMap() throws Exception {
        assertNotNull(TaskUtils.getEscapedEnvMap(null), "Null map should be ignored.");
        assertTrue(TaskUtils.getEscapedEnvMap(null).isEmpty(), "Null map should be ignored.");

        Map<String, String> ordinaryString = Maps.newHashMap();
        ordinaryString.put("momo", "popo");
        assertEquals(TaskUtils.getEscapedEnvMap(ordinaryString).get("momo"), "popo",
                "Ordinary string key should be added without modification.");

        Map<String, String> underscoreContainingString = Maps.newHashMap();
        underscoreContainingString.put("momo_koko", "popo");
        assertFalse(TaskUtils.getEscapedEnvMap(underscoreContainingString).containsKey("momo_koko"),
                "Underscore containing key should be modified to a '.'");
        assertEquals(TaskUtils.getEscapedEnvMap(underscoreContainingString).get("momo.koko"), "popo",
                "Underscore containing key should be modified to a '.'");

        Map<String, String> bambooPrefixContainingString = Maps.newHashMap();
        bambooPrefixContainingString.put("bamboo.momo", "popo");
        assertFalse(TaskUtils.getEscapedEnvMap(bambooPrefixContainingString).containsKey("bamboo.momo"),
                "'bamboo.' prefix should be removed from keys.");
        assertEquals(TaskUtils.getEscapedEnvMap(bambooPrefixContainingString).get("momo"), "popo",
                "'bamboo.' prefix should be removed from keys.");

        Map<String, String> allCombinationsContainingString = Maps.newHashMap();
        allCombinationsContainingString.put("bamboo_momo", "popo");
        assertFalse(TaskUtils.getEscapedEnvMap(allCombinationsContainingString).containsKey("bamboo_momo"),
                "'bamboo_' prefix should be removed from keys.");
        assertEquals(TaskUtils.getEscapedEnvMap(allCombinationsContainingString).get("momo"), "popo",
                "'bamboo_' prefix should be removed from keys.");
    }

    @Test
    public void testAppendBuildInfoPropertiesArgument() throws Exception {
        TaskUtils.appendBuildInfoPropertiesArgument(null, null);

        List<String> arguments = Lists.newArrayList();
        TaskUtils.appendBuildInfoPropertiesArgument(arguments, null);
        assertTrue(arguments.isEmpty(), "Null properties file path should be ignored.");

        TaskUtils.appendBuildInfoPropertiesArgument(arguments, "c:\\mieow files\\moo.properties");
        assertTrue(arguments.contains("\"-D" + BuildInfoConfigProperties.PROP_PROPS_FILE +
                "=c:\\mieow files\\moo.properties\""), "Unable to find expected properties file path argument.");

        TaskUtils.appendBuildInfoPropertiesArgument(arguments, "/mieow/moo.properties");
        assertTrue(arguments.contains("-D" + BuildInfoConfigProperties.PROP_PROPS_FILE + "=/mieow/moo.properties"),
                "Unable to find expected properties file path argument.");
    }
}
