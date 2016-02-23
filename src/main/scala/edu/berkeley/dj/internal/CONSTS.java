package edu.berkeley.dj.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by matthewfl
 */
public class CONSTS {

    private CONSTS(){}

    // indicate that we are intercepting the reads/writes,
    public static final int REMOTE_READS = 0x01;
    public static final int REMOTE_WRITES = 0x02;

    public static final int IS_NOT_MASTER = 0x04;
//    public static final int IS_MASTER = 0x08;
    public static final int IS_READY_FOR_LOCAL_READS = 0x80;
    public static final int OBJECT_INITED = 0x10;
    public static final int PERFORM_RPC_REDIRECTS = 0x20;
    public static final int IS_CACHED_COPY = 0x40;
    public static final int IS_IO_WRAPPER = 0x80;


    // attempt at getting more information about what is going on
    public static final int DESERIALIZED_HERE = 0x100;
    public static final int SERIALIZED_HERE = 0x200;

    public static final int CURRENTLY_DESERIALIZING = 0x400;
    public static final int SERIALIZED_OBJ_SENT = 0x800;
    // if this is an empty object, eg a proxy to a remote machine (not a cache or master)
    public static final int IS_PROXY_OBJ = 0x1000;


    public static final int CURRENTLY_SERIALIZING = 0x2000;
    public static final int WAS_LOCKED = 0x4000;

    public static String str(int i) {
        Field[] fs = CONSTS.class.getDeclaredFields();
        String ret = Integer.toHexString(i) + " ";
        for(Field f : fs) {
            if(f.getType() == int.class && Modifier.isStatic(f.getModifiers())) {
                try {
                    if ((f.getInt(null) & i) != 0) {
                        ret += f.getName() + " ";
                    }
                } catch(IllegalAccessException e) {
                }
            }
        }
        return ret;
    }

}
