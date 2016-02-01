package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.JITCommands;

/**
 * Created by matthewfl
 */
public class MoveOnUseJIT extends SimpleJIT {

    @Override
    public void recordReceiveRemoteRead(Object self, int from_machine, int to_machine, int field_id) {
        JITCommands.moveObject(self, from_machine);
    }

    @Override
    public void recordReceiveRemoteWrite(Object self, int from_machine, int to_machine, int field_id) {
        JITCommands.moveObject(self, from_machine);
    }

}
