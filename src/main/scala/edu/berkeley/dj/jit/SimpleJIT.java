package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.InternalInterface;

/**
 * Created by matthewfl
 *
 * Simple jit for managing distribuited programs that will randomlly allocate items or using trivial method
 */
public class SimpleJIT implements JITInterface {

    public SimpleJIT() {
        InternalInterface.debug("simple jit constructed");
    }

    public void newClient(int id) {
        InternalInterface.debug("see new client "+id);
    }
}
