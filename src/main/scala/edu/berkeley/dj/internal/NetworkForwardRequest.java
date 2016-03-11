package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class NetworkForwardRequest extends RuntimeException {

    public NetworkForwardRequest(int to) {
        if(to < 0)
            throw new RuntimeException("Attempt to forward to bad host");
        this.to = to;
    }

    final public int to;
}
