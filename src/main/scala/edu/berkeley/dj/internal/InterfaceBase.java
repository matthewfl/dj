package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public interface InterfaceBase {

    default int __dj_getClassMode() { return 0; }

    default ClassManager __dj_getManager() { return null; }
}
