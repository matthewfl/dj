package edu.berkeley.dj.internal;


/**
 * Created by matthewfl
 */
public class DistributedVariable<T> {

    private final String name;

    private DistributedLock lock;

    // TODO: some annotation to rewrite the
    @RewriteAddAccessorMethods
    @RewriteUseAccessorMethods
    private static final class ObjectHolder extends ObjectBase {
        private Object o;
        void set(Object o) { this.o = o; }
        Object get() { return o; }
    }

    private DistributedObjectHelper.DistributedObjectId objectId = null;

    public DistributedVariable(String name) {
        this.name = name;
        this.lock = new DistributedLock("DJ_var_lock_"+name);
    }

    public DistributedVariable(String name, T init) {
        this(name);
        setIfNull(init);
    }

    ObjectHolder getHolder() {
        if(objectId == null) {
            lock();
            try {
                byte[] da = InternalInterface.getInternalInterface().getDistributed(name);
                if(da.length == 0) {
                    ObjectHolder h = new ObjectHolder();
                    objectId = DistributedObjectHelper.getDistributedId(h);
                    InternalInterface.getInternalInterface().setDistributed(name, objectId.toArr());
                    return h;
                } else {
                    objectId = new DistributedObjectHelper.DistributedObjectId();
                }

            } finally {
                unlock();
            }
        }
        return (ObjectHolder)DistributedObjectHelper.getObject(objectId);
    }

    public void set(T t) {
        getHolder().set(t);
    }

    public T get() {
        return (T)getHolder().get();
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
