package org.jfrog.bamboo.util;

import org.testng.annotations.Test;

import java.util.Properties;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
public class PluginPropertiesTest {

    @Test
    public void testPropertyValues() throws Exception {
        assertEquals(PluginProperties.GRADLE_DEPENDENCY_FILENAME_KEY, "gradle.dependency.file.name");
        assertEquals(PluginProperties.IVY_DEPENDENCY_FILENAME_KEY, "ivy.dependency.file.name");
        assertEquals(PluginProperties.MAVEN3_DEPENDENCY_FILENAME_KEY, "maven3.dependency.file.name");

        assertTrue(Pattern.compile("org.jfrog.bamboo.bamboo-artifactory-plugin-(?:.+)(?:-SNAPSHOT)?")
                .matcher(PluginProperties.getPluginKey()).matches(),
                "Plugin key value doesn't match the expected pattern.");
        assertEquals(PluginProperties.getPluginDescriptorKey(), "org.jfrog.bamboo.bamboo-artifactory-plugin",
                "Plugin descriptor key value doesn't match the expected pattern.");
        assertTrue(Pattern.compile("build-info-extractor-gradle-(?:.+)(?:-SNAPSHOT)?-uber.jar")
                .matcher(PluginProperties.getPluginProperty(PluginProperties.GRADLE_DEPENDENCY_FILENAME_KEY)).
                        matches(), "Gradle dependency filename doesn't match the expected pattern.");
        assertTrue(Pattern.compile("build-info-extractor-maven3-(?:.+)(?:-SNAPSHOT)?-uber.jar")
                .matcher(PluginProperties.getPluginProperty(PluginProperties.MAVEN3_DEPENDENCY_FILENAME_KEY)).
                        matches(), "Maven 3 dependency filename doesn't match the expected pattern.");
        assertTrue(Pattern.compile("build-info-extractor-ivy-(?:.+)(?:-SNAPSHOT)?-uber.jar")
                .matcher(PluginProperties.getPluginProperty(PluginProperties.IVY_DEPENDENCY_FILENAME_KEY)).
                        matches(), "Ivy dependency filename doesn't match the expected pattern.");
    }

    @Test
    public void testPropCount() throws Exception {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/artifactory.plugin.properties"));
        assertEquals(properties.keySet().size(), 5, "Unexpected number of properties; " +
                "If you've added or removed any properties, please update this test.");
    }
}
