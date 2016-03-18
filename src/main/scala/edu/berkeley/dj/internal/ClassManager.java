package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.coreclazz.java.lang.Object00DJ;
import edu.berkeley.dj.internal.coreclazz.java.lang.Thread00DJ;
import sun.misc.Unsafe;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by matthewfl
 *
 * Represents the backing information for a class that is shared between multiple machines
 *
 */
@RewriteAllBut(nonModClasses = {
        "java/util/UUID",
        "java/nio/ByteBuffer",
        "java/nio/Buffer",
        "sun/misc/Unsafe",
        "java/lang/ref/",
})
final public class ClassManager extends WeakReference<ObjectBase> {

    // TODO: this is inheriting from ObjectBase that means that we have two extra fields here
    // that we don't need, so remove that

    UUID distributedObjectId = null; // "OB_HEX_UUID"

    int owning_machine = -1; // signify self

    public int getOwner() { return owning_machine; }

    int notifications_to_send = 0;

    Thread00DJ monitor_thread = null;

    // TODO: if we move this thread elsewhere then we still need to have the count
    // so I guess will have to make the master machien keep the count of the number of times the monitor has been
    // aquired
    int monitor_lock_count = 0;

    // machines that have cached copies of this object
    // need to be updated on writes
    int[] cached_copies = null;

    // will need a week pointer to the object base
//    ObjectBase managedObject;

    // remote machine reference count
    int reference_count = 0;

    ClassManager(Object00DJ o) {
        super((ObjectBase)o, DistributedObjectHelper.gcRefQueue);
//        managedObject = (ObjectBase)o;
        distributedObjectId = UUID.randomUUID();
    }

    ClassManager(Object00DJ o, UUID id, int owner) {
        super((ObjectBase)o, DistributedObjectHelper.gcRefQueue);
//        managedObject = (ObjectBase)o;
        distributedObjectId = id;
        owning_machine = owner;
    }

    public boolean isLocal() {
        return owning_machine == -1;
    }

    // TODO: I suppose that the identifier for the field can be a short, since that would be a limitation
    // on the class already

    protected int getMode() { return  get().__dj_getClassMode(); }

    public void writeField_Z(int id, boolean v) {
        ByteBuffer b = requestRemote(id, 1);
        if(v)
            b.put((byte)1);
        else
            b.put((byte)0);
        requestWrite(b, 20, id);
    }

    public void writeField_C(int id, char v) {
        ByteBuffer b = requestRemote(id, 4);
        b.putChar(v);
        requestWrite(b, 21, id);
    }

    public void writeField_B(int id, byte v) {
        ByteBuffer b = requestRemote(id, 1);
        b.put(v);
        requestWrite(b, 22, id);
    }

    public void writeField_S(int id, short v) {
        ByteBuffer b = requestRemote(id, 2);
        b.putShort(v);
        requestWrite(b, 23, id);
    }

    public void writeField_I(int id, int v) {
        ByteBuffer b = requestRemote(id, 4);
        b.putInt(v);
        requestWrite(b, 24, id);
    }

    public void writeField_J(int id, long v) {
        ByteBuffer b = requestRemote(id, 8);
        b.putLong(v);
        requestWrite(b, 25, id);
    }

    public void writeField_F(int id, float v) {
        ByteBuffer b = requestRemote(id, 4);
        b.putFloat(v);
        requestWrite(b, 26, id);
    }

    public void writeField_D(int id, double v) {
        ByteBuffer b = requestRemote(id, 8);
        b.putDouble(v);
        requestWrite(b, 27, id);
    }

    public void writeField_A(int id, Object v) {
        //if(v instanceof ObjectBase) {
        //  ObjectBase ob = (ObjectBase) v;
            byte[] did = DistributedObjectHelper.getDistributedId(v).toArr();
            ByteBuffer b = requestRemote(id, did.length);
            b.put(did);
            requestWrite(b, 28, id);
//        } else {
//            // TODO: have some sort of proxy object that we can then pass
//            // to wrap the object that we can't move
//            throw new NotImplementedException();
//        }
    }

