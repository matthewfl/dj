package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class SerializeException extends RuntimeException {

    public final Object obj;

    SerializeException(String msg, Object obj, Throwable e) {
        super(msg, e);
        this.obj = obj;
    }

    SerializeException(String msg, Object obj) {
        super(msg);
        this.obj = obj;
    }
}
