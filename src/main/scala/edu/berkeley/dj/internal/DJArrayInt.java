package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class DJArrayInt implements DJArray {

    private int[] val;

    private DJArrayInt(int l) {
        val = new int[l];
    }

    public int length() {
        return val.length;
    }

    static public int[] create(int len) {
        System.out.println("managed to call the create int array");

        return new int[len];
        //return new DJArrayInt(len);
    }

    public Object get(int i) {
        return Integer.valueOf(val[i]);
    }

    public void store(int i, Object v) {
        val[i] = (int)v;
    }

    public int get_I(int i) {
        return val[i];
    }

    public void store_I(int i, int v) {
        val[i] = v;
    }
}
