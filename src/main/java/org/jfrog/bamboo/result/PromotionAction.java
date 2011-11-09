package org.jfrog.bamboo.result;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Tomer Cohen
 */
public class PromotionAction {
    private List<String> log = Lists.newArrayList();
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
        return log;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
