package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.JITCommands;
import edu.berkeley.dj.internal.ObjectNotLocal;

/**
 * Created by matthewfl
 */
public class MoveOnUseJIT extends SimpleJIT {

//    @RewriteMakeRPC(0)
//    @RewriteAsyncCall
//    private void performMoveTo(Object self, int target) {
//        try {
//            JITCommands.moveObject(self, target);
//        } catch(ObjectNotLocal e) {
//            // this will keep redirecting the request until it finds the object
//            // and actually creates a move
//            performMoveTo(self, target);
//        }
//    }

    // these methods are call when the remote read/write operation has actually succeeded, which means that this
    // machine must already own these objects
    // therefore we do not need to do anything with redirecting these requests
    @Override
    public void recordReceiveRemoteRead(Object self, int from_machine, int to_machine, int field_id) {
        try {
            JITCommands.moveObject(self, from_machine);
        } catch (ObjectNotLocal e) {}
//        performMoveTo(self, from_machine);
    }

    @Override
    public void recordReceiveRemoteWrite(Object self, int from_machine, int to_machine, int field_id) {
        try {
            JITCommands.moveObject(self, from_machine);
        } catch (ObjectNotLocal e) {}

//        performMoveTo(self, from_machine);
    }

}
