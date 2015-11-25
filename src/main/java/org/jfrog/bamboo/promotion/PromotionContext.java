package org.jfrog.bamboo.promotion;

import org.jfrog.bamboo.util.ActionLog;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A promotion context used to share data between {@link org.jfrog.bamboo.promotion.PromotionThread} which is the thread running the promotion
 * and {@code org.jfrog.bamboo.release.action.ReleaseAndPromotionAction} which is the view displaying the promotion result.
 *
 * @author Lior Hasson
 */
public class PromotionContext {

    private ActionLog log = new ActionLog();
    private boolean done;
    private Integer buildNumber;
    private String buildKey;
    private ReentrantLock lock = new ReentrantLock();

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(Integer buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getBuildKey() {
        return buildKey;
    }

    public void setBuildKey(String buildKey) {
        this.buildKey = buildKey;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public List<String> getLog() {
        return log.getLogEntries();
    }

    public ActionLog getActionLog(){
        return log;
    }

    public void clearLog(){
        this.log.clearLog();
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
