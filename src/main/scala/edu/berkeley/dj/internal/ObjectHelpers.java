package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * This represents method calls that we have rewritten
 * It is designed to work with non distribuited class as well
 * which means that we can just change the references in the source code without
 * must effort
 */
public class ObjectHelpers {

    private ObjectHelpers() {}

    public static void notify(Object o) {
        if(o instanceof ObjectBase) {
            ((ObjectBase)o).__dj_notify();
        } else {
            o.notify();
        }
    }

    public static void notifyAll(Object o) {
        if(o instanceof ObjectBase) {
            ((ObjectBase)o).__dj_notifyAll();
        } else {
            o.notifyAll();
        }
    }

    public static void wait(Object o) throws InterruptedException {
        if(o instanceof ObjectBase) {
            ((ObjectBase)o).__dj_wait();
        } else {
            o.wait();
        }
    }

    public static void wait(Object o, long timeout) throws InterruptedException {
        if(o instanceof ObjectBase) {
            ((ObjectBase)o).__dj_wait(timeout);
        } else {
            o.wait(timeout);
        }
    }

    public static void wait(Object o, long timeout, int nanos) throws InterruptedException {
        if(o instanceof ObjectBase) {
            ((ObjectBase)o).__dj_wait(timeout, nanos);
        } else {
            o.wait(timeout, nanos);
        }
    }

}
