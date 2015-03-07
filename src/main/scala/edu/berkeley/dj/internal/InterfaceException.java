package edu.berkeley.dj.internal;

/**
 * Created by matthew on 3/6/15.
 */
public class InterfaceException extends Exception {

    InterfaceException(String method) {
        this.method = method;
    }

    public final String method;

    public String toString() {
        return "edu.berkeley.dj internal interface exception with method "+method;
    }
}
