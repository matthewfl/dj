package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class NetworkForwardRequest extends RuntimeException {

    public NetworkForwardRequest(int to) {
        this.to = to;
    }

    final public int to;
}
