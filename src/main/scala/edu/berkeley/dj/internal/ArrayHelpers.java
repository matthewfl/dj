package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.arrayclazz.Base;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Field;

/**
 * Created by matthewfl
 */
public class ArrayHelpers {

    private ArrayHelpers() {}

    /*static public int length_A(Object arr[]) {
        return arr.length;
    }

    static public int length_Z(boolean arr[]) {
        return arr.length;
    }

    static public int length_C(char arr[]) {
        return arr.length;
    }

    static public int length_B(byte arr[]) {
        return arr.length;
    }

    static public int length_S(short arr[]) {
        return arr.length;
    }

    static public int length_I(int arr[]) {
        return arr.length;
    }

    static public int length_J(long arr[]) {
        return arr.length;
    }

    static public int length_F(float arr[]) {
        return arr.length;
    }

    static public int length_D(double arr[]) {
        return arr.length;
    }

    static public int length(Object arr) {
        // have to determine the type of the array and then cast it to make the array length work...
        return 0;
    }


    static public void store_A(int ind, Object v, Object arr[]) {
        arr[ind] = v;
    }

    static public void store_Z(int ind, boolean v, boolean arr[]) {
        arr[ind] = v;
    }

    static public void store_C(int ind, char v, char arr[]) {
        arr[ind] = v;
    }

    static public void store_B(int ind, byte v, byte arr[]) {
        arr[ind] = v;
    }

    static public void store_S(int ind, short v, short arr[]) {
        arr[ind] = v;
    }

    static public void store_I(int ind, int v, int arr[]) {
        arr[ind] = v;
    }

    static public void store_J(int ind, long v, long arr[]) {
        arr[ind] = v;
    }

    static public void store_F(int ind, float v, float arr[]) {
        arr[ind] = v;
    }

    static public void store_D(int ind, double v, double arr[]) {
        arr[ind] = v;
    }

    static public Object get_A(int ind, Object arr[]) {
        return arr[ind];
    }

    static public boolean get_Z(int ind, boolean arr[]) {
        return arr[ind];
    }

    static public char get_C(int ind, char arr[]) {
        return arr[ind];
    }

    static public byte get_B(int ind, byte arr[]) {
        return arr[ind];
    }

    static public short get_S(int ind, short arr[]) {
        return arr[ind];
    }

    static public int get_I(int ind, int arr[]) {
        return arr[ind];
    }

    static public long get_J(int ind, long arr[]) {
        return arr[ind];
    }

    static public float get_F(int ind, float arr[]) {
        return arr[ind];
    }

    static public double get_D(int ind, double arr[]) {
        return arr[ind];
    }

*/

    /*static public int getLength(ObjectBase arr) {

    }*/

    static public Object makeDJArray(Object[] obj) {
        try {
            Class<?> acls = obj.getClass();
            if (!acls.isArray()) {
                throw new RuntimeException("Is not an array type" + acls.getName());
            }
            Class<?> cls = acls.getComponentType();
            if (cls.isArray()) {
                // We don not support multiple dimention arrays yet
                throw new NotImplementedException();
            }
            String newname = "edu.berkeley.dj.internal.arrayclazz." + cls.getName() + "_1";
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
        return makeDJArrayPrimitive("Char", arr);
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
            String newname = "edu.berkeley.dj.internal.arrayclazz." + type + "_1";
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

    static public Object makeNativeArray(Base arr) {
        if((arr.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0)
            throw new NotImplementedException();
        throw new NotImplementedException();
    }
}
