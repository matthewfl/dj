package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.java.lang.Object00;
import edu.berkeley.dj.internal.coreclazz.sun.misc.Unsafe00;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {"java/util/HashMap", "java/nio/ByteBuffer", "java/util/UUID", "java/lang/Thread"})
public class DistributedObjectHelper {

    private DistributedObjectHelper() {}

    static public final class DistributedObjectId implements Serializable {
        int lastKnownHost;
        UUID identifier;
        String classname;

        @Override
        public int hashCode() { return identifier.hashCode(); }

        @Override
        public String toString() { return "DJ_OBJ("+identifier+")"; }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof DistributedObjectId))
                return false;
            return identifier.equals(((DistributedObjectId)o).identifier);
        }

        private DistributedObjectId() {}

        DistributedObjectId(UUID u, int h, String cn) {
            this.identifier = u;
            this.lastKnownHost = h;
            this.classname = cn;
        }

        ByteBuffer toBB() {
            byte[] cn = classname.getBytes();
            ByteBuffer ret = ByteBuffer.allocate(cn.length + 20);
            ret.putInt(lastKnownHost);
            ret.putLong(identifier.getMostSignificantBits());
            ret.putLong(identifier.getLeastSignificantBits());
            ret.put(cn);
            return ret;
        }

        public byte[] toArr() {
            return toBB().array();
        }

        public DistributedObjectId(byte[] arr) {
            ByteBuffer b = ByteBuffer.wrap(arr);
            lastKnownHost = b.getInt();
            identifier = new UUID(b.getLong(), b.getLong());
            classname = new String(arr, 20, arr.length - 20);
        }

        DistributedObjectId(ByteBuffer b) {
            lastKnownHost = b.getInt();
            identifier = new UUID(b.getLong(), b.getLong());
            classname = new String(b.array(), b.position(), b.limit() - b.position());
        }

    }

    static private HashMap<UUID, Object00> localDistributedObjects = new HashMap<>();

    // give the Object some uuid so that it will be distribuited
    static public void makeDistributed(ObjectBase o) {
        InternalInterface.getInternalInterface().typeDistributed(o.getClass().getName());
        if(o.__dj_class_manager == null) {
            boolean ownsLock = Thread.currentThread().holdsLock(o);
            synchronized (o) {
                o.__dj_class_manager = new ClassManager(o);
                synchronized (localDistributedObjects) {
                    localDistributedObjects.put(o.__dj_class_manager.distributedObjectId, o);
                }
                if(ownsLock) {
                    // this thread is currently has a monitor lock on the object
                    // but as we change it to be a distributed object we need to tracked the lock
                    // different way since we may reference this object from another machine
                    throw new NotImplementedException();
                }
            }
        }
    }

    static public DistributedObjectId getDistributedId(ObjectBase o) {
        makeDistributed(o);
        int h = o.__dj_class_manager.owning_machine;
        if(h == -1)
            h = InternalInterface.getInternalInterface().getSelfId();
        return new DistributedObjectId(o.__dj_class_manager.distributedObjectId, h, o.getClass().getName());
    }

    static public DistributedObjectId getDistributedId(Object o) {
        if(!(o instanceof ObjectBase)) {
            throw new RuntimeException("something that is not an object base: "+o);
        }
        return getDistributedId((ObjectBase) o);
    }

    static public Object getObject(DistributedObjectId id) {
        synchronized (localDistributedObjects) {
            Object00 h = localDistributedObjects.get(id.identifier);
            if(h != null)
                return h;
            // we do not have some proxy of this object locally so we need to construct some proxy for it
            try {
                // shouldn't need the AugmentedClassLoader since the classname will already be augmented
                // this will save a round trip communication with the master machine
                Class<?> cls = Class.forName(id.classname);
                ObjectBase obj = (ObjectBase) Unsafe00.getUnsafe().allocateInstance(cls);
                obj.__dj_class_manager = new ClassManager(obj, id.identifier, id.lastKnownHost);
                obj.__dj_class_mode |= CONSTS.OBJECT_INITED |
                        CONSTS.REMOTE_READS |
                        CONSTS.REMOTE_WRITES |
                        CONSTS.IS_NOT_MASTER;
                localDistributedObjects.put(id.identifier, obj);
                return obj;
            } catch(ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch(InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static public boolean isLocal(ObjectBase o) {
        if(o.__dj_class_manager == null)
            return true;
        if(o.__dj_class_manager.owning_machine == -1)
            return true;
        return false;
    }

    static public void updateObjectLocation(UUID id, int machine_location) {
        Object00 h;
        synchronized (localDistributedObjects) {
            h = localDistributedObjects.get(id);
        }
        if(h == null)
            return;
        h.__dj_getManager().owning_machine = machine_location;
    }

    static public ByteBuffer readField(int op, ByteBuffer req) {
        UUID id = new UUID(req.getLong(), req.getLong());
        int fid = req.getInt();
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.REMOTE_READS) != 0) {
            // need to redirect the request elsewhere
            throw new NotImplementedException();
        }
        ByteBuffer ret;
        switch(op) {
            case 10:
                ret = ByteBuffer.allocate(1);
                if(h.__dj_readFieldID_Z(fid)) {
                    ret.put((byte)1);
                } else {
                    ret.put((byte)0);
                }
                return ret;
            case 11:
                ret = ByteBuffer.allocate(4);
                ret.putChar(h.__dj_readFieldID_C(fid));
                return ret;
            case 12:
                ret = ByteBuffer.allocate(1);
                ret.put(h.__dj_readFieldID_B(fid));
                return ret;
            case 13:
                ret = ByteBuffer.allocate(2);
                ret.putShort(h.__dj_readFieldID_S(fid));
                return ret;
            case 14:
                ret = ByteBuffer.allocate(4);
                ret.putInt(h.__dj_readFieldID_I(fid));
                return ret;
            case 15:
                ret = ByteBuffer.allocate(8);
                ret.putLong(h.__dj_readFieldID_J(fid));
                return ret;
            case 16:
                ret = ByteBuffer.allocate(4);
                ret.putFloat(h.__dj_readFieldID_F(fid));
                return ret;
            case 17:
                ret = ByteBuffer.allocate(8);
                ret.putDouble(h.__dj_readFieldID_D(fid));
                return ret;
            case 18: // OBJECT
                return DistributedObjectHelper.getDistributedId(h.__dj_readFieldID_A(fid)).toBB();
            default:
                throw new InterfaceException();
        }
    }

    static public void writeField(int op, ByteBuffer req) {
        UUID id = new UUID(req.getLong(), req.getLong());
        int fid = req.getInt();
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.REMOTE_WRITES) != 0) {
            // need to redirect the request elsewhere
            throw new NotImplementedException();
        }
        switch(op) {
            case 20:
                h.__dj_writeFieldID_Z(fid, req.get() == 1);
                return;
            case 21:
                h.__dj_writeFieldID_C(fid, req.getChar());
                return;
            case 22:
                h.__dj_writeFieldID_B(fid, req.get());
                return;
            case 23:
                h.__dj_writeFieldID_S(fid, req.getShort());
                return;
            case 24:
                h.__dj_writeFieldID_I(fid, req.getInt());
                return;
            case 25:
                h.__dj_writeFieldID_J(fid, req.getLong());
                return;
            case 26:
                h.__dj_writeFieldID_F(fid, req.getFloat());
                return;
            case 27:
                h.__dj_writeFieldID_D(fid, req.getDouble());
                return;
            case 28: // OBJECT
                h.__dj_writeFieldID_A(fid, DistributedObjectHelper.getObject(new DistributedObjectId(req)));
                return;
            default:
                throw new InterfaceException();
        }
    }

    static public void waitingFrom(int machine, ByteBuffer obj) {
        UUID id = new UUID(obj.getLong(), obj.getLong());
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
            // we are not the master machine here, we should forward this request
            throw new NotImplementedException();
        }
        h.__dj_class_manager.addMachineToWaiting(machine);
    }

    static public void notifyObject(ByteBuffer obj) {
        UUID id = new UUID(obj.getLong(), obj.getLong());
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        // TODO:
    }

    static public boolean lockMonitor(ByteBuffer obj, boolean spin) {
        UUID id = new UUID(obj.getLong(), obj.getLong());
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0)
            // redirect to the correct machine
            throw new NotImplementedException();
        synchronized (h) {
            // we have a param for spinning since we do not want to block the io threads with spinning on an object
            do {
                synchronized (h) {
                    if(h.__dj_class_manager.monitor_lock_count != 0)
                        continue;
                    h.__dj_class_manager.monitor_lock_count = 1;
                    return true;
                }
            } while(spin);
            return false;
        }
    }

    static public void unlockMonitor(ByteBuffer obj) {
        UUID id = new UUID(obj.getLong(), obj.getLong());
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0)
            // redirect to the correct machine
            throw new NotImplementedException();
        synchronized (h.__dj_class_manager) {
            assert(h.__dj_class_manager.monitor_lock_count == 1);
            h.__dj_class_manager.monitor_lock_count = 0;
        }
    }


}
