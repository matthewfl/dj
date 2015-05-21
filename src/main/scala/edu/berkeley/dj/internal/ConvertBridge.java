package edu.berkeley.dj.internal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by matthewfl
 */
public class ConvertBridge {

    Object makeNative(Proxied p) {
        return p.__dj_getRawProxyPointer();
    }

    Object makeNative(Object o) {
        if(o instanceof Proxied) {
            return makeNative((Proxied)o);
        }

        throw new NotImplementedException();
    }

    Object makeDJ(Object o) {
        // we are going to end up recursivly calling this class
        // so we need to independently know when to construct the objects in the internal namespace or not
        //o.getClass().getName()
        return null;
    }

}
