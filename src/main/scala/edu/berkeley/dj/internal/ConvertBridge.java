package edu.berkeley.dj.internal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by matthewfl
 */
public class ConvertBridge {

    Object makeNative(Proxied p) {
        p.__dj_getRawProxyPointer();
    }

    Object makeNative(Object o) {
        if(o instanceof Proxied) {
            return makeNative((Proxied)o);
        }

        throw new NotImplementedException();
    }

    Object makeDJ(Object o) {
        o.getClass().getName()
    }

}
