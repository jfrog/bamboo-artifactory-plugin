package org.jfrog.bamboo.release.scm.perforce.command;

import com.tek42.perforce.Depot;
import com.tek42.perforce.PerforceException;
import com.tek42.perforce.parse.AbstractPerforceTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Restore shelved files from a pending change into a workspace
 *
 * @author Shay Yaakov
 * @see <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/unshelve.html">http://www.perforce.com/perforce/doc.current/manuals/cmdref/unshelve.html</a>
 */
public class Unshelve extends AbstractPerforceTemplate {
    private static Logger debuggingLogger = Logger.getLogger(Unshelve.class.getName());

    private final Depot depot;

    public Unshelve(Depot depot) {
        super(depot);
        this.depot = depot;
    }

    public void unshelve(int changeListNum) throws PerforceException {
        debuggingLogger.log(Level.FINE, "Unshelving default changelist");
        String p4Exec = depot.getExecutable();
        getPerforceResponse(new String[]{p4Exec, "unshelve", "-f", "-s", Integer.toString(changeListNum)});
    }
}
