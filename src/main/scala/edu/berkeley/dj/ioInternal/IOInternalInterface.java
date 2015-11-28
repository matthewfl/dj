package edu.berkeley.dj.ioInternal;

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

}

class IOInternalInterfaceWrap extends IOInternalInterface {

    private Object base;
    private Class cls;

    IOInternalInterfaceWrap(Object o) {
        base = o;
        cls = o.getClass();
    }


}
