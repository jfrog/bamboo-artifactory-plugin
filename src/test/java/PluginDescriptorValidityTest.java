/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Performs test on the descriptor to assure it's validity
 *
 * @author Noam Y. Tenne
 */
@Test
public class PluginDescriptorValidityTest {

    /**
     * Tests that the summary template location tag (which is a little long) was not line-wrapped. Sometimes the
     * auto-formatting wraps the line and breaks the descriptor.
     */
    public void testSummaryTemplateLocationIsNotWrapped() throws IOException {
        InputStream pluginDescriptorStream = getClass().getResourceAsStream("atlassian-plugin.xml");
        String pluginDescriptorContent = IOUtils.toString(pluginDescriptorStream);
        Assert.assertTrue(pluginDescriptorContent.contains("<result name=\"input\" type=\"freemarker\">/" +
                "templates/plugins/result/viewArtifactoryReleaseManagement.ftl</result>"),
                "Could not find expected summary template declaration. What the line wrapped again?");
    }
}
