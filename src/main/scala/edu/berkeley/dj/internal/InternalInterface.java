package edu.berkeley.dj.internal;

//import edu.berkeley.dj.internal.Object;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by matthewfl
 */
public class InternalInterface {

    InternalInterface() {}

    static private InternalInterface ii = null;

    static void setInternalInterface(Object o) {
        if(!o.getClass().getName().equals("edu.berkeley.dj.rt.RunningInterface"))
            throw new RuntimeException("Invalid class for running interface");
        if(ii != null)
            throw new RuntimeException("Running interface already set");
        ii = new InternalInterfaceWrap(o);
    }

    public static InternalInterface getInternalInterface() {
        return ii == null ? new InternalInterface() : ii;
    }

    public String toString() { return "InternalInterface(Empty)"; }

    public String getUUID() throws InterfaceException { return null; }



}

class InternalInterfaceWrap extends  InternalInterface {
    // because we are not fully loading the classes from the runtime in
    // it appears that we can't directly call the methods etc,
    // so keep it as an object and use invoke instead
    private Object base;
    private Class cls;


    InternalInterfaceWrap(Object o) {
        base = o;
        cls = base.getClass();

        try {
            invoke("setCallIn", new Class[]{Object.class}, this);
        } catch(InterfaceException e) {
            throw new RuntimeException("setting call in failed");
        }
    }

    private Object invoke(String name, Class[] sig, Object... obj) throws InterfaceException {
        try {
            return cls.getMethod(name, sig).invoke(base, obj);
        }
        catch(NoSuchMethodException e) {
            throw new InterfaceException(name);
        }
        catch(IllegalAccessException e) {
            throw new InterfaceException(name);
        }
        catch(InvocationTargetException e) {
            throw new InterfaceException(name);
        }
    }


    public String getUUID() throws InterfaceException {
        return (String) invoke("getUUID", new Class[]{});
    }

    public String toString() {
        try {
            return (String) invoke("toString", new Class[]{});
        } catch(InterfaceException e) {
            return "interface exception";
        }
    }

    public Object callIn(int action, Object[] args) {
        switch(action) {
            case 0:
                return "this works";
        }
        return null;
    }
}