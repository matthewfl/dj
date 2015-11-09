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
}
