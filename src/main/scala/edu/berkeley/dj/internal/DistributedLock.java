package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
final public class DistributedLock {

    private String name;

    boolean hasLock = false;

    public DistributedLock(String name) {
        this.name = name;
    }

    public void lock() {
        while(InternalInterface.getInternalInterface().lock(name) == false) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {}
        }
        hasLock = true;
    }

    public void unlock() {
        if(!hasLock)
            throw new RuntimeException("did not have lock to unlock");
        InternalInterface.getInternalInterface().unlock(name);
        hasLock = false;
    }
}
