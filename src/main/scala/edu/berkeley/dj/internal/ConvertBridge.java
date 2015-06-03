package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.sun.misc.Unsafe2;
import javassist.bytecode.CodeAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {"java/lang/Object"})
public class ConvertBridge {

    /*Object makeNative(Proxied p) {
        return p.__dj_getRawProxyPointer();
    }*/

    private static final ConvertBridge converter = new ConvertBridge();

    public static ConvertBridge getConverter() { return converter; }

    private final Unsafe2 unsafe = Unsafe2.getUnsafe();

    private ConvertBridge() {}

    private final String internalPrefix = "edu.berkeley.dj.internal.";

    private final ClassLoader cl = this.getClass().getClassLoader();

    public Object makeNative(Object o) {
        /*if(o instanceof Proxied) {
            return makeNative((Proxied)o);
        }*/

        //throw new NotImplementedException();

        if(o instanceof BridgeGetNative) {
            return ((BridgeGetNative)o).__dj_toNative();
        }

        if(o.getClass().getName().startsWith(internalPrefix)) {
            // then there is going to be some corresponding class that we should convert to

        }
        return o;
    }

    public Object makeDJ(Object o) {
        // we are going to end up recursivly calling this class
        // so we need to independently know when to construct the objects in the internal namespace or not
        //o.getClass().getName()


        String name = o.getClass().getName();
        String nname = InternalInterface.getInternalInterface().classRenamed(name);

        if(nname == null || nname.equals(name)) {
            return o;
        }

        Class<?> cls;
        try {
            cls = cl.loadClass(nname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            Method fnative = cls.getMethod("__dj_fromNative", new Class[]{Object.class});
            return fnative.invoke(null, o);
        }
        catch (NoSuchMethodException e) {}
        catch (IllegalAccessException e) {}
        catch (InvocationTargetException e) {}


        InternalInterface.getInternalInterface().simplePrint("Failed to make dj object");
        throw new RuntimeException("Failed to make dj object from cls: "+name);
    }


}
