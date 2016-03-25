package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class DJIOException extends DJException {

    public DJIOException(String msg) {
        super(msg);
    }

    public DJIOException(String msg, Throwable e) {
        super(msg, e);
    }

}
