package org.jfrog.bamboo.utils;

import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.*;

import static org.jfrog.bamboo.util.TaskUtils.addBuildInfoToContext;
import static org.jfrog.bamboo.util.TaskUtils.getAndDeleteAggregatedBuildInfo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author yahavi
 **/
public class TaskUtilsTest {
    private Map<String, String> customBuildData;
    private AutoCloseable mocks;

    @Mock
    CurrentBuildResult buildResult;
    @Mock
    BuildContext buildContext;
    @Mock
    TaskContext taskContext;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        customBuildData = new HashMap<>();
        when(taskContext.getBuildContext()).thenReturn(buildContext);
        when(buildContext.getParentBuildContext()).thenReturn(buildContext);
        when(buildContext.getBuildResult()).thenReturn(buildResult);
        when(buildResult.getCustomBuildData()).thenReturn(customBuildData);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void TestAddBuildInfoToContextOneDependency() throws IOException {
        List<Dependency> dependencyList = new ArrayList<>();
        dependencyList.add(new DependencyBuilder().id("a:b:c").build());
        testAddBuildInfoToContext(dependencyList);
    }

    @Test
    public void TestAddBuildInfoToContextManyDependencies() throws IOException {
        List<Dependency> dependencyList = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            dependencyList.add(new DependencyBuilder().id(String.valueOf(i)).build());
        }
        testAddBuildInfoToContext(dependencyList);
    }

    private void testAddBuildInfoToContext(List<Dependency> dependencyList) throws IOException {
        // Create a build info with the input dependencies
        Module module = new ModuleBuilder().id("test-module").dependencies(dependencyList).build();
        Build build = new BuildInfoBuilder("test-build").number("1").started(new Date().toString()).addModule(module).build();
        String expectedBuildInfo = BuildInfoExtractorUtils.buildInfoToJsonString(build);

        // Run addBuildInfoToContext
        addBuildInfoToContext(taskContext, expectedBuildInfo);

        // Run getAndDeleteAggregatedBuildInfo and make sure the actual Build Info equals to the expected
        assertEquals(expectedBuildInfo, getAndDeleteAggregatedBuildInfo(taskContext));

        // Make sure that all buildData variables deleted
        assertEquals(new HashMap<>(), customBuildData);
    }
}
