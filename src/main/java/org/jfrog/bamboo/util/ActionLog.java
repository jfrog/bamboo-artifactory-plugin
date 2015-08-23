package org.jfrog.bamboo.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * For Actions like promote and Push to Bintray where we have no BuildLog available
 * This will handle all logs to screen
 *
 * @author Aviad Shikloshi
 */
public class ActionLog {

    private static final String errorFormat = "<p style='color:red'>%s</p>";
    private static final String messageFormat = "<p>%s</p>";

    private List<String> logEntries;
    private Logger log;

    public ActionLog(){
        this.log = Logger.getLogger(ActionLog.class);
        this.logEntries = Lists.newArrayList();
    }

    public void setLogger(Logger log) {
        this.log = log;
    }
    public List<String> getLogEntries() {
        return logEntries;
    }


    public void clearLog(){
        this.logEntries.clear();
    }

    public void logError(String message, Exception e) {
        if (e != null) {
            message += " " + e.getMessage() + " <br>";
            logStackTrace(e);
        }
        log.error(message, e);
        logEntries.add(String.format(errorFormat, message));
    }

    public void logError(String message) {
        log.error(message);
        logEntries.add(String.format(errorFormat, message));
    }

    public void logMessage(String message) {
        log.info(message);
        logEntries.add(String.format(messageFormat, message));
    }

    private void logStackTrace(Exception e) {
        String stackTrace = ExceptionUtils.getStackTrace(e);
        stackTrace = String.format(errorFormat, stackTrace);
        logEntries.add(stackTrace);
    }

}
