package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class DistributedVariable<T> {

    private final String name;

    private DistributedLock lock;

    public DistributedVariable(String name) {
        this.name = name;
        this.lock = new DistributedLock("DJ_lock_"+name);
    }

    public DistributedVariable(String name, T init) {
        this(name);
        setIfNull(init);
    }

    public void set(T t) {
        InternalInterface.getInternalInterface().setDistributed(name, t);
    }

    public T get() {
        return (T)InternalInterface.getInternalInterface().getDistributed(name);
    }

    public void setIfNull(T t) {
        lock();
        try {
            if (get() == null) {
                set(t);
            }
        } finally {
            unlock();
        }
    }

    public void lock() { lock.lock(); }

    public void unlock() { lock.unlock(); }

}
