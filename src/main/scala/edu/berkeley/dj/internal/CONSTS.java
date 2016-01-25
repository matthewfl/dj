package edu.berkeley.dj.internal;

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

}
