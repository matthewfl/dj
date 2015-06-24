package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.coreclazz.java.lang.Object00;
import edu.berkeley.dj.internal.coreclazz.java.lang.Thread00;
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

    Thread00 monitor_thread = null;

    // TODO: if we move this thread elsewhere then we still need to have the count
    // so I guess will have to make the master machien keep the count of the number of times the monitor has been
    // aquired
    int monitor_lock_count = 0;

    // will need a week pointer to the object base
    ObjectBase managedObject;

    ClassManager(Object00 o) {
        managedObject = (ObjectBase)o;
        distributedObjectId = UUID.randomUUID();
    }

    ClassManager(Object00 o, UUID id, int owner) {
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
        requestWrite(b, 20);
    }

    public void writeField_C(int id, char v) {
        ByteBuffer b = requestRemote(id, 4);
        b.putChar(v);
        requestWrite(b, 21);
    }

    public void writeField_B(int id, byte v) {
        ByteBuffer b = requestRemote(id, 1);
        b.put(v);
        requestWrite(b, 22);
    }

    public void writeField_S(int id, short v) {
        ByteBuffer b = requestRemote(id, 2);
        b.putShort(v);
        requestWrite(b, 23);
    }

    public void writeField_I(int id, int v) {
        ByteBuffer b = requestRemote(id, 4);
        b.putInt(v);
        requestWrite(b, 24);
    }

    public void writeField_J(int id, long v) {
        ByteBuffer b = requestRemote(id, 8);
        b.putLong(v);
        requestWrite(b, 25);
    }

    public void writeField_F(int id, float v) {
        ByteBuffer b = requestRemote(id, 4);
        b.putFloat(v);
        requestWrite(b, 26);
    }

    public void writeField_D(int id, double v) {
        ByteBuffer b = requestRemote(id, 8);
        b.putDouble(v);
        requestWrite(b, 27);
    }

    public void writeField_A(int id, Object v) {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }


    private ByteBuffer requestRead(int fid, int op) {
        ByteBuffer bb = requestRemote(fid, 0);
        return InternalInterface.getInternalInterface().readField(bb, op, owning_machine);
    }

    private ByteBuffer requestRemote(int fid, int exces) {
        ByteBuffer bb = ByteBuffer.allocate(20 + exces);
        bb.putLong(distributedObjectId.getMostSignificantBits());
        bb.putLong(distributedObjectId.getLeastSignificantBits());
        bb.putInt(fid);
        return bb;
    }

    private void requestWrite(ByteBuffer bb, int op) {
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

    private int getNextWaitingMachine() {
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
    }

    public void dj_notify() {
        int n = getNextWaitingMachine();
        if(n == -1) {
            managedObject.notify();
        } else {
            // send the notification to another machine
        }
    }

    public void dj_notifyAll() {
        synchronized (this) {
            if(waitingMachines == null) return;
            for(int i = 0; i < waitingMachines.length; i++) {
                if(waitingMachines[i] == -1) {
                    managedObject.notify();
                } else {
                    // send notification to another machine
                }
            }
        }
    }

    public void dj_wait() throws InterruptedException {
        if((getMode() & CONSTS.IS_NOT_MASTER) == 0) {
            // we are msater
            addMachineToWaiting(-1);
        } else {

        }
        managedObject.wait();
    }

    public void dj_wait(long timeout) throws InterruptedException {
        // need to remove self from the queue somehow
        // I guess the queue could also have the timeout for the object contained with them, but idk how well that would work
    }

    public void dj_wait(long timeout, long nanos) throws InterruptedException {

    }

    private ByteBuffer objectId() {
        ByteBuffer b = ByteBuffer.allocate(16);
        b.putLong(distributedObjectId.getMostSignificantBits());
        b.putLong(distributedObjectId.getLeastSignificantBits());
        return b;
    }


    public void acquireMonitor() {
        synchronized (this) {
            if((managedObject.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
                // we have to communicate with the master
                // first get the lock on the local object
                while (true) {
                    synchronized (this) {
                        if(monitor_lock_count != 0 && monitor_thread != Thread00.currentThread()) {
                            continue;
                        }
                        monitor_lock_count++;
                        monitor_thread = Thread00.currentThread();
                        break;
                    }
                }
                InternalInterface.getInternalInterface().acquireObjectMonitor(objectId(), owning_machine);
            } else {
                // we are the master
                while(true) {
                    synchronized (this) {
                        if (monitor_lock_count != 0 && monitor_thread != Thread00.currentThread()) {
                            continue;
                        }
                        monitor_lock_count++;
                        monitor_thread = Thread00.currentThread();
                    }
                }
            }
        }
    }

    public void releaseMonitor() {
        synchronized (this) {
            if((managedObject.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
                // communicate with the master
                synchronized (this) {
                    assert(monitor_lock_count > 0 && monitor_thread == Thread00.currentThread());
                    if(monitor_lock_count == 1) {
                        monitor_thread = null;
                        InternalInterface.getInternalInterface().releaseObjectMonitor(objectId(), owning_machine);
                    }
                    monitor_lock_count--;
                }
            } else {
                synchronized (this) {
                    assert(monitor_lock_count > 0 && monitor_thread == Thread00.currentThread());
                    if(monitor_lock_count == 1) {
                        monitor_thread = null;
                    }
                    monitor_lock_count--;
                }

            }
        }
    }



    // there should be some seralization methods added to the class

}
