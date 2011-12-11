package org.jfrog.bamboo.util;

import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskDefinitionImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jfrog.bamboo.task.ArtifactoryGradleTask;
import org.jfrog.bamboo.task.ArtifactoryMaven3Task;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Y. Tenne
 */
public class TaskDefinitionHelperTest {

    private static TaskDefinitionImpl FOO = new TaskDefinitionImpl(0, "foo", "", Maps.<String, String>newHashMap());
    private static TaskDefinitionImpl MAVEN = new TaskDefinitionImpl(0, ArtifactoryMaven3Task.TASK_NAME, "",
            Maps.<String, String>newHashMap());
    private static TaskDefinitionImpl GRADLE = new TaskDefinitionImpl(0, ArtifactoryGradleTask.TASK_NAME, "",
            Maps.<String, String>newHashMap());

    @Test
    public void testFindMavenOrGradleDefs() throws Exception {
        assertNull(TaskDefinitionHelper.findMavenOrGradleDefinition(null), "Unexpected task definition for null list.");
        assertNull(TaskDefinitionHelper.findMavenOrGradleDefinition(Lists.<TaskDefinition>newArrayList()),
                "Unexpected task definition for empty list.");
        assertNull(TaskDefinitionHelper.findMavenOrGradleDefinition(Lists.<TaskDefinition>newArrayList(FOO)),
                "Unexpected task definition for list containing unknown def.");
        assertEquals(TaskDefinitionHelper.findMavenOrGradleDefinition(Lists.<TaskDefinition>newArrayList(FOO, MAVEN)),
                MAVEN, "Expected the maven task definition.");
        assertEquals(TaskDefinitionHelper.findMavenOrGradleDefinition(Lists.<TaskDefinition>newArrayList(FOO, GRADLE)),
                GRADLE, "Expected the gradle task definition.");
        assertEquals(TaskDefinitionHelper.findMavenOrGradleDefinition(
                Lists.<TaskDefinition>newArrayList(FOO, GRADLE, MAVEN)), MAVEN, "Expected the maven task definition.");
    }

    @Test
    public void testFindGradleDefinition() throws Exception {
        assertNull(TaskDefinitionHelper.findGradleDefinition(null), "Unexpected task definition for null list.");
        assertNull(TaskDefinitionHelper.findGradleDefinition(Lists.<TaskDefinition>newArrayList()),
                "Unexpected task definition for empty list.");
        assertNull(TaskDefinitionHelper.findGradleDefinition(Lists.<TaskDefinition>newArrayList(FOO)),
                "Unexpected task definition for list containing unknown def.");
        assertEquals(TaskDefinitionHelper.findGradleDefinition(Lists.<TaskDefinition>newArrayList(FOO, GRADLE)), GRADLE,
                "Expected the gradle task definition.");
    }

    @Test
    public void testFindMavenDefinition() throws Exception {
        assertNull(TaskDefinitionHelper.findMavenDefinition(null), "Unexpected task definition for null list.");
        assertNull(TaskDefinitionHelper.findMavenDefinition(Lists.<TaskDefinition>newArrayList()),
                "Unexpected task definition for empty list.");
        assertNull(TaskDefinitionHelper.findMavenDefinition(Lists.<TaskDefinition>newArrayList(FOO)),
                "Unexpected task definition for list containing unknown def.");
        assertEquals(TaskDefinitionHelper.findMavenDefinition(Lists.<TaskDefinition>newArrayList(FOO, MAVEN)), MAVEN,
                "Expected the maven task definition.");
    }
}
