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

package org.jfrog.bamboo.util;

import com.atlassian.bamboo.build.logger.BuildLogger;
import org.apache.log4j.Logger;
import org.jfrog.build.api.util.Log;

/**
 * @author Noam Y. Tenne
 */
public class BuildInfoLog implements Log {

    private Logger log;
    private BuildLogger buildLogger;

    public BuildInfoLog(Logger log) {
        this.log = log;
    }

    public BuildInfoLog(Logger log, BuildLogger buildLogger) {
        this.log = log;
        this.buildLogger = buildLogger;
    }

    public void debug(String message) {
        log.debug(message);
    }

    public void info(String message) {
        if (this.buildLogger != null) {
            this.buildLogger.addBuildLogEntry(message);
        }
        log.info(message);
    }

    public void warn(String message) {
        log.warn(message);
    }

    public void error(String message) {
        if (this.buildLogger != null) {
            this.buildLogger.addErrorLogEntry(message);
        }
        log.error(message);
    }

    public void error(String message, Throwable e) {
        if (this.buildLogger != null) {
            this.buildLogger.addErrorLogEntry(message, e);
        }
        log.error(message, e);
    }
}
