package edu.berkeley.dj.internal.arrayclazz;

/**
 * Created by matthewfl
 */
public abstract class Base {

    abstract int length();

    //void store_B(Base arr, int indx, byte v) { throw new NotImplementedException(); }

    //void store_C(Base arr, int indx, byte v) { throw new NotImplementedException(); }


    //static public Base ANewArray(int )

    static int length(Base b) { return b.length(); }
}
