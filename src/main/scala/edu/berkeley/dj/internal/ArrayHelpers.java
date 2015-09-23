package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.arrayclazz.Base_impl;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by matthewfl
 */
public class ArrayHelpers {

    private ArrayHelpers() {}

    static final String arrayPrefix = "edu.berkeley.dj.internal.arrayclazz.";

    static public Object makeDJArray(Object[] obj) {
        try {
            if(obj == null)
                return null;
            Class<?> acls = obj.getClass();
            if (!acls.isArray()) {
                throw new RuntimeException("Is not an array type" + acls.getName());
            }
            Class<?> cls = acls.getComponentType();
            if (cls.isArray()) {
                // We don not support multiple dimention arrays yet
                throw new NotImplementedException();
            }
            String newname = "edu.berkeley.dj.internal.arrayclazz." + cls.getName() + "_impl_1";
            Class<?> ncls = Class.forName(newname);
            Object ret = ncls.newInstance();
            Field irf = ncls.getDeclaredField("ir");
            irf.setAccessible(true);
            irf.set(ret, obj);
            return ret;
        } catch (ClassNotFoundException|
                IllegalAccessException|
                NoSuchFieldException|
                InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static public Object makeDJArray(boolean[] arr) {
        return makeDJArrayPrimitive("Boolean", arr);
    }

    static public Object makeDJArray(byte[] arr) {
        return makeDJArrayPrimitive("Byte", arr);
    }

    static public Object makeDJArray(char[] arr) {
        return makeDJArrayPrimitive("Character", arr);
    }

    static public Object makeDJArray(short[] arr) {
        return makeDJArrayPrimitive("Short", arr);
    }

    static public Object makeDJArray(int[] arr) {
        return makeDJArrayPrimitive("Integer", arr);
    }

    static public Object makeDJArray(long[] arr) {
        return makeDJArrayPrimitive("Long", arr);
    }

    static public Object makeDJArray(float[] arr) {
        return makeDJArrayPrimitive("Float", arr);
    }

    static public Object makeDJArray(double[] arr) {
        return makeDJArrayPrimitive("Double", arr);
    }

    static private Object makeDJArrayPrimitive(String type, Object arr) {
        try {
            if(arr == null)
                return null;
            String newname = "edu.berkeley.dj.internal.arrayclazz." + type + "_impl_1";
            Class<?> ncls = Class.forName(newname);
            Object ret = ncls.newInstance();
            Field irf = ncls.getField("ir");
            irf.setAccessible(true);
            irf.set(ret, arr);
            return ret;
        } catch (ClassNotFoundException|
                IllegalAccessException|
                NoSuchFieldException|
                InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    static public Object makeNativeArray(Base_impl arr) {
        if((arr.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0)
            throw new NotImplementedException();
        try {
            Field irf = arr.getClass().getDeclaredField("ir");
            irf.setAccessible(true);
            return irf.get(arr);
        } catch (NoSuchFieldException|
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static public Object makeNativeArray(Object o) {
        return makeNativeArray((Base_impl)o);
    }

    static public Object tryMakeNativeArray(Object o) {
        try {
            return makeNativeArray((Base_impl)o);
        } catch (ClassCastException e) {
            return o;
        }
    }

    // replace java.lang.reflect.Array:newInstance
    static public Object newInstance(Class<?> componentType, int length) {
        String name = componentType.getName();
        if(name.startsWith(arrayPrefix)) {
            // compute the dimension and increment it by one

            throw new NotImplementedException();
        } else {
            try {
                Class<?> arrcls = Class.forName("edu.berkeley.dj.internal.arrayclazz."+name+"_impl_1");
                Object arr = arrcls.getDeclaredMethod("newInstance_1", int.class).invoke(null, length);
                return arr;
            } catch (ClassNotFoundException|
                    NoSuchMethodException|
                    IllegalAccessException|
                    InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // replace java.lang.reflect:newInstance
    static public Object newInstance(Class<?> componentType, int ...lengths) {
        throw new NotImplementedException();
    }
}
