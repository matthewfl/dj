package edu.berkeley.dj.ioInternal;

import edu.berkeley.dj.internal.DJIOException;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by matthewfl
 *
 * This class represents
 */
public class IOInternalInterface {

    IOInternalInterface() {}

    private static IOInternalInterface ii;

    public static IOInternalInterface getIOInternalInterface() {
        return ii;
    }

    static void setInternalInterface(Object o) {
        if(ii != null)
            throw new RuntimeException("IOInternalInterface already set");
        ii = new IOInternalInterfaceWrap(o);
    }

    public Object call(Object from, String method, String[] argsType, Object[] args) {
        throw new DJIOException("call method not overwritten");
    }

}

class IOInternalInterfaceWrap extends IOInternalInterface {

    private Object base;
    private Class cls;

    IOInternalInterfaceWrap(Object o) {
        base = o;
        cls = o.getClass();
        if(cls.getName() != "edu.berkeley.dj.rt.IOManager") {
            throw new RuntimeException("Wrong class for IOInternalInterface");
        }
    }

    private Object invoke(String name, Class[] sig, Object... obj) {
        try {
            return cls.getMethod(name, sig).invoke(base, obj);
        } catch(NoSuchMethodException|
                IllegalAccessException|
                InvocationTargetException e) {
            throw new DJIOException("Method '"+name+"' failed to invoke", e);
        }
    }

    @Override
    public Object call(Object from, String method, String[] argsType, Object[] args) {
        return invoke("callDJMethod", new Class[]{Object.class,String.class,String[].class,Object[].class}, from, method, argsType, args);
    }


}
