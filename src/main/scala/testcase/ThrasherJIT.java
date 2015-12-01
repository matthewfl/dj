package testcase;

import edu.berkeley.dj.internal.InternalInterface;
import edu.berkeley.dj.internal.JITCommands;
import edu.berkeley.dj.internal.StackRepresentation;
import edu.berkeley.dj.jit.JITInterface;

/**
 * Created by matthewfl
 */
public class ThrasherJIT implements JITInterface {

    public ThrasherJIT() {
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

        // since this is getting called on the machine that doesn't own the object
        // there isn't a lot of information that can be gathered from

        if(this == self)
            return;
//        InternalInterface.debug("got remote read notification");
    }

    @Override
    public void recordRemoteWrite(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {
        if(this == self)
            return;
    }

    @Override
    public void recordRemoteRPC(Object self, int from_machine, int to_machine, StackRepresentation stack) {
        if(this == self)
            return;

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

    @Override
    public void recordReceiveRemoteRead(Object self, int from_machine, int to_machine, int field_id) {

    }

    @Override
    public void recordReceiveRemoteWrite(Object self, int from_machine, int to_machine, int field_id) {

    }
}