    public boolean readField_Z(int id) {
        return requestRead(id, 10).get() != 0;
    }

    public char readField_C(int id) {
        return requestRead(id, 11).getChar();
    }

    public byte readField_B(int id) {
        return requestRead(id, 12).get();
    }

    public short readField_S(int id) {
        return requestRead(id, 13).getShort();
    }

    public int readField_I(int id) {
        return requestRead(id, 14).getInt();
    }

    public long readField_J(int id) {
        return requestRead(id, 15).getLong();
    }

    public float readField_F(int id) {
        return requestRead(id, 16).getFloat();
    }

    public double readField_D(int id) {
        return requestRead(id, 17).getDouble();
    }

    public Object readField_A(int id) {
        ByteBuffer res = requestRead(id, 18);
        Object r = DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(res.array()));
        return r;
    }


    // these are getting called from the wrapped methods, so they should not fail when accessing the object
    private ByteBuffer requestRead(int fid, int op) {
        long startTime = InternalLogger.getTime();
        int owner = owning_machine;
//        int mode = managedObject.__dj_class_mode;
        //InternalInterface.debug("read request "+fid+" "+op+" "+owner+" "+distributedObjectId);
        ObjectBase managedObject = get();
        while(owner == -1) {
            // we are sending this request to ourselves, this is likely the result of synchronization with serialization
            ByteBuffer ret = DistributedObjectHelper.readFieldSwitch(managedObject, op, fid);
            int mode2 = managedObject.__dj_class_mode;
            if((mode2 & CONSTS.REMOTE_READS) == 0 || (mode2 & CONSTS.IS_READY_FOR_LOCAL_READS) != 0) {
                // this is to deal with a race between the deserialization
                // set the flags to allow not calling back into the class manager for this object
                if((mode2 & CONSTS.IS_READY_FOR_LOCAL_READS) != 0) {
//                    int om;
//                    int nm;
//                    do {
//                        om = managedObject.__dj_class_mode;
//                        nm = om & ~(CONSTS.REMOTE_READS | CONSTS.IS_READY_FOR_LOCAL_READS);
//                    } while(!unsafe.compareAndSwapInt(managedObject, DistributedObjectHelper.object_base_mode_field_offset, om, nm));
                    DistributedObjectHelper.updateMode(managedObject, 0, CONSTS.REMOTE_READS | CONSTS.IS_READY_FOR_LOCAL_READS);
                }

                ret.position(0);
                return ret;
            }
            owner = owning_machine;
//            assert(owner != -1);
//            if(owner == -1)
//                throw new AssertionError();
        }
        ByteBuffer bb = requestRemote(fid, 0);
        JITWrapper.recordRemoteRead(managedObject, fid, owner);
        ByteBuffer ret;
        try {
            ret = InternalInterface.getInternalInterface().readField(bb, op, owner);
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
        InternalLogger.addReadTime(InternalLogger.getTime() - startTime);
        return ret;
    }

    private ByteBuffer requestRemote(int fid, int exces) {
        ByteBuffer bb = ByteBuffer.allocate(20 + exces);
        bb.putLong(distributedObjectId.getMostSignificantBits());
        bb.putLong(distributedObjectId.getLeastSignificantBits());
        bb.putInt(fid);
        return bb;
    }

    private void requestWrite(ByteBuffer bb, int op, int fid) {
        long startTime = InternalLogger.getTime();
        ObjectBase managedObject = get();
        int owner = owning_machine;
        int mode = managedObject.__dj_class_mode;
        if(owner == -1) {
            // we are sending this to ourselves
            if(cached_copies != null) {
                // update all of the cached copies with the new value
                throw new NotImplementedException();
            }
            bb.position(20); // 2*long + int
            DistributedObjectHelper.writeFieldSwitch(managedObject, bb, op, fid);
            while(owner == -1) {
                int mode2 = managedObject.__dj_class_mode;
                if ((mode2 & CONSTS.REMOTE_WRITES) == 0) {
                    return;
                }
                owner = owning_machine;
            }
            bb.position(0);
//            if(owner == -1)
//                throw new AssertionError();
////            assert(owner != -1);
        }
        JITWrapper.recordRemoteWrite(managedObject, fid, owner);
        InternalInterface.getInternalInterface().writeField(bb, op, owner);
        InternalLogger.addWriteTime(InternalLogger.getTime() - startTime);
    }


    int[] waitingMachines;

    void addMachineToWaiting(int id) {
        synchronized (this) {
            if (waitingMachines == null) {
                waitingMachines = new int[]{id};
            } else {
                int[] n = new int[waitingMachines.length + 1];
                System.arraycopy(waitingMachines, 0, n, 0, waitingMachines.length);
                n[waitingMachines.length] = id;
                waitingMachines = n;
            }
        }
    }

    void removeMachineFromWaiting(int id) {
        synchronized (this) {
            int r = -2;
            // find the last instances of this machine in the waiting queue
            for(int i = waitingMachines.length - 1; i >= 0; i--) {
                if(waitingMachines[i] == id) {
                    r = i;
                    break;
                }
            }
            if(r == -2) return;
            int[] n = new int[waitingMachines.length - 1];
            System.arraycopy(waitingMachines, 0, n, 0, r);
            System.arraycopy(waitingMachines, r + 1, n, r, waitingMachines.length - 1);
            waitingMachines = n;
        }
    }

    /*private int getNextWaitingMachine() {
        synchronized (this) {
            if (waitingMachines == null) {
                return -1;
            }
            if (waitingMachines.length == 1) {
                int r = waitingMachines[0];
                waitingMachines = null;
                return r;
            }
            int r = waitingMachines[0];
            int[] n = new int[waitingMachines.length - 1];
            System.arraycopy(waitingMachines, 1, n, 0, waitingMachines.length - 1);
            waitingMachines = n;
            return r;
        }
    }*/

    public void dj_notify() {
        if(notifications_to_send != -1) {
            notifications_to_send++;
        }
        /*
        if((getMode() & CONSTS.IS_NOT_MASTER) == 0) {
            // is the master
            int n = getNextWaitingMachine();
            if (n == -1) {
                managedObject.notify();
            } else {
                // send the notification to another machine
                InternalInterface.getInternalInterface().sendNotifyOnObject(objectId().array(), n);
            }
        } else {
            // send a message to the master to create a notification
            InternalInterface.getInternalInterface().sendNotify(objectId().array(), owning_machine);
        }
        */
    }

    public void dj_notifyAll() {
        notifications_to_send = -1;
        /*
        if((getMode() & CONSTS.IS_NOT_MASTER) == 0) {
            // is master send notification to all instances
            synchronized (this) {
                if (waitingMachines == null) return;
                for (int i = 0; i < waitingMachines.length; i++) {
                    if (waitingMachines[i] == -1) {
                        managedObject.notify();
                    } else {
                        // send notification to another machine
                        InternalInterface.getInternalInterface().sendNotifyOnObject(objectId().array(), waitingMachines[i]);
                    }
                }
            }
        } else {
            InternalInterface.getInternalInterface().sendNotifyAll(objectId().array(), owning_machine);
        }*/

    }

    /*void sendNNotifications(int n) {
        synchronized (managedObject) {
            synchronized (this) {
                int i = 0;
                byte[] obj = objectId().array();
                while (i < waitingMachines.length && (i < n || n == -1)) {
                    // TODO:
                    if (waitingMachines[i] == -1) {
                        // send a notification to the local machine
                        managedObject.notify();
                    } else {
                        InternalInterface.getInternalInterface().sendNotifyOnObject(obj, waitingMachines[i]);
                    }
                    i++;
                }
                if (i == waitingMachines.length) {
                    waitingMachines = null;
                } else {
                    int na[] = new int[waitingMachines.length - i];
                    System.arraycopy(waitingMachines, i, na, 0, na.length);
                    waitingMachines = na;
                }
            }
        }
    }*/

    void sendANotification() {
        // send 1 notification
        ObjectBase managedObject = get();
        synchronized (managedObject) {
            synchronized (this) {
                if(waitingMachines != null && waitingMachines.length > 0) {
                    int i = waitingMachines[0];
                    if(i == -1) {
                        managedObject.notify();
                    } else {
                        InternalInterface.getInternalInterface().sendNotifyOnObject(
                                objectId().array(), i);
                        boolean cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, 0, 0x100 + i);
                        if(!cas)
                            throw new RuntimeException("cas");
//                        assert(cas);
                        monitor_thread = Thread00DJ.dummy_lock;
//                        assert(monitor_lock_count == 0);
//                        monitor_lock_count = 1;
                    }
                    if(waitingMachines.length > 1) {
                        int na[] = new int[waitingMachines.length - 1];
                        System.arraycopy(waitingMachines, 1, na, 0, na.length);
                        waitingMachines = na;
                    } else {
                        waitingMachines = null;
                    }
                }
            }
        }
    }

    void processNotifications() {
        // there is nothing to do
        if(notifications_to_send == 0)
            return;
        ObjectBase managedObject = get();
        synchronized (managedObject) {
            if(notifications_to_send == -1) {
                if(waitingMachines != null)
                    notifications_to_send = waitingMachines.length;
                else
                    notifications_to_send = 0;
            }
            // make sure that we are the master
            assert((getMode() & CONSTS.IS_NOT_MASTER) == 0);
            if(notifications_to_send > 0) {
                sendANotification();
                notifications_to_send--;
            }
        }
    }

    public void dj_wait() throws InterruptedException {
        checkHasLock();
        ObjectBase managedObject = get();
        if((getMode() & CONSTS.IS_NOT_MASTER) == 0) {
            // we are master
            //assert(monitor_lock_count != 0 && monitor_thread == Thread00.currentThread());
            synchronized (managedObject) {
                processNotifications();
                addMachineToWaiting(-1);
                int cnt = monitor_lock_count;
                assert(monitor_thread == Thread00DJ.currentThread());
                monitor_thread = null;
                boolean cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, cnt, 0);
                if(!cas)
                    throw new RuntimeException("cas");
//                assert(cas);
//                monitor_lock_count = 0;
                //assert(notifications_to_send == 0);
                managedObject.wait();
                cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, 0, cnt);
                if(!cas)
                    throw new RuntimeException("cas");
                assert(cas);
//                monitor_lock_count = cnt;
                monitor_thread = Thread00DJ.currentThread();
            }
        } else {
            synchronized (managedObject) {
                // it does not make since for us to wait on an object after sending a notification
                // so for now just make sure that we don't have any to send
                //assert(notifications_to_send == 0);
                InternalInterface.getInternalInterface().waitOnObject(objectId().array(),
                    owning_machine, notifications_to_send);
                notifications_to_send = 0;
                //processNotifications();
                assert(monitor_thread == Thread00DJ.currentThread());
                int cnt = monitor_lock_count;
                boolean cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, cnt, 0);
                if(!cas)
                    throw new RuntimeException("cas");
//                assert(cas);
                monitor_thread = null;
                managedObject.wait();
                // TODO: something else could attempt to get the lock here
                // I guess that would be a thread that first acquires the local lock, and then attempts to grab the global lock
                cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, 0, cnt);
                if(!cas)
                    throw new RuntimeException("cas");
