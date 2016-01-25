package edu.berkeley.dj.internal.arrayclazz;

import edu.berkeley.dj.internal.ObjectBase;

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
            try {
                // don't try and redirect the length request if no longer the master, simply throw the exception
                return raw_length();
            } catch (java.lang.NullPointerException e) {
                // caller needs to determine if this value is valid
                return 0;
            }
        } else {
            return super.__dj_readFieldID_I(id);
        }
    }

    @Override
    public void __dj_serialize_obj(edu.berkeley.dj.internal.SerializeManager man) {
        super.__dj_serialize_obj(man);
//        System.err.println("trying to serialize an array");
//        throw new NotImplementedException();
    }

    @Override
    public void __dj_deserialize_obj(edu.berkeley.dj.internal.SerializeManager man) {
        super.__dj_deserialize_obj(man);
//        System.err.println("trying to deserialize an array");
    }
}
