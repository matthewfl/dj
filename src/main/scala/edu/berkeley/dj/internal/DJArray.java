package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public interface DJArray {

    int length();

    Object get(int i);

    void store(int i, Object v);
}
