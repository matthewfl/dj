package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.sun.misc.Unsafe00;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.*;

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

    private final Unsafe00 unsafe = Unsafe00.getUnsafe();

    private ConvertBridge() {}

    private final String internalPrefix = "edu.berkeley.dj.internal.coreclazz.";

    private final String djFieldPrefix = "__dj_";

    private final ClassLoader cl = this.getClass().getClassLoader();

    public Object makeNative(Object o) {
        /*if(o instanceof Proxied) {
            return makeNative((Proxied)o);
        }*/

        //throw new NotImplementedException();

        if(o instanceof BridgeGetNative) {
            // this class implements its own conversion to native instances
            return ((BridgeGetNative)o).__dj_toNative();
        }

        Class<?> cls = o.getClass();

        if(cls.isArray()) {
            // This could potentially break
            // need to know what type we are going to be casting to
            // otherwise there may be an issue with different subclasses being an array
            // so would need to find the common base class for all of the items
            //
            // even if we determine a common base type, then there may be something assigned latter that causes
            // it to break

            // TODO: are we going to make some wrapper for the array primitive types???
            Class<?> arrct = cls.getComponentType();

            if(arrct.isAssignableFrom(BridgeGetNative.class) || arrct.getName().startsWith(internalPrefix)) {
                Object[] arr = (Object[])o;
                if(arr.length == 0) {
                    // TODO: need to figure out the proper return type
                    // and then cast to it
                    try {
                        //BridgeGetNative ni = (BridgeGetNative)unsafe.allocateInstance(arrct);
                        Object ni = makeNative(unsafe.allocateInstance(arrct));
                        return Array.newInstance(ni.getClass(), 0);
                    } catch (InstantiationException e) { throw new RuntimeException(e); }
                }
                Object first = makeNative(arr[0]);
                // this is going to possibly break:
                Object[] ret = (Object[]) Array.newInstance(first.getClass(), arr.length);
                ret[0] = first;
                for(int i = 1; i < arr.length; i++) {
                    ret[i] = makeNative(arr[i]);
                }
                return ret;
            }
            return o;
        }

        if(cls.getName().startsWith(internalPrefix)) {
            // then there is going to be some corresponding class that we should convert to
            String nname = cls.getName().substring(internalPrefix.length());
            Class<?> ncls;
            try {
                ncls = cl.loadClass(nname);
            } catch (ClassNotFoundException e) { throw new RuntimeException(e); }

            // use unsafe to allocate the class so that we do not call the constructor on it
            Object ri;
            try {
                ri = unsafe.allocateInstance(ncls);
            } catch(InstantiationException e) { throw new RuntimeException(e); }

            Class<?> curncls = ncls;
            Class<?> curocls = cls;
            while(curncls != null) {
                for (Field f : curocls.getDeclaredFields()) {
                    if (f.getName().startsWith(djFieldPrefix))
                        continue;
                    if (Modifier.isStatic(f.getModifiers()))
                        continue;
                    try {
                        Field nf = curncls.getDeclaredField(f.getName());
                        long fa = unsafe.objectFieldOffset(f);
                        long ta = unsafe.objectFieldOffset(nf);

                        if (f.getType().isPrimitive()) {
                            // I miss scala about now
                            // some issue with comparing the native classes directly
                            String typ = f.getType().getName();
                            if ("boolean".equals(typ)) {
                                unsafe.putBoolean(ri, ta, unsafe.getBoolean(o, fa));
                            } else if ("byte".equals(typ)) {
                                unsafe.putByte(ri, ta, unsafe.getByte(o, fa));
                            } else if ("char".equals(typ)) {
                                unsafe.putChar(ri, ta, unsafe.getChar(o, fa));
                            } else if ("short".equals(typ)) {
                                unsafe.putShort(ri, ta, unsafe.getShort(o, fa));
                            } else if ("int".equals(typ)) {
                                unsafe.putInt(ri, ta, unsafe.getInt(o, fa));
                            } else if ("long".equals(typ)) {
                                unsafe.putLong(ri, ta, unsafe.getLong(o, fa));
                            } else if ("float".equals(typ)) {
                                unsafe.putFloat(ri, ta, unsafe.getFloat(o, fa));
                            } else if ("double".equals(typ)) {
                                unsafe.putDouble(ri, ta, unsafe.getDouble(o, fa));
                            } else {
                                throw new RuntimeException("unable to figure out type: "+typ);
                            }
                        } else {
                            // TODO: will have to deal with possible recursion
                            Object fro = unsafe.getObject(o, fa);
                            unsafe.putObject(ri, ta, makeNative(fro));
                        }
                    } catch (NoSuchFieldException e) {
                        System.err.println("wtf");
                        throw new RuntimeException(e);
                    }
                }
                curncls = curncls.getSuperclass();
                curocls = curocls.getSuperclass();
            }
            return ri;
        }

        // this class does not have a method to convert between itself and native
        // and not in our rewritten namespace, so it must be using its original name
        return o;
    }

    public Object makeDJ(Object o) {
        // we are going to end up recursivly calling this class
        // so we need to independently know when to construct the objects in the internal namespace or not
        //o.getClass().getName()

        Class<?> cls = o.getClass();

        if(cls.isArray()) {
            throw new NotImplementedException();
        }

        String name = cls.getName();
        String nname = InternalInterface.getInternalInterface().classRenamed(name);

        // check if we need to rewrite this class
        if(nname == null || nname.equals(name)) {
            return o;
        }

        Class<?> ncls;
        try {
            ncls = cl.loadClass(nname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            Method fnative = ncls.getMethod("__dj_fromNative", new Class[]{Object.class});
            return fnative.invoke(null, o);
        }
        catch (NoSuchMethodException e) {}
        catch (IllegalAccessException e) {}
        catch (InvocationTargetException e) {}

        Object ri;
        try {
            ri = unsafe.allocateInstance(ncls);
        } catch(InstantiationException e) { throw new RuntimeException(e); }

        throw new NotImplementedException();


        //InternalInterface.getInternalInterface().simplePrint("Failed to make dj object");
        //throw new RuntimeException("Failed to make dj object from cls: "+name);
    }


}
