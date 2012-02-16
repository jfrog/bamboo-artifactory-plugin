/*
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.release.scm.perforce.command;

import com.tek42.perforce.Depot;
import com.tek42.perforce.PerforceException;
import com.tek42.perforce.parse.AbstractPerforceTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deletes (unlocked) perforce label.
 *
 * @author Yossi Shaul
 * @see <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/label.html">http://www.perforce.com/perforce/doc.current/manuals/cmdref/label.html</a>
 */
public class DeleteLabel extends AbstractPerforceTemplate {
    private static Logger debuggingLogger = Logger.getLogger(DeleteLabel.class.getName());

    private final String labelName;
    private Depot depot;

    public DeleteLabel(Depot depot, String labelName) {
        super(depot);
        this.labelName = labelName;
        this.depot = depot;
    }

    /**
     * Delete the remote label.
     */
    public void deleteLabel() throws PerforceException {
        debuggingLogger.log(Level.FINE, "Deleting label: " + labelName);
        String p4Exec = depot.getExecutable();
        getPerforceResponse(new String[]{p4Exec, "-s", "label", "-d", labelName});
    }
}
