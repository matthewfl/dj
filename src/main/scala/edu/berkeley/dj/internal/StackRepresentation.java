package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * Wrap the stack trace objects for easy use by the JIT
 *
 * TODO: this object is going to have to be able to be shared between multiple machines
 * need to represent the stack in some serializable form
 */
public class StackRepresentation {

    private final StackTraceElement[] stack;

    StackRepresentation (StackTraceElement[] stack) {
        this.stack = stack;
    }


}
