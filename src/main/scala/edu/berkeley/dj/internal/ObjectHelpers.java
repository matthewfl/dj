package edu.berkeley.dj.internal;

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

    public static void notify(Object o) {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager == null) {
                // this object is not distribuited so just use the standard methods
                o.notify();
            } else {

                throw new NotImplementedException();
            }
        } else {
            o.notify();
        }
    }

    public static void notifyAll(Object o) {
        if(o instanceof ObjectBase) {
            ObjectBase ob = (ObjectBase)o;
            if(ob.__dj_class_manager == null) {
                o.notifyAll();
            } else {
                throw new NotImplementedException();
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
            } else {
                throw new NotImplementedException();
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
                ob.wait(timeout, nanos);
            } else {
                throw new NotImplementedException();
            }
        } else {
            o.wait(timeout, nanos);
        }
    }

    public static void monitorenter(Object o) {

    }

    public static void moniterexit(Object o) {

    }

}
