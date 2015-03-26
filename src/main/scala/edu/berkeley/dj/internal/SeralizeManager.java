package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * This class represent that an object or a set of objects
 * are getting seralize or deseralized
 *
 *
 */
public class SeralizeManager {

    public int depth_left;

    void enter() { depth_left--; }

    void leave() { depth_left++; }

    boolean shouldStub() {
        return depth_left <= 0;
    }


}
