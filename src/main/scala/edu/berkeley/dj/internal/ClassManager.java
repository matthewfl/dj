package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.coreclazz.java.lang.Object00DJ;
import edu.berkeley.dj.internal.coreclazz.java.lang.Thread00DJ;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by matthewfl
 *
 * Represents the backing information for a class that is shared between multiple machines
 *
 */
@RewriteAllBut(nonModClasses = {"java/util/UUID", "java/nio/ByteBuffer"}) // tmp
final public class ClassManager {

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
    ObjectBase managedObject;

    ClassManager(Object00DJ o) {
        managedObject = (ObjectBase)o;
        distributedObjectId = UUID.randomUUID();
    }

    ClassManager(Object00DJ o, UUID id, int owner) {
        managedObject = (ObjectBase)o;
        distributedObjectId = id;
        owning_machine = owner;
    }

    // TODO: I suppose that the identifier for the field can be a short, since that would be a limitation
    // on the class already

    protected int getMode() { return  managedObject.__dj_getClassMode(); }

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
        return DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(res.array()));
    }


    private ByteBuffer requestRead(int fid, int op) {
        ByteBuffer bb = requestRemote(fid, 0);
        JITWrapper.recordRemoteRead(managedObject, fid, owning_machine);
        return InternalInterface.getInternalInterface().readField(bb, op, owning_machine);
    }

    private ByteBuffer requestRemote(int fid, int exces) {
        ByteBuffer bb = ByteBuffer.allocate(20 + exces);
        bb.putLong(distributedObjectId.getMostSignificantBits());
        bb.putLong(distributedObjectId.getLeastSignificantBits());
        bb.putInt(fid);
        return bb;
    }

    private void requestWrite(ByteBuffer bb, int op, int fid) {
        JITWrapper.recordRemoteWrite(managedObject, fid, owning_machine);
        InternalInterface.getInternalInterface().writeField(bb, op, owning_machine);
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
        synchronized (managedObject) {
            synchronized (this) {
                if(waitingMachines != null && waitingMachines.length > 0) {
                    int i = waitingMachines[0];
                    if(i == -1) {
                        managedObject.notify();
                    } else {
                        InternalInterface.getInternalInterface().sendNotifyOnObject(
                                objectId().array(), i);
                        assert(monitor_lock_count == 0);
                        monitor_lock_count = 1;
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
        if((getMode() & CONSTS.IS_NOT_MASTER) == 0) {
            // we are master
            //assert(monitor_lock_count != 0 && monitor_thread == Thread00.currentThread());
            synchronized (managedObject) {
                processNotifications();
                addMachineToWaiting(-1);
                int cnt = monitor_lock_count;
                monitor_thread = null;
                monitor_lock_count = 0;
                //assert(notifications_to_send == 0);
                managedObject.wait();
                monitor_lock_count = cnt;
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
                managedObject.wait();
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
            if((managedObject.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
                // we have to communicate with the master
                // first get the lock on the local object
                while (true) {
                    synchronized (managedObject) {
                        if(monitor_lock_count != 0 && monitor_thread != Thread00DJ.currentThread()) {
                            continue;
                        }
                        monitor_lock_count++;
                        monitor_thread = Thread00DJ.currentThread();
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
                        monitor_lock_count++;
                        monitor_thread = Thread00DJ.currentThread();
                        break;
                    }
                }
            }
        //}
    }

    public void releaseMonitor() {
        //synchronized (this) {
            if((managedObject.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
                // communicate with the master
                synchronized (managedObject) {
                    assert(monitor_lock_count > 0 && monitor_thread == Thread00DJ.currentThread());
                    monitor_lock_count--;
                    if(monitor_lock_count == 0) {
                        monitor_thread = null;
                        InternalInterface.getInternalInterface().releaseObjectMonitor(objectId(), owning_machine, notifications_to_send);
                        notifications_to_send = 0;
                    }
                }
            } else {
                synchronized (managedObject) {
                    assert(monitor_lock_count > 0 && monitor_thread == Thread00DJ.currentThread());
                    monitor_lock_count--;
                    if(monitor_lock_count == 0) {
                        monitor_thread = null;
                        processNotifications();
                    }
                }
            }
        //}
    }

    private void checkHasLock() {
        synchronized (managedObject) {
            if(monitor_lock_count == 0 || monitor_thread != Thread00DJ.currentThread())
                throw new IllegalMonitorStateException();
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


    }

    void dj_deserialize_obj(SerializeManager man, SerializeManager.SerializationAction act) {
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
    }


    // there should be some seralization methods added to the class

}
