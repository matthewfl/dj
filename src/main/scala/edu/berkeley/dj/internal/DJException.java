package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class DJException extends RuntimeException {

    public DJException(String msg) { super(msg); }

    public DJException(String msg, Throwable e) { super(msg, e); }

}
