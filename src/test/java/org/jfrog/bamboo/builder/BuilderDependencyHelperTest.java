package org.jfrog.bamboo.builder;

import org.jfrog.bamboo.context.Maven3BuildContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;


public class BuilderDependencyHelperTest {

    private BuilderDependencyHelper builderDependencyHelper;

    @Test
    public void shouldNotThrowExceptionForEmptyTempDir() throws IOException {
        builderDependencyHelper = new BuilderDependencyHelper("artifactoryBuilderTest");
        builderDependencyHelper.downloadDependenciesAndGetPath(new File(""), "TEST-ART-BAMB", new Maven3BuildContext(new HashMap<>()), "something");
    }

    @Test
    public void shouldNotThrowExceptionForNullTemp() throws IOException {
        builderDependencyHelper = new BuilderDependencyHelper("artifactoryBuilderTest");
        builderDependencyHelper.downloadDependenciesAndGetPath(null, "TEST-ART-BAMB", new Maven3BuildContext(new HashMap<>()), "something");
    }
}