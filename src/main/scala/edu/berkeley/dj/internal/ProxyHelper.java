package edu.berkeley.dj.internal;

import sun.misc.Unsafe;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by matthewfl
 *
 * Supports calling native methods on rewritten classes
 */
public class ProxyHelper {

    private ProxyHelper() {
    }

    private static Unsafe unsafe = InternalInterface.getInternalInterface().getUnsafe();

    final static String coreClassPrefix = "edu.berkeley.dj.internal.coreclazz.";

    public static Object invokeProxy(Object self,
                                     String tocls_,
                                     Class<?> fromcls,
                                     String[] argumentsTypes_,
                                     String methodName,
                                     Object[] arguments) throws Throwable {
        try {
            Object inst = null;
            Class<?> tocls = Class.forName(tocls_);
            Class<?> argumentsTypes[] = new Class<?>[argumentsTypes_.length];
            for(int i = 0; i < argumentsTypes_.length; i++) {
                argumentsTypes[i] = argumentLookup(argumentsTypes_[i]);
            }
            Method mth = tocls.getDeclaredMethod(methodName, argumentsTypes);
            mth.setAccessible(true);
            Map<Object, Object> convertToMap = new HashMap<>();
            if (self != null) {
                assert (fromcls.getName().startsWith(coreClassPrefix));
                inst = unsafe.allocateInstance(tocls);
                convertToMap.put(self, inst);
                updateNativeObject(fromcls, tocls, inst, self, convertToMap, 2);
            }

            for(int i = 0; i < arguments.length; i++) {
                arguments[i] = makeNative(arguments[i], convertToMap);
            }

            Object res = mth.invoke(inst, arguments);

            if(self != null) {
                updateDjObject(tocls, fromcls, self, inst, null, -1, convertToMap);
            }

            return makeDJ(res, convertToMap);

        } catch (ClassNotFoundException|
                NoSuchMethodException|
                InstantiationException|
                IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

    }

    static Class<?> argumentLookup(String n) throws ClassNotFoundException {
        if(n.equals("boolean")) {
            return boolean.class;
        } else if(n.equals("byte")) {
            return byte.class;
        } else if(n.equals("short")) {
            return short.class;
        } else if(n.equals("char")) {
            return char.class;
        } else if(n.equals("int")) {
            return int.class;
        } else if(n.equals("long")) {
            return long.class;
        } else if(n.equals("float")) {
            return float.class;
        } else if(n.equals("double")) {
            return double.class;
        } else {
            return Class.forName(n);
        }
    }

    static Object makeDJ(Object o, Map<Object, Object> convertToMap) {
        if(o == null)
            return null;
        Class<?> ocls = o.getClass();
        String name = ocls.getName();
        String nname = InternalInterface.getInternalInterface().classRenamed(name);
        if (nname == null || nname.equals(name)) {
            return o;
        }
        // we have to convert this to a dj object
        try {
            Class<?> cls = Class.forName(nname);
            Object ret = null;
            for(Map.Entry<Object, Object> en : convertToMap.entrySet()) {
                if(en.getValue() == o) {
                    ret = en.getKey();
                    break;
                }
            }
            if(ret == null)
                ret = unsafe.allocateInstance(cls);

            updateDjObject(ocls, cls, ret, o, null, -1, convertToMap);

            return ret;

        } catch (ClassNotFoundException|
                InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static Object makeNative(Object o, Map<Object, Object> convertToMap) {
        if(o == null)
            return null;
        Class<?> ocls = o.getClass();
        String oname = ocls.getName();
        try {
            if (oname.startsWith(coreClassPrefix)) {
                // we have to convert this class to the original one
                Class<?> ncls = Class.forName(oname.substring(coreClassPrefix.length()));
                // TODO: make this not stack overflow when getting the hash for objects
                Object ninst = null;//convertToMap.get(o);
                if(ninst == null) {
                    ninst = unsafe.allocateInstance(ncls);
                    //convertToMap.put(o, ninst);
                }

                updateNativeObject(ocls, ncls, ninst, o, convertToMap, 2);

                return ninst;
            } else {
                return o;
            }
        } catch (ClassNotFoundException|
                InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static void updateDjObject(Class<?> from, Class<?> to, Object self, Object inst, Map<Object, Object> convertMap, int depth, Map<Object, Object> convertToMap) {
        if(depth == 0)
            return;
        if(convertMap == null)
            convertMap = new HashMap<>();
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
                    Object curval = rmethod.invoke(null, self);

                    Class<?> ftype = f.getType();
                    String fname = ftype.getName();
                    Class<?> djtype = ftype;
                    if(!ftype.isPrimitive()) {
                        // need to determine what the equiv dj type will be
                        Object setval = f.get(inst);
                        // we do not need to copy this value over since they are identical
                        // expected to happen when they did not have to have their name changed
                        if(curval == setval)
                            continue;

                        if(setval == null) {
                            String nname = InternalInterface.getInternalInterface().classRenamed(fname);
                            if(fname != null && !nname.equals(fname)) {
                                djtype = Class.forName(nname);
                            }
                            Method wmethod = tocurcls.getDeclaredMethod("__dj_write_field_"+f.getName(), new Class[]{tocurcls, djtype});
                            wmethod.setAccessible(true);
                            wmethod.invoke(null, self, null);
                            continue;
                        }

                        Class<?> setclass = setval.getClass();
                        String setname = setclass.getName();


                        if(setclass.isArray()) {
                            Class<?> ctype = ftype.getComponentType();
                            if(ctype.isPrimitive()) {

                            }
                            // TODO: make the arrays get copied over and stuff
                        } else {
                            // check if we already have converted this type and then just use that
                            Object mapConvert = convertMap.get(setval);
                            if(mapConvert != null) {
                                String nname = InternalInterface.getInternalInterface().classRenamed(fname);
                                if(fname != null && !nname.equals(fname)) {
                                    djtype = Class.forName(nname);
                                }
                                Method wmethod = tocurcls.getDeclaredMethod("__dj_write_field_"+f.getName(), new Class[]{tocurcls, djtype});
                                wmethod.setAccessible(true);
                                wmethod.invoke(null, self, mapConvert);
                                continue;
                            }

                            String nname = InternalInterface.getInternalInterface().classRenamed(setname);
                            if(fname.equals(nname) || nname == null) {
                                // we are not changing the name of this class
                                // but the current value and this value are not equal so we are going to set it
                                // TODO: may have a different ftype?
                                Method wmethod = tocurcls.getDeclaredMethod("__dj_write_field_"+f.getName(), new Class[]{tocurcls, ftype});
                                wmethod.setAccessible(true);
                                wmethod.invoke(null, self, setval);
                                continue;
                            } else {
                                if(curval != null && nname.equals(curval.getClass().getName()) &&
                                        convertToMap != null && convertToMap.get(curval) == setval) {
                                    // then we can just update the original object
                                    convertMap.put(setval, curval);
                                    updateDjObject(setclass, curval.getClass(), curval, setval, convertMap, depth - 1, convertToMap);
                                    continue;
                                }
                                String nfname = InternalInterface.getInternalInterface().classRenamed(fname);
                                if(nfname != null && !nfname.equals(fname))
                                    djtype = Class.forName(nfname);

                                Class<?> mkcls = Class.forName(nname);
                                Object mkinst = unsafe.allocateInstance(mkcls);
                                convertMap.put(setval, mkinst);
                                updateDjObject(setclass, mkcls, mkinst, setval, convertMap, depth - 1, convertToMap);
                                Method wmethod = tocurcls.getDeclaredMethod("__dj_write_field_"+f.getName(), new Class[]{tocurcls, djtype});
                                wmethod.setAccessible(true);
                                wmethod.invoke(null, self, mkinst);
                                continue;
                            }
                        }
                        throw new NotImplementedException();
                    } else {
                        djtype = ftype;
                    }
                    Method wmethod = tocurcls.getDeclaredMethod("__dj_write_field_"+f.getName(), new Class[]{tocurcls, djtype});
                    wmethod.setAccessible(true);
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
                fromcurcls = fromcurcls.getSuperclass();
                tocurcls = tocurcls.getSuperclass();
            }
        } catch (NoSuchMethodException|
                IllegalAccessException|
                InvocationTargetException|
                ClassNotFoundException|
                InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static void updateNativeObject(Class<?> from, Class<?> to, Object inst, Object self, Map<Object, Object> convertedMap, int depth) {
        if(depth == 0)
            return;
        // copy the fields from `self` to `inst`
        if(convertedMap == null)
            convertedMap = new HashMap<>();
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
                        Object v = rmethod.invoke(null, self);
                        Object r = convertedMap.get(v);
                        if(v == null) {
                            f.set(inst, null);
                        } else if(r != null) {
                            f.set(inst, r);
                        } else {
                            Class<?> vcls = v.getClass();
                            if(vcls.isArray()) {
                                throw new NotImplementedException();

                            } else {
                                String vname = vcls.getName();
                                if (vname.startsWith(coreClassPrefix)) {
                                    Class<?> ncls = Class.forName(vname.substring(coreClassPrefix.length()));
                                    r = unsafe.allocateInstance(ncls);
                                    convertedMap.put(v, r);
                                    f.set(inst, r);
                                    updateNativeObject(vcls, ncls, r, v, convertedMap, depth - 1);
                                } else {
                                    f.set(inst, v);
                                }
                            }
                        }
                        //throw new NotImplementedException();
                    }
                }
                tocurcls = tocurcls.getSuperclass();
                fromcurcls = fromcurcls.getSuperclass();
            }
        } catch (NoSuchMethodException|
                IllegalAccessException|
                InvocationTargetException|
                InstantiationException|
                ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
