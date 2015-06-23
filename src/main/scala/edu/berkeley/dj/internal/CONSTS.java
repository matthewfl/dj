package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class CONSTS {

    private CONSTS(){}

    public static final int REMOTE_READS = 0x01;
    public static final int REMOTE_WRITES = 0x02;
    public static final int IS_NOT_MASTER = 0x04;
    public static final int MONITOR_LOCK = 0x08;
    public static final int OBJECT_INITED = 0x10;
}