//                assert(cas);
                monitor_thread = Thread00DJ.currentThread();
            }
        }
        //managedObject.wait();
    }

    public void dj_wait(long timeout) throws InterruptedException {
        // need to remove self from the queue somehow
        // I guess the queue could also have the timeout for the object contained with them, but idk how well that would work
        throw new NotImplementedException();
    }

    public void dj_wait(long timeout, long nanos) throws InterruptedException {
        throw new NotImplementedException();
    }

    private ByteBuffer objectId() {
        ByteBuffer b = ByteBuffer.allocate(16);
        b.putLong(distributedObjectId.getMostSignificantBits());
        b.putLong(distributedObjectId.getLeastSignificantBits());
        return b;
    }


    public void acquireMonitor() {
        //synchronized (this) {
        ObjectBase managedObject = get();
            if((managedObject.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
                // we have to communicate with the master
                // first get the lock on the local object
                while (true) {
                    synchronized (managedObject) {
                        if(monitor_lock_count != 0 && monitor_thread != Thread00DJ.currentThread()) {
                            continue;
                        }
//                        monitor_lock_count++;
                        int v = monitor_lock_count;
                        boolean cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, v, v+1);
                        if(!cas)
                            throw new RuntimeException("cas");
//                        assert(cas);
                        Thread00DJ ct = Thread00DJ.currentThread();
                        // then this thread already owns this lock
                        // so we don't need to relock it again
                        if(monitor_thread == ct)
                            return;
                        monitor_thread = ct;
                        break;
                    }
                }
                InternalInterface.getInternalInterface().acquireObjectMonitor(objectId(), owning_machine);
            } else {
                // we are the master
                while(true) {
                    synchronized (managedObject) {
                        if (monitor_lock_count != 0 && monitor_thread != Thread00DJ.currentThread()) {
                            continue;
                        }
//                        monitor_lock_count++;
                        int v = monitor_lock_count;
                        boolean cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, v, v+1);
                        if(!cas)
                            throw new RuntimeException("cas");
//                        assert(cas);
                        Thread00DJ ct =  Thread00DJ.currentThread();
                        if(monitor_thread == ct)
                            return;
                        monitor_thread = ct;
                        break;
                    }
                }
            }
        //}
    }

    public void releaseMonitor() {
        //synchronized (this) {
        ObjectBase managedObject = get();
        if((managedObject.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
                // communicate with the master
                synchronized (managedObject) {
                    assert(monitor_lock_count > 0 && monitor_thread == Thread00DJ.currentThread());
//                    monitor_lock_count--;
                    int v = monitor_lock_count;
                    boolean cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, v, v-1);
                    if(!cas)
                        throw new RuntimeException("cas");
//                    assert(cas);
                    if(v == 1) {
                        monitor_thread = null;
                        InternalInterface.getInternalInterface().releaseObjectMonitor(objectId(), owning_machine, notifications_to_send);
                        notifications_to_send = 0;
                    }
                }
            } else {
                synchronized (managedObject) {
                    assert(monitor_lock_count > 0 && monitor_thread == Thread00DJ.currentThread());
                    int v = monitor_lock_count;
                    boolean cas = unsafe.compareAndSwapInt(this, class_manager_monitor_lock_offset, v, v-1);
                    if(!cas)
                        throw new RuntimeException("cas");
//                    assert(cas);
//                    monitor_lock_count--;
                    if(v == 1) {
                        monitor_thread = null;
                        processNotifications();
                    }
                }
            }
        //}
    }

    private void checkHasLock() {
        ObjectBase managedObject = get();
        synchronized (managedObject) {
            if(monitor_thread != Thread00DJ.currentThread() || monitor_lock_count == 0) {
                int mode = getMode();
                throw new IllegalMonitorStateException("DJ: lock count: " + monitor_lock_count +
                        " matching current thread: " + (monitor_thread != Thread00DJ.currentThread()) +
                        " owner: " + monitor_thread +
                        " obj is null:" + (managedObject == null) +
                        " mode: "+CONSTS.str(mode));
            }

        }
    }


    // serialization methods

    int getSerializedSize() {
        // return the number of bytes that will be required for this object
        // TODO:
        return 200;
    }


    void dj_serialize_obj(SerializeManager man, SerializeManager.SerializationAction act) {
        // only call in the case that the object is getting moved

        // notifications to send is local
        // the monitor thread is also local

        assert(monitor_lock_count == 0);

        if(cached_copies == null) {
            man.put_value_I(0);
        } else {
            man.put_value_I(cached_copies.length);
            for(int i = 0; i < cached_copies.length; i++) {
                man.put_value_I(cached_copies[i]);
            }
        }

        int selfId = InternalInterface.getInternalInterface().getSelfId();

        if(waitingMachines == null) {
            man.put_value_I(0);
        } else {
            man.put_value_I(waitingMachines.length);
            for(int i = 0; i < waitingMachines.length; i++) {
                if(waitingMachines[i] == -1) {
                    man.put_value_I(selfId);
                } else {
                    man.put_value_I(waitingMachines[i]);
                }
            }
        }

        // we are losing one remote ref in the machine that is getting this object
        // and gaining another remote ref from this machine
        int cref;
        do {
            cref = reference_count;
        } while(!unsafe.compareAndSwapInt(this, class_manager_ref_field_offset, cref, 0));

        man.put_value_I(cref);

    }

    void dj_deserialize_obj(SerializeManager man, SerializeManager.SerializationAction act) {

        assert(cached_copies == null);

        int cached_length = man.get_value_I();
        if(cached_length == 0){
            cached_copies = null;
        } else {
            cached_copies = new int[cached_length];
            for(int i = 0; i < cached_length; i++) {
                cached_copies[i] = man.get_value_I();
            }
        }

        int selfId = InternalInterface.getInternalInterface().getSelfId();

        assert(waitingMachines == null);

        int waiting_length = man.get_value_I();
        if(waiting_length == 0) {
            waitingMachines = null;
        } else {
            waitingMachines = new int[waiting_length];
            for(int i = 0; i < waiting_length; i++) {
                int val = man.get_value_I();
                if(val == selfId)
                    waitingMachines[i] = -1;
                else
                    waitingMachines[i] = val;
            }
        }

        int delta = man.get_value_I();
        int cref;
        int nref;
        do {
            cref = reference_count;
            nref = cref + delta;
        } while(!unsafe.compareAndSwapInt(this, class_manager_ref_field_offset, cref, nref));
    }


    // there should be some seralization methods added to the class


    static private Unsafe unsafe = InternalInterface.getInternalInterface().getUnsafe();

    static final int class_manager_ref_field_offset;
    static final int class_manager_cache_copies_offset;
    static final int class_manager_owning_machine_offset;
    static final int class_manager_monitor_lock_offset;
    static {
        int v = -1;
        try {
            v = (int)unsafe.objectFieldOffset(ClassManager.class.getDeclaredField("reference_count"));
        } catch (NoSuchFieldException e) {
        }
        class_manager_ref_field_offset = v;
        v = -1;
        try {
            v = (int)unsafe.objectFieldOffset(ClassManager.class.getDeclaredField("cached_copies"));
        } catch(NoSuchFieldException e) {}
        class_manager_cache_copies_offset = v;
        v = -1;
        try {
            v = (int)unsafe.objectFieldOffset(ClassManager.class.getDeclaredField("owning_machine"));
        } catch(NoSuchFieldException e) {}
        class_manager_owning_machine_offset = v;
        v = -1;
        try {
            v = (int)unsafe.objectFieldOffset(ClassManager.class.getDeclaredField("monitor_lock_count"));
        } catch(NoSuchFieldException e) {}
        class_manager_monitor_lock_offset = v;
    }
}
