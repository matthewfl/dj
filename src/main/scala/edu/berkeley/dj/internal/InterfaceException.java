package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class InterfaceException extends Error /*Exception*/ {

    InterfaceException(String method) {
        this.method = method;
    }

    public final String method;

    public String toString() {
        return "edu.berkeley.dj internal interface exception with method "+method;
    }
}
