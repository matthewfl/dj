package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.java.lang.Object00;

/**
 * Created by matthewfl
 *
 * This becomes super class of the new "base class" that will represent objects
 * it will have to override methods such as lock or wait to make them work in a distribuited fashion
 */
@RewriteClassRef(
        oldName = "edu.berkeley.dj.internal.coreclazz.java.lang.Object2",
        newName = "edu.berkeley.dj.internal.coreclazz.java.lang.Object"
)
public class ObjectBase implements Object00 {

    public int __dj_class_mode = 0;

    public ClassManager __dj_class_manager = null;

    public ObjectBase() {
        __dj_class_mode |= CONSTS.OBJECT_INITED;
    }

    // these have to be public in case they are getting
    // used by the interface
    //
    // I guess in a pure java since interfaces can't have
    // code to implement the methods
    //
    // having code on traits is a other language feature
    // (eg scala) and is implemented by having small
    // wrapper functions that call out to static methods
    // on other classes
    // so, then should those other classes use accessors
    // to get at data, or should they directly accesses
    // the "Manager"
    final public int __dj_getClassMode() {
        return __dj_class_mode;
    }

    final public ClassManager __dj_getManager() {
        return __dj_class_manager;
    }


    // need to override any methods that would be on java.lang.Object

    public int hashCode() {
        // TODO: make the hashCode check if this is a
        // full copy
        return super.hashCode();
    }

    public boolean equals(Object obj) {
        // this _should_ be ok, as if we are comparing the proxy
        // objects then it will come out to be the same proxied object
        // on a given machine
        try {
            return this.equals((Object00)obj);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public boolean equals(Object00 obj) {
        return this == obj;
    }

    public String toString() {
        if((__dj_class_mode & CONSTS.REMOTE_READS) == 0)
            return super.toString();
        else {
            // remotely call the external class
            return "";
        }
    }

    // these are final on the java.lang.Object
    // and we can't change the java.lang.Object class
    // so rewrite all uses of these methods to call these instead
    public final void __dj_notify() {
        // will have to send the notification back to the master node?
        // if there is some local item waiting on this class instance
        // then maybe just notify locally
        if((__dj_class_mode & (CONSTS.IS_NOT_MASTER)) == 0) {
            //if((__dj_class_mode & CONSTS.MONITOR_LOCK) == 0)
            //    throw new IllegalMonitorStateException();
            //synchronized (this) {
                super.notify();
            //}
        } else {
            // need to perform a remote operation for sending the notify
            assert(false);
        }

    }

    public final void __dj_notifyAll() {
        if((__dj_class_mode & (CONSTS.IS_NOT_MASTER)) == 0) {
            //if ((__dj_class_mode & CONSTS.MONITOR_LOCK) == 0)
            //    throw new IllegalMonitorStateException();
            //synchronized (this) {
                super.notifyAll();
            //}
        } else {
            assert(false);
        }
    }

    public final void __dj_wait(long timeout) throws InterruptedException {
        if((__dj_class_mode & (CONSTS.IS_NOT_MASTER)) == 0) {
            //if ((__dj_class_mode & CONSTS.MONITOR_LOCK) == 0)
            //    throw new IllegalMonitorStateException();
            //synchronized (this) {
                super.wait(timeout);
            //}
        } else {
            assert(false);
        }
    }

    public final void __dj_wait(long timeout, int nanos) throws InterruptedException {
        // copied from java.lang.Object
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && timeout == 0)) {
            timeout++;
        }

        __dj_wait(timeout);
    }

    public final void __dj_wait() throws InterruptedException { __dj_wait(0); }

    protected /*final*/ void finalize () throws Throwable {
        // we can delete proxy classes
        // but then we don't want to call the finalize method
        // TODO: check that this is not a proxy instance of the class
        this.__dj_client_finalize();
    }

    protected void __dj_client_finalize() throws Throwable {}

    // TODO: going to have to rewrite the monitor enter instructions
    final public void __dj_monitorenter() {
        // TODO: better locking management
        while(true) {
            synchronized (this) {
                // I think that there is some unsafe method that can be used to access the actual monitor on this object
                if((__dj_class_mode & CONSTS.MONITOR_LOCK) == 0) {
                    __dj_class_mode |= CONSTS.MONITOR_LOCK;
                    return;
                }
            }
        }
    }

    final public void __dj_monitorexit() {
        synchronized (this) {
            assert((__dj_class_mode & CONSTS.MONITOR_LOCK) != 0);
            __dj_class_mode &= ~CONSTS.MONITOR_LOCK;
        }
    }

    public void __dj_seralize_obj(SeralizeManager man) {

    }

    public void __dj_deseralize_obj(SeralizeManager man) {

    }
}
