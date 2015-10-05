package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.InternalInterface;
import edu.berkeley.dj.internal.StackRepresentation;

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


    public void recordRemoteRead(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {
        InternalInterface.debug("got remote read notification");
    }

    public void recordRemoteWrite(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {

    }

    public void recordRemoteRPC(Object self, int from_machine, int to_machine, StackRepresentation stack) {

    }
}
