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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens a file in the workspace for edit.
 *
 * @author Yossi Shaul
 * @see <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/edit.html#1040665">http://www.perforce.com/perforce/doc.current/manuals/cmdref/edit.html#1040665</a>
 */
public class Edit extends AbstractPerforceTemplate {
    private static Logger debuggingLogger = Logger.getLogger(Edit.class.getName());

    /**
     * The file to open for edit
     */
    private final File file;

    private final Depot depot;

    public Edit(Depot depot, File file) {
        super(depot);
        this.depot = depot;
        this.file = file;
    }

    /**
     * Open file for edit
     */
    public void editFile() throws PerforceException {
        debuggingLogger.log(Level.FINE, "Opening file for edit: " + file.getAbsolutePath());
        String p4Exec = depot.getExecutable();
        getPerforceResponse(new String[]{p4Exec, "-s", "edit", file.getAbsolutePath()});
    }
}
