package edu.berkeley.dj.internal;

import sun.misc.Unsafe;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by matthewfl
 */
public class ProxyHelper {

    private ProxyHelper() {
    }

    private static Unsafe unsafe = InternalInterface.getInternalInterface().getUnsafe();

    final static String coreClassPrefix = "edu.berkeley.dj.internal.coreclazz.";

    public static Object invokeProxy(Object self,
                                     String tocls_,
                                     Class<?> fromcls,
                                     Class<?>[] argumentsTypes,
                                     String methodName,
                                     Object[] arguments) {
        try {
            Object inst = null;
            Class<?> tocls = Class.forName(tocls_);
            Method mth = tocls.getMethod(methodName, argumentsTypes);
            if (self != null) {
                assert (fromcls.getName().startsWith(coreClassPrefix));
                mth.setAccessible(true);
                inst = unsafe.allocateInstance(tocls);

                updateNativeObject(fromcls, tocls, inst, self);
            }

            for(int i = 0; i < arguments.length; i++) {
                arguments[i] = makeNative(arguments[i]);
            }

            Object res = mth.invoke(inst, arguments);

            if(self != null) {
                updateDjObject(tocls, fromcls, self, inst);
            }

            return makeDJ(res);

        } catch (ClassNotFoundException|
                NoSuchMethodException|
                InstantiationException|
                IllegalAccessException|
                InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    static Object makeDJ(Object o) {
        Class<?> ocls = o.getClass();
        String name = ocls.getName();
        String nname = InternalInterface.getInternalInterface().classRenamed(name);
        if (nname == null || nname.equals(name)) {
            return o;
        }
        // we have to convert this to a dj object
        try {
            Class<?> cls = Class.forName(nname);
            Object ret = unsafe.allocateInstance(cls);

            updateDjObject(ocls, cls, ret, o);

            return ret;

        } catch (ClassNotFoundException|
                InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static Object makeNative(Object o) {
        Class<?> ocls = o.getClass();
        String oname = ocls.getName();
        try {
            if (oname.startsWith(coreClassPrefix)) {
                // we have to convert this class to the original one
                Class<?> ncls = Class.forName(oname.substring(coreClassPrefix.length()));
                Object ninst = unsafe.allocateInstance(ncls);

                updateNativeObject(ocls, ncls, ninst, o);

                return ninst;
            } else {
                return o;
            }
        } catch (ClassNotFoundException|
                InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static void updateDjObject(Class<?> from, Class<?> to, Object self, Object inst) {
        // to should be the dj class
        // self should be the dj class
        try {
            Class<?> tocurcls = to;
            Class<?> fromcurcls = from;
            while(fromcurcls != null) {
                for(Field f : fromcurcls.getDeclaredFields()) {
                    if((f.getModifiers() & Modifier.STATIC) != 0)
                        continue;
                    f.setAccessible(true);
                    Method rmethod = tocurcls.getDeclaredMethod("__dj_read_field_"+f.getName(), new Class[] {tocurcls});
                    rmethod.setAccessible(true);

                    Class<?> ftype = f.getType();
                    Class<?> djtype;
                    if(!ftype.isPrimitive()) {
                        // need to determine what the equiv dj type will be
                        throw new NotImplementedException();
                    } else {
                        djtype = ftype;
                    }
                    Method wmethod = tocurcls.getDeclaredMethod("__dj_write_field_"+f.getName(), new Class[]{tocurcls, djtype});
                    wmethod.setAccessible(true);
                    Object curval = rmethod.invoke(null, self);
                    if(ftype == boolean.class) {
                        boolean v = f.getBoolean(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else if(ftype == byte.class) {
                        byte v = f.getByte(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else if(ftype == short.class) {
                        short v = f.getShort(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else if(ftype == char.class) {
                        char v = f.getChar(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else if(ftype == int.class) {
                        int v = f.getInt(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else if(ftype == long.class) {
                        long v = f.getLong(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else if(ftype == float.class) {
                        float v = f.getFloat(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else if(ftype == double.class) {
                        double v = f.getDouble(inst);
                        if(!curval.equals(v)) {
                            wmethod.invoke(null, self, v);
                        }
                    } else {
                        throw new NotImplementedException();
                    }


                }
            }
        } catch (NoSuchMethodException|
                IllegalAccessException|
                InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static void updateNativeObject(Class<?> from, Class<?> to, Object inst, Object self) {
        // copy the fields from `self` to `inst`
        try {
            Class<?> tocurcls = to;
            Class<?> fromcurcls = from;
            while (tocurcls != null) {
                for (Field f : tocurcls.getDeclaredFields()) {
                    if((f.getModifiers() & Modifier.STATIC) != 0)
                        continue;
                    f.setAccessible(true);
                    Class<?> ftype = f.getType();
                    Method rmethod = fromcurcls.getDeclaredMethod("__dj_read_field_" + f.getName(), new Class[]{fromcurcls });
                    rmethod.setAccessible(true);
                    if (ftype == boolean.class) {
                        f.setBoolean(inst, (boolean) rmethod.invoke(null, self));
                    } else if (ftype == byte.class) {
                        f.setByte(inst, (byte) rmethod.invoke(null, self));
                    } else if (ftype == short.class) {
                        f.setShort(inst, (short) rmethod.invoke(null, self));
                    } else if (ftype == char.class) {
                        f.setChar(inst, (char) rmethod.invoke(null, self));
                    } else if (ftype == int.class) {
                        f.setInt(inst, (int) rmethod.invoke(null, self));
                    } else if (ftype == long.class) {
                        f.setLong(inst, (long) rmethod.invoke(null, self));
                    } else if (ftype == float.class) {
                        f.setFloat(inst, (float) rmethod.invoke(null, self));
                    } else if (ftype == double.class) {
                        f.setDouble(inst, (double) rmethod.invoke(null, self));
                    } else {
                        // This is some object type
                        // so we may need to rewrite what the object is pointing at as its type might not agree
                        throw new NotImplementedException();
                    }
                }
                tocurcls = tocurcls.getSuperclass();
                fromcurcls = fromcurcls.getSuperclass();
            }
        } catch (NoSuchMethodException|
                IllegalAccessException|
                InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
