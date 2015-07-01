package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
final public class DistributedLock {

    private String name;

    int hasLock = 0;

    public DistributedLock(String name) {
        this.name = name;
    }

    public void lock() {
        //assert(hasLock == false);
        if(hasLock == 0) {
            while (InternalInterface.getInternalInterface().lock(name) == false) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {}
            }
            hasLock = 1;
        } else {
            hasLock++;
        }
    }

    public void unlock() {

        if(hasLock == 0)
            throw new RuntimeException("did not have lock to unlock");
        hasLock--;
        if(hasLock == 0)
            InternalInterface.getInternalInterface().unlock(name);
    }

    protected void finalize() {
        // ensure that we still do not own the lock
        assert(hasLock == 0);
    }
}
