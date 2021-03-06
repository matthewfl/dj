package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * For constructors of the DJIO, we need to know which argument contains the
 * id of the target machine that the class should be constructed on
 *
 * Starting from index 1
 */
public @interface DJIOTargetMachineArgPosition {
    int value();
}
