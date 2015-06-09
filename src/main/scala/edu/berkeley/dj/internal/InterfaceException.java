package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class InterfaceException extends Error /*Exception*/ {

    InterfaceException(String method) {
        super("edu.berkeley.dj internal interface exception with method "+method);
    }

    InterfaceException() {
        super("InternalInterface not inited");
    }

    InterfaceException(String method, Throwable e) {
        super("edu.berkeley.dj internal interface exception with method "+method, e);
    }

}
