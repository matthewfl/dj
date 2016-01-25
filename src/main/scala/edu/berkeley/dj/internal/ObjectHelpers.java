package edu.berkeley.dj.internal;

import sun.misc.Unsafe;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

    private static Unsafe unsafe = InternalInterface.getInternalInterface().getUnsafe();

    public static void notify(Object o) {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager == null) {
                // this object is not distributed so just use the standard methods
                o.notify();
            } else {
                ob.__dj_class_manager.dj_notify();
            }
        } else {
            o.notify();
        }
    }

    public static void notifyAll(Object o) {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager == null) {
                ob.notifyAll();
            } else {
                ob.__dj_class_manager.dj_notifyAll();
            }
        } else {
            o.notifyAll();
        }
    }

    public static void wait(Object o) throws InterruptedException {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager == null) {
                ob.wait();
                // there will be a notify all when we switch from a non distributed mode to a distributed mode
                if(ob.__dj_class_manager != null) {
                    // TODO: may have to unlock the current monitor
                    // since we will end up spinning trying to get the lock
                    ob.__dj_class_manager.acquireMonitor();
                    ob.__dj_class_manager.dj_wait();
                    unsafe.monitorExit(ob);
                }
            } else {
                ob.__dj_class_manager.dj_wait();
                //throw new NotImplementedException();
            }
        } else {
            o.wait();
        }
    }

    public static void wait(Object o, long timeout) throws InterruptedException {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager == null) {
                ob.wait(timeout);
                if(ob.__dj_class_manager != null)
                    throw new NotImplementedException(); // we need to rewait on the distributed object
            } else {
                // TODO: need a timer implementation of waiting on distributed objects
                throw new NotImplementedException();
            }
        } else {
            o.wait(timeout);
        }
    }

    public static void wait(Object o, long timeout, int nanos) throws InterruptedException {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager == null) {
                ob.wait(timeout, nanos);
                if(ob.__dj_class_manager != null)
                    throw new NotImplementedException();
            } else {
                throw new NotImplementedException();
            }
        } else {
            o.wait(timeout, nanos);
        }
    }

    public static void monitorEnter(Object o) {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager != null) {
                ob.__dj_class_manager.acquireMonitor();
            } else {
                unsafe.monitorEnter(ob);
                if(ob.__dj_class_manager != null) {
                    unsafe.monitorExit(ob);
                    ob.__dj_class_manager.acquireMonitor();
                }
            }
        } else {
            // this is going to break if someone tries and synchronizes on something like Class<?> or String between machines
            unsafe.monitorEnter(o);
        }
    }

    public static void monitorExit(Object o) {
        // TODO: need to handle the case where something is holding the monitor while something is becoming distributed
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager != null) {
                ob.__dj_class_manager.releaseMonitor();
            } else {
                unsafe.monitorExit(ob);
            }
        } else {
            unsafe.monitorExit(o);
        }
    }

}
