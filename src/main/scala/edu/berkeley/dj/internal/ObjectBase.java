package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * This becomes super class of the new "base class" that will represent objects
 * it will have to override methods such as lock or wait to make them work in a distribuited fashion
 */
public class ObjectBase implements InterfaceBase {

    public int __dj_class_mode = 0;

    public ClassManager __dj_class_manager = null;

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
    public int __dj_getClassMode() {
        return __dj_class_mode;
    }

    public ClassManager __dj_getManager() {
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
        return this == obj;
    }

    public String toString() { return super.toString(); }

    // these are final on the java.lang.Object
    // and we can't change the java.lang.Object class
    // so rewrite all uses of these methods to call these instead
    public final void __dj_notify() {
        // will have to send the notification back to the master node?
        // if there is some local item waiting on this class instance
        // then maybe just notify locally
        super.notify();
    }

    public final void __dj_notifyAll() {
        super.notify();
    }

    public final void __dj_wait(long timeout) throws InterruptedException {
        super.wait(timeout);
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

    protected final void finalize () throws Throwable {
        // we can delete proxy classes
        // but then we don't want to call the finalize method

    }

    protected void __dj_client_finalize() throws Throwable {}
}
