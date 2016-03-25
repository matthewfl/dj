package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.*;
import edu.berkeley.dj.internal.coreclazz.RewriteLocalFieldOnly;

import java.util.HashMap;

/**
 * Created by matthewfl
 */
public class PredictFieldAccessJIT extends SimpleJIT {

    @RewriteMakeRPC(0)
    @RewriteAsyncCall
    private void performMoveTo(Object self, int target) {
        try {
            JITCommands.moveObject(self, target);
        } catch (ObjectNotLocal e) {
            // do nothing if the location of the object is wrong

        }
    }

    @RewriteLocalFieldOnly
    private HashMap<Class<?>, HashMap<Integer, Integer>> fidmap = null;

    private HashMap<Class<?>, HashMap<Integer, Integer>> getFidMap() {
        if(fidmap == null) {
            fidmap = new HashMap<>();
        }
        return fidmap;
    }

    @RewriteLocalFieldOnly
    private HashMap<ObjectHashCodeWrap, Integer> lastField = null;

    private HashMap<ObjectHashCodeWrap, Integer> getLastField() {
        if(lastField == null || lastField.size() > 120) {
            lastField = new HashMap<>();
        }
        return lastField;
    }


    private void determineNextField (Object self, int fid, int machine) {
        Class<?> cls = self.getClass();
        HashMap<Integer, Integer> map = getFidMap().get(cls);
        if(map == null) {
            map = new HashMap<>();
            getFidMap().put(cls, map);
        }
        ObjectHashCodeWrap hcw = new ObjectHashCodeWrap(self);
        Integer lfid = getLastField().get(hcw);
        if(lfid != null) {
            // then there was some last field and we want to predict what the next one is going to be in the future
            if(fid != lfid) {
                map.put(lfid, fid);
            }
        }
        getLastField().put(hcw, fid);
        Integer next = map.get(fid);
        if(next != null) {
            //InternalInterface.debug("predicting next field access: "+fid+"->"+next);
            JITCommands.moveObjectFieldRef(self, next, machine);
        }
    }

    @Override
    public void recordRemoteWrite(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {
        determineNextField(self, field_id, to_machine);

    }

    @Override
    public void recordRemoteRead(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {
        determineNextField(self, field_id, to_machine);
    }

    @Override
    public void recordReceiveRemoteRead(Object self, int from_machine, int to_machine, int field_id) {
        try {
            JITCommands.moveObject(self, from_machine);
        } catch (ObjectNotLocal e) {}
    }

    @Override
    public void recordReceiveRemoteWrite(Object self, int from_machine, int to_machine, int field_id) {
        try {
            JITCommands.moveObject(self, from_machine);
        } catch (ObjectNotLocal e) {}
    }

}


class ObjectHashCodeWrap {

    final Object obj;

    ObjectHashCodeWrap(Object o) {
        obj = o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(obj);
    }

    @Override
    public boolean equals(Object o) {
        try {
            return ((ObjectHashCodeWrap)o).obj == obj;
        } catch (ClassCastException e) {
            return false;
        }
    }
}