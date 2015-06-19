package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class DJError extends Error {

    public DJError(String v) { super(v); }

    public DJError(String v, Throwable e) { super(v, e); }

    public DJError() {}
}
