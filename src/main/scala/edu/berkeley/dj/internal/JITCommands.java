package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * Commands that the JIT can issue to the system to control what is going on
 */
@RewriteAllBut(nonModClasses = {})
public class JITCommands {

    static public int getObjectLocation(Object self) {
        try {
            ObjectBase b = (ObjectBase) self;
            if(b.__dj_class_manager != null) {
                int r = b.__dj_class_manager.owning_machine;
                if(r != -1)
                    return r;
            }
        } catch (ClassCastException e) {

        }
        return InternalInterface.getInternalInterface().getSelfId();
    }

    // move the ownership of some object to a target machine
    static public void moveObject(Object self, int target) {
        DistributedObjectHelper.moveObject((ObjectBase)self, target);
    }

    // use a field reference to determine where to move an object
    static public void moveObjectFieldRef(Object from, int field, int target) {
        DistributedObjectHelper.moveObjectFieldRef((ObjectBase)from, field, target);
    }

    // make a read only cache of some object on target machine
    static public void cacheObject(Object self, int target) {
        DistributedObjectHelper.makeCacheOfObject((ObjectBase)self, target);
    }

    static public void removeCacheObject(Object self, int target) {
        DistributedObjectHelper.removeCache((ObjectBase)self, target);
    }

    // send a command to the runtime to rewrite a method to have the RPC header
    // and reload the class with the new code
    static public void makeMethodRPC(String clsname, String methodSignature) {
        RPCHelpers.makeMethodRPC(clsname, methodSignature);
    }

    // when the system get some work submitted to something like a ForkJoinPool
    // it is the job of jit to schedule when the jobs should actually be run
    static public void runQueuedWork(Object self, int target) {
        // should have a queue of work on a given machine

        // if this is set to run on the local machine then it will just start a thread
        DistributedRunner.runOnRemote(target, (Runnable)self);
    }

    static public Object lookupObject(byte[] identifier) {
        return DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(identifier));
    }

}
