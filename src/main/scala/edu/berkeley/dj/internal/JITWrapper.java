package edu.berkeley.dj.internal;

import edu.berkeley.dj.jit.JITInterface;

/**
 * Created by matthewfl
 */
public class JITWrapper {

    private JITWrapper() {}

    private static DistributedVariable<JITInterface> djit = new DistributedVariable<JITInterface>("DJ_jit");

    static public JITInterface get() {
        return djit.get();
    }

    static void setJIT(JITInterface jit) {
        djit.lock();
        try {
            // we want to assert that this is null, rather then the check and set that is usually used
            assert(djit.get() == null);
            djit.set(jit);
        } finally {
            djit.unlock();
        }
    }

    static void registerNewClient(int id) {
        get().newClient(id);
    }

}


