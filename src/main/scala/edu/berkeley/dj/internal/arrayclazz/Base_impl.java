package edu.berkeley.dj.internal.arrayclazz;

import edu.berkeley.dj.internal.ObjectBase;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by matthewfl
 */
public abstract class Base_impl extends ObjectBase implements Base {

    //abstract int public length();

    //void store_B(Base arr, int indx, byte v) { throw new NotImplementedException(); }

    //void store_C(Base arr, int indx, byte v) { throw new NotImplementedException(); }


    //static public Base ANewArray(int )

    //static int length(Base_impl b) { return b.length(); }

    public int __dj_readFieldID_I(int id) {
        if(id == 9) {
            return length();
        } else {
            return super.__dj_readFieldID_I(id);
        }
    }

    public void __dj_serialize_obj(edu.berkeley.dj.internal.SerializeManager man) {
        System.err.println("trying to serialize an array");
        throw new NotImplementedException();
    }
}
