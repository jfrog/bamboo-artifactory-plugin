package org.jfrog.bamboo.release.scm.perforce.command;

import com.tek42.perforce.Depot;
import com.tek42.perforce.PerforceException;
import com.tek42.perforce.parse.AbstractPerforceTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shelve the default changelist
 *
 * @author Shay Yaakov
 * @see <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/shelve.html">http://www.perforce.com/perforce/doc.current/manuals/cmdref/shelve.html</a>
 */
public class Shelve extends AbstractPerforceTemplate {
    private static Logger debuggingLogger = Logger.getLogger(Shelve.class.getName());

    private final Depot depot;

    public Shelve(Depot depot) {
        super(depot);
        this.depot = depot;
    }

    public void shelve() throws PerforceException {
        debuggingLogger.log(Level.FINE, "Shelving default changelist");
        String p4Exec = depot.getExecutable();
        getPerforceResponse(new String[]{p4Exec, "-s", "shelve"}, true);
    }
}
