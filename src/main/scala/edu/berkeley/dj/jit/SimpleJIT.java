package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.InternalInterface;
import edu.berkeley.dj.internal.JITCommands;
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

    @Override
    public void newClient(int id) {
        InternalInterface.debug("see new client "+id);
    }

    @Override
    public void recordRemoteRead(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {
        // we will get notifications if this class ends up performing a remote read and write as well
        // don't want to deal with the
        if(this == self)
            return;
        InternalInterface.debug("got remote read notification");
    }

    @Override
    public void recordRemoteWrite(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {

    }

    @Override
    public void recordRemoteRPC(Object self, int from_machine, int to_machine, StackRepresentation stack) {

    }

    @Override
    public int placeThread(Object self, int from_machine, StackRepresentation stack) {
        return from_machine;
    }

    @Override
    public void scheduleQueuedWork(Object self, int from_machine, StackRepresentation stack) {
        // if this system doesn't call runQueueWork at some point then the job will be lost
        JITCommands.runQueuedWork(self, from_machine);
    }
}
