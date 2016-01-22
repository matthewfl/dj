package edu.berkeley.dj.internal;

import edu.berkeley.dj.jit.JITInterface;

import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by matthewfl
 */
@RewriteClassRef(
        // only want to rewrite a limited number of classes
        oldName = "java.lang.Runnable",
        newName = "edu.berkeley.dj.internal.coreclazz.java.lang.Runnable"
)
public class JITWrapper {

    private JITWrapper() {
    }

    private static DistributedVariable<JITInterface> djit = new DistributedVariable<>("DJ_jit");

    // a local cache of the jit
    // since we can't remove a node atm or change the jit after the program is started
    // this is safe
    static private JITInterface cache = null;

    static public JITInterface get() {
        if (cache != null)
            return cache;
        return cache = djit.get();
    }

    // call before the main class is even loaded
    static void setJIT(JITInterface jit) {
        djit.lock();
        try {
            // we want to assert that this is null, rather then the check and set that is usually used
            assert (djit.get() == null);
            djit.set(jit);
        } finally {
            djit.unlock();
        }
    }

    // only called on the main node
    static void registerNewClient(int id) {
        get().newClient(id);
    }

    public enum OperationType {
        RemoteRead,
        RemoteWrite,
        RemoteRPC,
        ReceiveRemoteRead,
        ReceiveRemoteWrite,
        ProxyObjectCreated,
    }

    public static class RecordedOperation {

        public final OperationType type;
        public final WeakReference<Object> self;
        public final StackTraceElement[] stack;
        public final Object info;
        public final int target_machine;

        RecordedOperation(OperationType type, Object self, Throwable stack, Object info, int target_machine) {
            this.type = type;
            this.self = new WeakReference<>(self);
            if(stack != null) {
                this.stack = stack.getStackTrace();
            } else {
                this.stack = null;
            }
            this.info = info;
            this.target_machine = target_machine;
        }
    }

    private static ArrayBlockingQueue<RecordedOperation> operations = new ArrayBlockingQueue<RecordedOperation>(1000);

    static void recordRemoteRead(Object self, int fid, int target_machine) {
        RecordedOperation r = new RecordedOperation(OperationType.RemoteRead, self, new Throwable(), fid, target_machine);
        synchronized (operations) {
            if(operations.offer(r))
                operations.notify();
        }
    }

    static void recordRemoteWrite(Object self, int fid, int target_machine) {
        RecordedOperation r = new RecordedOperation(OperationType.RemoteWrite, self, new Throwable(), fid, target_machine);
        synchronized (operations) {
            if(operations.offer(r)) {
                operations.notify();
            }
        }
    }

    static void recordReceiveRemoteRead(Object self, int fid, int source_machine) {
        RecordedOperation r = new RecordedOperation(OperationType.ReceiveRemoteRead, self, null, fid, source_machine);
        synchronized (operations) {
            if(operations.offer(r)) {
                operations.notify();
            }
        }
    }

    static void recordReceiveRemoteWrite(Object self, int fid, int source_machine) {
        RecordedOperation r = new RecordedOperation(OperationType.ReceiveRemoteWrite, self, null, fid, source_machine);
        synchronized (operations) {
            if(operations.offer(r)) {
                operations.notify();
            }
        }
    }


    static void recordRemoteRPC(Object self, String method, int target_machine) {
        RecordedOperation r = new RecordedOperation(OperationType.RemoteRPC, self, new Throwable(), method, target_machine);
        synchronized (operations) {
            if(operations.offer(r)) {
                operations.notify();
            }
        }
    }

    static void recordProxyObjectCreated(Object self) {
        // TODO: determine when this is a proxy object and not a deserialization I guess
        // will allow the JIT to start moving over other commonly accessed objects based off the type
        //RecordedOperation r = new RecordedOperation()
    }

    // call from the machine
    static int placeThread(Object self) {
        StackRepresentation s = new StackRepresentation(new Throwable().getStackTrace());
        return get().placeThread(self, InternalInterface.getInternalInterface().getSelfId(), s);
    }

    static void queueScheduledWork(Object self) {
        StackRepresentation s = new StackRepresentation(new Throwable().getStackTrace());
        get().scheduleQueuedWork(self, InternalInterface.getInternalInterface().getSelfId(), s);
    }

    // TODO:
    // notify that thread (self) is block on some object (on)
    // eg (on) is a thread that isn't scheduled but (self) is now waiting for it to run
    //
    // This is a notification that giving (on) time to run should unblock (self)
    static void notifyBlockedThread(Object self, Object on) {
    }

    static void managedOperationQueue () {
        int self_id = InternalInterface.getInternalInterface().getSelfId();
        while(true) {
            RecordedOperation r;
            synchronized (operations) {
                while(true) {
                    r = operations.poll();
                    if (r != null)
                        break;
                    try {
                        operations.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            Object self = r.self.get();

            StackRepresentation s = null;
            if(r.stack != null)
                s = new StackRepresentation(r.stack);
            if(self != null) {
                switch(r.type) {
                    case RemoteRead:
                        get().recordRemoteRead(self, self_id, r.target_machine, (int)r.info, s);
                        break;
                    case RemoteWrite:
                        get().recordRemoteWrite(self, self_id, r.target_machine, (int)r.info, s);
                        break;
                    case RemoteRPC:
                        get().recordRemoteRPC(self, self_id, r.target_machine, s);
                        break;
                    case ReceiveRemoteRead:
                        get().recordReceiveRemoteRead(self, r.target_machine, self_id, (int)r.info);
                        break;
                    case ReceiveRemoteWrite:
                        get().recordReceiveRemoteWrite(self, r.target_machine, self_id, (int)r.info);
                        break;
                    default:
                        System.err.println("Got unknown type");
                        throw new RuntimeException();
                }
            }
            //InternalInterface.debug("got operation");
        }
    }

    static {
        // TODO: make this a daemon
        ThreadHelpers.runAsync(new Runnable() {
            @Override
            public void run() {
                JITWrapper.managedOperationQueue();
            }
        });
    }

}


