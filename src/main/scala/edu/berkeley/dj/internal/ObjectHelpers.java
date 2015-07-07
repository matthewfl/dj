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
                /*synchronized (ob) {
                    if((ob.__dj_class_mode & CONSTS.MONITOR_LOCK) == 0)
                        throw new IllegalMonitorStateException();
                    ob.__dj_class_mode &= ~CONSTS.MONITOR_LOCK;
                    ob.wait();
                    //assert((ob.__dj_class_mode & CONSTS.MONITOR_LOCK) == 0);
                    //ob.__dj_class_mode |= CONSTS.MONITOR_LOCK;
                }
                monitorEnter(ob);
                */
                ob.wait();
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
                /*synchronized (ob) {
                    if((ob.__dj_class_mode & CONSTS.MONITOR_LOCK) == 0)
                        throw new IllegalMonitorStateException();
                    ob.__dj_class_mode &= ~CONSTS.MONITOR_LOCK;
                    ob.wait(timeout);
                    //assert((ob.__dj_class_mode & CONSTS.MONITOR_LOCK) == 0);
                    //ob.__dj_class_mode |= CONSTS.MONITOR_LOCK;
                }
                monitorEnter(ob);
                */
                ob.wait(timeout);
            } else {
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
                /*synchronized (ob) {
                    if((ob.__dj_class_mode & CONSTS.MONITOR_LOCK) == 0)
                        throw new IllegalMonitorStateException();
                    ob.__dj_class_mode &= ~CONSTS.MONITOR_LOCK;
                    ob.wait(timeout, nanos);
                    //assert((ob.__dj_class_mode & CONSTS.MONITOR_LOCK) == 0);
                    //ob.__dj_class_mode |= CONSTS.MONITOR_LOCK;
                }
                monitorEnter(ob);
                */
                ob.wait(timeout, nanos);
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
            }
        } else {
            unsafe.monitorEnter(o);
        }
    }

    public static void monitorExit(Object o) {
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
