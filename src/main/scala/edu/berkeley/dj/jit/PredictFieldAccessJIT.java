package edu.berkeley.dj.jit;

import edu.berkeley.dj.internal.JITCommands;
import edu.berkeley.dj.internal.StackRepresentation;
import edu.berkeley.dj.internal.coreclazz.RewriteLocalFieldOnly;

import java.util.HashMap;

/**
 * Created by matthewfl
 */
public class PredictFieldAccessJIT extends MoveOnUseJIT {

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
        if(next != null)
            JITCommands.moveObjectFieldRef(self, next, machine);
    }

    @Override
    public void recordRemoteWrite(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {
        determineNextField(self, field_id, to_machine);

    }

    @Override
    public void recordRemoteRead(Object self, int from_machine, int to_machine, int field_id, StackRepresentation stack) {
        determineNextField(self, field_id, to_machine);
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