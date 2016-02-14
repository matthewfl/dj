package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.java.lang.Object00DJ;
import edu.berkeley.dj.internal.coreclazz.sun.misc.Unsafe00DJ;
import sun.misc.Unsafe;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {
        "java/util/HashMap",
        "java/nio/ByteBuffer",
        "java/util/UUID",
        "java/lang/Thread",
        "java/nio/Buffer",
        "java/lang/System",
        "java/io/ByteArrayOutputStream",
        "java/io/ObjectOutputStream",
        "java/io/ByteArrayInputStream",
        "java/io/ObjectInputStream",
        "java/io/OutputStream",
        "java/io/InputStream",
        "sun/misc/Unsafe",
        "java/lang/ref/"
})
public class DistributedObjectHelper {

    private DistributedObjectHelper() {}

    // TODO: make this _not_ extend ObjectBase
    static public final class DistributedObjectId implements Serializable {
        int lastKnownHost;
        UUID identifier;
        //String classname;
        byte[] extradata;

        @Override
        public int hashCode() { return identifier.hashCode(); }

        @Override
        public String toString() {
            if(!isFinalObj())
                return "DJ_OBJ("+identifier+", "+new String(extradata)+")";
            else
                return "DJ_OBJ(final)";
        }

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
            this.extradata = cn.getBytes();
            //this.classname = cn;
        }

        ByteBuffer toBB() {
            if(lastKnownHost != -2) {
                byte[] cn = extradata;
                ByteBuffer ret = ByteBuffer.allocate(cn.length + 24);
                ret.putInt(lastKnownHost);
                ret.putLong(identifier.getMostSignificantBits());
                ret.putLong(identifier.getLeastSignificantBits());
                ret.putInt(cn.length);
                ret.put(cn);
                return ret;
            } else {
                return ByteBuffer.wrap(extradata);
            }
        }

        void saveBB(ByteBuffer ret) {
            if(lastKnownHost != -2) {
                ret.putInt(lastKnownHost);
                ret.putLong(identifier.getMostSignificantBits());
                ret.putLong(identifier.getLeastSignificantBits());
                ret.putInt(extradata.length);
                ret.put(extradata);
            } else {
                ret.put(extradata);
            }
        }

        public byte[] toArr() {
            return toBB().array();
        }

        public boolean isFinalObj() { return lastKnownHost == -2; }

        public DistributedObjectId(byte[] arr) {
            ByteBuffer b = ByteBuffer.wrap(arr);
            lastKnownHost = b.getInt();
            if(lastKnownHost == -2) {
                extradata = arr;
            } else {
                identifier = new UUID(b.getLong(), b.getLong());
                int length = b.getInt();
                extradata = new byte[length]; //arr.length - 20];
                System.arraycopy(arr, 24, extradata, 0, extradata.length);
            }
        }

        DistributedObjectId(ByteBuffer b) {
            lastKnownHost = b.getInt();
            if(lastKnownHost == -2) {
                int length = b.getInt();
                byte[] barr = b.array();
                if(length == barr.length - 8) {
                    assert(b.position() == 8);
                    extradata = barr;
                } else {
                    extradata = new byte[length + 8];
                    b.position(b.position() - 8);
                    b.get(extradata);
                }
            } else {
                identifier = new UUID(b.getLong(), b.getLong());
                int length = b.getInt();
                extradata = new byte[length];//b.limit() - b.position()];
                b.get(extradata);
            }
        }

    }


    // this will contain the object itself or the class manager
    // the class manager contains a weak reference to the object itself
    // so if it gets gc, then we can know and notify the true master that we have
    // lost the reference
    static private HashMap<UUID, Object> localDistributedObjects = new HashMap<>();

    static final ReferenceQueue<Object> gcRefQueue = new ReferenceQueue<>();

     static private ObjectBase getLocalObject(UUID id) {
        synchronized (localDistributedObjects) {
            // check if it is a weak reference and get the actual object that it points to instead
            Object o = localDistributedObjects.get(id);
            if(o instanceof ClassManager) {
                return ((ClassManager)o).get();
            }
            return (ObjectBase)o;
        }
    }

    static private void makeWeakRef(ObjectBase ob) {
        synchronized (localDistributedObjects) {
            assert(ob.__dj_class_manager != null);
            Object cur = localDistributedObjects.get(ob.__dj_class_manager.distributedObjectId);
            if(!(cur instanceof ClassManager))
                localDistributedObjects.put(ob.__dj_class_manager.distributedObjectId, ob.__dj_class_manager);
        }
    }

    static private void makeStrongRef(ObjectBase ob) {
        synchronized (localDistributedObjects) {
            assert(ob.__dj_class_manager != null);
            //Object cur = localDistributedObjects.get(ob.__dj_class_manager.distributedObjectId);
            localDistributedObjects.put(ob.__dj_class_manager.distributedObjectId, ob);
        }
    }


    // give the Object some uuid so that it will be distributed
    static public void makeDistributed(ObjectBase o) {
        if(o.__dj_class_manager == null) {
            // if we have a class manager then we must already be distributed
            InternalInterface.getInternalInterface().typeDistributed(o.getClass().getName());
            boolean ownsLock = Thread.currentThread().holdsLock(o);
            synchronized (o) {
                if(o.__dj_class_manager != null)
                    return;
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
                // send a notification to any objects that may be waiting on this
                // they will start waiting on the distributed object
                o.notifyAll();
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
            //throw new RuntimeException("something that is not an object base: "+o);
            return makeFinalObjectId(o);
        }
        return getDistributedId((ObjectBase) o);
    }

    static public Object getObject(DistributedObjectId id) {
        Object00DJ h = getLocalObject(id.identifier);
//        synchronized (localDistributedObjects) {
//            h = localDistributedObjects.get(id.identifier);
//        }
        if(h != null)
            return h;
        // we do not have some proxy of this object locally so we need to construct some proxy for it
        try {
            if(id.isFinalObj() /* lastKnownHost == -2 */) {
                // this is some final object, and we just need to reconstruct it
                return constructFinalObject(ByteBuffer.wrap(id.extradata));
            } else {
                // shouldn't need the AugmentedClassLoader since the classname will already be augmented
                // this will save a round trip communication with the master machine
                // this potentially represents a round trip as a remote class is loaded
                // that is why we release the lock before we get to this point
                Class<?> cls = Class.forName(new String(id.extradata));
                ObjectBase obj = (ObjectBase) Unsafe00DJ.getUnsafe().allocateInstance(cls);
                assert(id.lastKnownHost != -1);
                if(id.lastKnownHost == InternalInterface.getInternalInterface().getSelfId()) {
                    // ga wtf
                    InternalInterface.getInternalInterface().relocateObject(id.identifier);
                }
                obj.__dj_class_manager = new ClassManager(obj, id.identifier, id.lastKnownHost);
                obj.__dj_class_mode |= CONSTS.OBJECT_INITED |
                        CONSTS.REMOTE_READS |
                        CONSTS.REMOTE_WRITES |
                        CONSTS.IS_NOT_MASTER |
                        CONSTS.IS_PROXY_OBJ;
                synchronized (localDistributedObjects) {
                    // check again if we now have the object
                    // if we do, then return that instead
                    // the other objects will end up getting collected shortly
                    h = getLocalObject(id.identifier);
                    if(h != null)
                        return h;
                    localDistributedObjects.put(id.identifier, obj);
                }
                InternalInterface.getInternalInterface().changeReferenceCount(id.identifier, 1, id.lastKnownHost);
                JITWrapper.recordProxyObjectCreated(obj);
                return obj;
            }
        } catch(ClassNotFoundException|InstantiationException e) {
            throw new RuntimeException(e);
        }

    }

    static Object constructFinalObject(ByteBuffer buf) {
        try {
            //buf.rewind();
            //buf.position(buf.position() - 4); // go back to the start of the message
            int objectIdent = buf.getInt();
            assert (objectIdent == -2); // check that this is of the correct type
            int msglen = buf.getInt();
            int cnamelen = buf.getInt();
            String cname = new String(buf.array(), buf.position(), cnamelen);
            buf.position(cnamelen + buf.position());
            Class<?> cls = Class.forName(cname);
            finalObjectConverter<?> conv = finalObjectConverters.get(cls);
            if (conv == null)
                throw new RuntimeException();
            return conv.makeObject(buf);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static DistributedObjectId makeFinalObjectId(Object o) {
        if(o == null) {
            o = nullcls;
        }
        Class<?> cls = o.getClass();
        finalObjectConverter<?> conv = finalObjectConverters.get(cls);
        if(conv == null) {
            if(Throwable.class.isAssignableFrom(cls)) {
                cls = Throwable.class;
                conv = finalObjectConverters.get(cls);
            } else {
                InternalInterface.debug("failed class convert " + cls.getName());
                throw new RuntimeException("could not find a converter for class: " + cls.getName());
            }
        }
        int size = conv.getSizeO(o);
        byte[] cname = cls.getName().getBytes();
        ByteBuffer a = ByteBuffer.allocate(4 + 4 + 4 + cname.length + size);
        a.putInt(-2);
        a.putInt(cname.length + size + 4);
        a.putInt(cname.length);
        a.put(cname);
        conv.makeIdO(o, a);
        a.flip();
        return new DistributedObjectId(a);
    }

    static abstract class finalObjectConverter<T> {
        abstract public int getSize(T o);
        abstract public T makeObject(ByteBuffer buf);
        abstract public void makeId(T o, ByteBuffer id);

        public int getSizeO(Object o) { return getSize((T) o); }
        public void makeIdO(Object o, ByteBuffer b) { makeId((T)o, b); }

    }

    static private final HashMap<Class<?>, finalObjectConverter<?>> finalObjectConverters = new HashMap<>();

    // such a hack
    static private class NULLCLS {}

    static final private NULLCLS nullcls = new NULLCLS();

    static {
        finalObjectConverters.put(NULLCLS.class, new finalObjectConverter<NULLCLS>() {
            @Override
            public int getSize(NULLCLS o) {
                return 4;
            }

            @Override
            public NULLCLS makeObject(ByteBuffer buf) {
                buf.getInt();
                return null;
            }

            @Override
            public void makeId(NULLCLS o, ByteBuffer id) {
                id.putInt(0);
            }
        });
        finalObjectConverters.put(String.class, new finalObjectConverter<String>() {
            @Override
            public int getSize(String o) {
                return o.length() * 4 + 4;
            }

            @Override
            public String makeObject(ByteBuffer buf) {
                int length = buf.getInt();
                String r = new String(buf.array(), buf.position(), length);
                buf.position(buf.position() + length);
                return r;
            }

            @Override
            public void makeId(String o, ByteBuffer id) {
                byte[] b = o.getBytes();
                id.putInt(b.length);
                id.put(b);
            }
        });
        finalObjectConverters.put(Class.class, new finalObjectConverter<Class<?>>() {
            @Override
            public int getSize(Class<?> o) {
                return o.getName().length() * 4 + 4;
            }

            @Override
            public Class<?> makeObject(ByteBuffer buf) {
                int length = buf.getInt();
                String name = new String(buf.array(), buf.position(), length);
                buf.position(buf.position() + length);
                try {
                    // returns null if name is not a primitive class type
                    Class<?> ret = AugmentedClassLoader.getPrimitiveClass(name);
                    if(ret != null)
                        return ret;
                } catch(Throwable e) {}
                try {
                    return Class.forName(name);
                } catch(ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void makeId(Class<?> o, ByteBuffer id) {
                byte[] name = o.getName().getBytes();
                id.putInt(name.length);
                id.put(name);
            }
        });
        finalObjectConverters.put(Throwable.class, new finalObjectConverter<Throwable>() {
            // should try and seralize the object
            // and then reconstruct the object on the other end....

            @Override
            public int getSize(Throwable o) {
                try {
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    ObjectOutputStream oss = new ObjectOutputStream(bao);
                    oss.writeObject(o);
                    oss.flush();
                    return bao.toByteArray().length + 4;
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
//                throw new NotImplementedException();
            }

            @Override
            public Throwable makeObject(ByteBuffer buf) {
                try {
                    int length = buf.getInt();
                    byte[] arr = new byte[length];
                    buf.get(arr);
                    ByteArrayInputStream bai = new ByteArrayInputStream(arr);
                    ObjectInputStream ois = new ObjectInputStream(bai);
                    return (Throwable)ois.readObject();
                } catch(IOException|ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void makeId(Throwable o, ByteBuffer id) {
                try {
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    ObjectOutputStream oss = new ObjectOutputStream(bao);
                    oss.writeObject(o);
                    oss.flush();
                    byte[] a = bao.toByteArray();
                    id.putInt(a.length);
                    id.put(a);
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
//                throw new NotImplementedException();
            }
        });
        finalObjectConverters.put(Byte.class, new finalObjectConverter<Byte>() {
            @Override
            public int getSize(Byte o) {
                return 5;
            }

            @Override
            public Byte makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.get();
            }

            @Override
            public void makeId(Byte o, ByteBuffer id) {
                id.putInt(1);
                id.put(o);
            }
        });
        finalObjectConverters.put(Boolean.class, new finalObjectConverter<Boolean>() {
            @Override
            public int getSize(Boolean o) {
                return 5;
            }

            @Override
            public Boolean makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.get() == 1;
            }

            @Override
            public void makeId(Boolean o, ByteBuffer id) {
                id.putInt(1);
                if(o) {
                    id.put((byte)1);
                } else {
                    id.put((byte)0);
                }
            }
        });
        finalObjectConverters.put(Character.class, new finalObjectConverter<Character>() {
            @Override
            public int getSize(Character o) {
                return 8;
            }

            @Override
            public Character makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.getChar();
            }

            @Override
            public void makeId(Character o, ByteBuffer id) {
                id.putInt(4);
                id.putChar(o);
            }
        });
        finalObjectConverters.put(Short.class, new finalObjectConverter<Short>() {
            @Override
            public int getSize(Short o) {
                return 6;
            }

            @Override
            public Short makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.getShort();
            }

            @Override
            public void makeId(Short o, ByteBuffer id) {
                id.putInt(2);
                id.putShort(o);
            }
        });
        finalObjectConverters.put(Integer.class, new finalObjectConverter<Integer>() {
            @Override
            public int getSize(Integer o) {
                return 8;
            }

            @Override
            public Integer makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.getInt();
            }

            @Override
            public void makeId(Integer o, ByteBuffer id) {
                id.putInt(4);
                id.putInt(o);
            }
        });
        finalObjectConverters.put(Long.class, new finalObjectConverter<Long>() {
            @Override
            public int getSize(Long o) {
                return 12;
            }

            @Override
            public Long makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.getLong();
            }

            @Override
            public void makeId(Long o, ByteBuffer id) {
                id.putInt(8);
                id.putLong(o);
            }
        });
        finalObjectConverters.put(Float.class, new finalObjectConverter<Float>() {
            @Override
            public int getSize(Float o) {
                return 8;
            }

            @Override
            public Float makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.getFloat();
            }

            @Override
            public void makeId(Float o, ByteBuffer id) {
                id.putInt(4);
                id.putFloat(o);
            }
        });
        finalObjectConverters.put(Double.class, new finalObjectConverter<Double>() {
            @Override
            public int getSize(Double o) {
                return 12;
            }

            @Override
            public Double makeObject(ByteBuffer buf) {
                buf.getInt();
                return buf.getDouble();
            }

            @Override
            public void makeId(Double o, ByteBuffer id) {
                id.putInt(8);
                id.putDouble(o);
            }
        });
    }

    static public boolean isLocal(ObjectBase o) {
        if(o.__dj_class_manager == null)
            return true;
        if(o.__dj_class_manager.owning_machine == -1)
            return true;
        return false;
    }

    static public void updateObjectLocation(UUID id, int machine_location) {
        ObjectBase h = getLocalObject(id);
        if(h == null)
            return;
        // this is the master machine, we should not be telling it where the owner is
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) == 0)
            return;
        int oldLoc;
        do {
            oldLoc = h.__dj_class_manager.owning_machine;
            if(oldLoc == -1 || (h.__dj_class_mode & CONSTS.IS_NOT_MASTER) == 0)
                return; // this is the master machine, so we should not update it
        } while(!unsafe.compareAndSwapInt(h.__dj_class_manager, ClassManager.class_manager_owning_machine_offset,
                oldLoc, machine_location));
        //InternalInterface.debug("successful update of object location: "+id+" to "+machine_location);
    }

    static public void sendUpdateObjectLocation(UUID id, int machine_location, int to) {
        if(to == InternalInterface.getInternalInterface().getSelfId()) {
            // this is a waste
//            throw new RuntimeException();
            return;
        }
        if(to == machine_location) {
            // if the remote machine truely owns it then it must be getting deserialized or
            // changed, so we don't want to interfear
//            InternalInterface.debug("trying to update object location to self machine");
            return;
        }
        assert(machine_location != -1);
        InternalInterface.getInternalInterface().updateObjectLocation(id, machine_location, to);
    }

    static Object lastReadLoop = null;
    static int lastReadLoopCnt = 0;

    // this is receiving the remote reads on the network, so it is already on the slow path
    static public ByteBuffer readField(int op, int from, ByteBuffer req) {
        UUID id = new UUID(req.getLong(), req.getLong());
        int fid = req.getInt();
        ObjectBase h = getLocalObject(id);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
        if(h == null)
            throw new InterfaceException();
        int mode;
        if(((mode = h.__dj_class_mode) & CONSTS.REMOTE_READS) != 0) {
            // need to redirect the request elsewhere

            assert(h.__dj_class_manager.distributedObjectId.equals(id));

            // TODO: update the location from the machine that made the request
            // already exists the code for recving the update
            int owner = h.__dj_class_manager.owning_machine;
            int selfid = InternalInterface.getInternalInterface().getSelfId();
            if(owner ==  selfid || owner == -1) {
                InternalInterface.debug("trying to resolve machine to self");
            }
            if(h == lastReadLoop) {
                InternalInterface.debug("in loop "+id+" "+mode+" "+selfid);
                printState(h);
                if(lastReadLoopCnt == 0 || lastReadLoopCnt > 5)
                    InternalInterface.getInternalInterface().relocateObject(id);
                try { Thread.sleep(10 << lastReadLoopCnt); } catch(InterruptedException e) {}
                lastReadLoopCnt++;
                if(lastReadLoopCnt > 7)
                    lastReadLoopCnt = 7;
            } else {
                lastReadLoopCnt = 0;
                // try alert the other machine able the new location
                if(owner == -1) {
                    if(from != selfid)
                        sendUpdateObjectLocation(id, selfid, from);
                } else
                    sendUpdateObjectLocation(id, owner, from);
            }
            lastReadLoop = h;
            if(owner != -1) // check twice
                throw new NetworkForwardRequest(owner);
            if((h.__dj_class_mode & CONSTS.REMOTE_READS) != 0)
                throw new DJError(); // invalid state, idk what to do
        }
        ByteBuffer ret = readFieldSwitch(h, op, fid);
        if((h.__dj_class_mode & CONSTS.REMOTE_READS) != 0) {
            // the fact that this has changed since we performed the last operation, means that if we redirect this
            // request at this very moment, then it is likely to race
            // this is already a network request, and doing it incorrectly could incur 2 extra trips
            try { Thread.sleep(2); } catch(InterruptedException e) {}
            int owner = h.__dj_class_manager.owning_machine;
            if(owner != -1)
                throw new NetworkForwardRequest(owner);
            throw new NetworkForwardRequest(InternalInterface.getInternalInterface().getSelfId()); // ga
        }
        JITWrapper.recordReceiveRemoteRead(h, fid, from);
        return ret;

    }

    static public ByteBuffer readFieldSwitch(ObjectBase h, int op, int fid) {
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
            case 18: {// OBJECT
                Object o = h.__dj_readFieldID_A(fid);
                return DistributedObjectHelper.getDistributedId(o).toBB();
            }
            default:
                throw new InterfaceException();
        }
    }

    static Object lastWriteLoop = null;
    static int lastWriteLoopCnt = 0;

    static public void writeField(int op, int from, ByteBuffer req) {
        UUID id = new UUID(req.getLong(), req.getLong());
        int fid = req.getInt();
        ObjectBase h = getLocalObject(id);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
        if(h == null)
            throw new InterfaceException();
        int mode;
        if(((mode = h.__dj_class_mode) & CONSTS.REMOTE_WRITES) != 0) {
            // need to redirect the request elsewhere
//            throw new NotImplementedException();

            assert(h.__dj_class_manager.distributedObjectId.equals(id));

            int owner = h.__dj_class_manager.owning_machine;
            int selfid = InternalInterface.getInternalInterface().getSelfId();
            if(owner ==  selfid || owner == -1) {
                InternalInterface.debug("trying to resolve machine to self "+owner);
            }

            if(h == lastWriteLoop) {
                InternalInterface.debug("In a loop "+id+" "+mode+" "+selfid);
                printState(h);
                if(lastWriteLoopCnt == 0 || lastWriteLoopCnt > 5)
                    InternalInterface.getInternalInterface().relocateObject(id);
//                throw new RuntimeException();
                try { Thread.sleep(10 << lastWriteLoopCnt); } catch(InterruptedException e) {}
                lastWriteLoopCnt++;
                if(lastWriteLoopCnt > 7)
                    lastWriteLoopCnt = 7;
            } else {
                lastWriteLoopCnt = 0;
                if(owner == -1) {
                    if(from != selfid)
                        sendUpdateObjectLocation(id, selfid, from);
                } else
                    sendUpdateObjectLocation(id, owner, from);
            }
            lastWriteLoop = h;
            if(owner != -1)
                throw new NetworkForwardRequest(owner);
            int[] cache = h.__dj_class_manager.cached_copies;
            if(cache != null)
                throw new NotImplementedException();
        }
        writeFieldSwitch(h, req, op, fid);
        if((h.__dj_class_mode & CONSTS.REMOTE_WRITES) != 0) {
            try { Thread.sleep(2); } catch(InterruptedException e) {}
            int owner = h.__dj_class_manager.owning_machine;
            if(owner != -1)
                throw new NetworkForwardRequest(owner);
            throw new NetworkForwardRequest(InternalInterface.getInternalInterface().getSelfId()); // ga
        }
//            throw new DJError();
        JITWrapper.recordReceiveRemoteWrite(h, fid, from);
    }

    static public void writeFieldSwitch(ObjectBase h, ByteBuffer req, int op, int fid) {
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
        int notify_cnt = obj.getInt();
        h = getLocalObject(id);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
            // we are not the master machine here, we should forward this request
            throw new NetworkForwardRequest(h.__dj_class_manager.owning_machine);
//            throw new NotImplementedException();
        }
        if(h.__dj_class_manager.notifications_to_send != -1) {
            if(notify_cnt == -1) {
                h.__dj_class_manager.notifications_to_send = -1;
            } else {
                h.__dj_class_manager.notifications_to_send += notify_cnt;
            }
        }

        assert(h.__dj_class_manager.monitor_lock_count == 1);
        h.__dj_class_manager.addMachineToWaiting(machine);
        h.__dj_class_manager.monitor_lock_count = 0;
        h.__dj_class_manager.processNotifications();
    }

    /*static public void notifyObject(ByteBuffer obj) {
        UUID id = new UUID(obj.getLong(), obj.getLong());
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        // TODO:
    }*/

    static public boolean lockMonitor(ByteBuffer obj, boolean spin) {
        UUID id = new UUID(obj.getLong(), obj.getLong());
        ObjectBase h = getLocalObject(id);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
        if(h == null)
            throw new InterfaceException();
        while(true) {
            synchronized (h) {
                if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0)
                    throw new NetworkForwardRequest(h.__dj_class_manager.owning_machine);
                if(h.__dj_class_manager.monitor_lock_count == 0) {
                    h.__dj_class_manager.monitor_lock_count = 1;
                    return true;
                }
            }
            if(!spin)
                return false;
            try { Thread.sleep(1); } catch (InterruptedException e) {}
        }
//
//        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0)
//            // redirect to the correct machine
//                throw new NetworkForwardRequest(h.__dj_class_manager.owning_machine);
////            throw new NotImplementedException();
//        synchronized (h) {
//            // we have a param for spinning since we do not want to block the io threads with spinning on an object
//            do {
//                synchronized (h) {
//                    if(h.__dj_class_manager.monitor_lock_count != 0)
//                        continue;
//                    h.__dj_class_manager.monitor_lock_count = 1;
//                    return true;
//                }
//            } while(spin);
//            return false;
//        }
    }

    static public void unlockMonitor(ByteBuffer obj) {
        UUID id = new UUID(obj.getLong(), obj.getLong());
        int notify_cnt = obj.getInt();
        ObjectBase h = getLocalObject(id);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0) {
            // objects should get moved when they are locked, so this should not happen
            throw new RuntimeException();
        }
            // redirect to the correct machine
//            throw new NetworkForwardRequest(h.__dj_class_manager.owning_machine);
//            throw new NotImplementedException();
        synchronized (h) {
            assert(h.__dj_class_manager.monitor_lock_count == 1);
            if(h.__dj_class_manager.notifications_to_send != -1) {
                if(notify_cnt == -1) {
                    h.__dj_class_manager.notifications_to_send = -1;
                } else {
                    h.__dj_class_manager.notifications_to_send += notify_cnt;
                }
            }
            h.__dj_class_manager.monitor_lock_count = 0;
            h.__dj_class_manager.processNotifications();
        }
    }

    /*static public void sendNotify(ByteBuffer obj) {
        // this should be the master machine and we are sending a notification
        UUID id = new UUID(obj.getLong(), obj.getLong());
        int count = obj.getInt();
        ObjectBase h;
        synchronized (localDistributedObjects) {
            h = (ObjectBase)localDistributedObjects.get(id);
        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) != 0)
            // redirect to the correct machine
            throw new NotImplementedException();
        h.__dj_class_manager.(count);
    }*/

    static public void recvNotify(ByteBuffer obj) {
        // the master is sending us a notification for some object
        UUID id = new UUID(obj.getLong(), obj.getLong());
        ObjectBase h = getLocalObject(id);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
        if(h == null)
            throw new InterfaceException();
        if((h.__dj_class_mode & CONSTS.IS_NOT_MASTER) == 0)
            // these commands should not be sent to the master
            throw new RuntimeException(); // somehow someone else is sending us a notification
        // TODO: check that there are actually threads still waiting on this object
        // otherwise we should raise some exception/redirect this notification request
        // as if we don't end up waking an object then that means a notification was loss by the system
        synchronized (h) {
            h.notify();
        }
    }


    static public void moveObject(ObjectBase obj, int to) {
        DistributedObjectId id = getDistributedId(obj);
        if(obj.__dj_class_manager.owning_machine == to || id.isFinalObj()) {
            // object is already on desired machine or is final
            return;
        }
        if(!obj.__dj_class_manager.isLocal() /*owning_machine != -1*/) {
            // we don't own this object, send a message to the owning machine to move it
            int owner = obj.__dj_class_manager.owning_machine;
            byte[] ida = id.toArr();
            ByteBuffer b = ByteBuffer.allocate(ida.length + 4);
            b.putInt(to);
            b.put(ida);
            if(owner != -1)
                InternalInterface.getInternalInterface().sendMoveObject(b, owner);
        } else {
            if(to == InternalInterface.getInternalInterface().getSelfId()) {
                // already on desired machine
                return;
            }
            // serialize the object, and then send it to the new machine
//            InternalInterface.debug("before send obj "+id);
            try {
                preSendObj++;
                ByteBuffer so = SerializeManager.serialize(obj, new SerializeManager.SerializationController() {
                    @Override
                    public SerializeManager.SerializationAction getAction(Object o) {
                        if (o == obj)
                            return SerializeManager.SerializationAction.TRY_MOVE_OBJ_MASTER;
                        else
                            return SerializeManager.SerializationAction.MAKE_REFERENCE;
                        //return null;
                    }
                }, 1, to);
                // we could have a zero byte object, then no need to try and move it
                if(so.limit() != 0) {
                    InternalInterface.getInternalInterface().sendSerializedObject(so, to);
                    lastMovedSend = obj;
                    lastBufferSend = so;
                    //sendObjsL.add(obj);
                    sendObjs++;
                    obj.__dj_class_mode |= CONSTS.SERIALIZED_OBJ_SENT;
                }
            } catch(Throwable e) {
                e.printStackTrace();
                throw e;
            }
//            InternalInterface.debug("send moved obj "+id);
            //throw new NotImplementedException();
        }
    }

    static public void moveObjectFieldRef(ObjectBase obj, int field, int target) {
        Object fieldVal;
        try {
            fieldVal = obj.__dj_readFieldID_A(target);
        } catch (DJError e) {
            // then this field does not exist as an object type
            // and trying to move it is meaning less
            return;
        }
        int mode = obj.__dj_class_mode;
        if((mode & CONSTS.REMOTE_READS) != 0) {
            // the object that we have found is not the one that we are looking for and we should redirect
            // this operation
            int owner = obj.__dj_class_manager.owning_machine;
            if(owner != -1) {
                byte[] arr = getDistributedId(obj).toArr();
                ByteBuffer buf = ByteBuffer.allocate(arr.length + 8);
                buf.putInt(target);
                buf.putInt(field);
                buf.put(arr);
                InternalInterface.getInternalInterface().moveObjectFieldRef(owner, buf);
            }
        }
        if(fieldVal instanceof ObjectBase)
            moveObject((ObjectBase)fieldVal, target);
    }

    static int sendObjs = 0;
    static int recvObjs = 0;
    static int finRecvObjs = 0;
    static int preSendObj = 0;
    static int recvFinal = 0;


    static Object lastMovedSend = null;
    static ByteBuffer lastBufferSend = null;
    static Object lastMovedRecv = null;
    static ByteBuffer lastBufferRecv = null;
    static LinkedList<Object> sendObjsL = new LinkedList<Object>();
    static LinkedList<Object> recvObjsL = new LinkedList<Object>();

    static public void printState(Object look) {
        /*
        InternalInterface.debug("DOH state: "+preSendObj+" "+sendObjs+" "+recvObjs+" "+finRecvObjs+" "+recvFinal);
        if(lastMovedSend != null)
            InternalInterface.debug("last move send:"+((ObjectBase)lastMovedSend).__dj_class_manager.distributedObjectId);
        if(lastMovedRecv != null)
            InternalInterface.debug("last move recv:"+((ObjectBase)lastMovedRecv).__dj_class_manager.distributedObjectId);

        LinkedList<Object> ll;
        if(sendObjsL.size() > recvObjsL.size())
            ll = sendObjsL;
        else
            ll = recvObjsL;

        String s = "";
        int i = ll.size() - 15;
        if(i < 0) i =0;
        for(; i < ll.size(); i++) {
            s += ((ObjectBase)ll.get(i)).__dj_class_manager.distributedObjectId.toString() + " ";
        }
        InternalInterface.debug("objs: "+s);

        for(i = 0; i < ll.size(); i++) {
            if(ll.get(i) == look)
                InternalInterface.debug("found object at: "+i);
        }
        */
    }

    static public void recvMoveReq(ByteBuffer req) {
        int to = req.getInt();
        DistributedObjectId id = new DistributedObjectId(req);
        ObjectBase h = getLocalObject(id.identifier);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id.identifier);
//        }
        if(h == null) {
            // there is something wrong since we should have owned this object??
            // the object could have received another command already to move it?
            //throw new RuntimeException();
            try { Thread.sleep(500); } catch(InterruptedException e) {}
            InternalInterface.debug("failed to locate object to move: "+id);
        } else {
            if(h.__dj_class_manager.isLocal())
                moveObject(h, to);
            // ignore otherwise
        }
    }

    static void recvMoveFiedReq(ByteBuffer req) {
        int to = req.getInt();
        int field = req.getInt();
        DistributedObjectId id = new DistributedObjectId(req);
        ObjectBase h = getLocalObject(id.identifier);
        if(h == null) {
            // failed to find the object that we are looking for
            return;
        }
        Object val;
        try {
            val = h.__dj_readFieldID_A(field);
        } catch (DJError e) {
            // could not find the field to read
            return;
        }
        int mode = h.__dj_class_mode;
        if((mode & CONSTS.REMOTE_READS) == 0 && val instanceof ObjectBase) {
            // the field read is valid, so we can try and move that object now
            moveObject((ObjectBase)val, to);
        }
        // don't try and redirect, since by this point the system likely has
        // already submitted another move request for this object
        // since it has already had the request redirected once
    }

    static Object recvLock = new Object();

    static public void recvMovedObject(ByteBuffer buf) {
        // calling on the recving machine for make the
        //synchronized (recvLock) {
            try {
                recvObjs++;
//            try { Thread.sleep(10); } catch(InterruptedException e) {}
                ObjectBase obj = (ObjectBase) SerializeManager.deserialize(buf);
                lastMovedRecv = obj;
                lastBufferRecv = buf;
                //recvObjsL.add(obj);
                finRecvObjs++;
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            } finally {
                recvFinal++;
            }
        //}
//        InternalInterface.debug("recv moved obj  "+getDistributedId(obj));
//        UUID id = new UUID(buf.getLong(), buf.getLong());
//        ObjectBase h;
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
//        if(h == null) {
//            // we have to construct a new instance of this object
//        }
    }


    static public void changeReferenceCount(ByteBuffer buf) {
        UUID id = new UUID(buf.getLong(), buf.getLong());
        int delta = buf.getInt();
        ObjectBase h = getLocalObject(id);
//        synchronized (localDistributedObjects) {
//            h = (ObjectBase)localDistributedObjects.get(id);
//        }
        if(h == null)
            throw new InterfaceException();

        if(!h.__dj_class_manager.isLocal()) {
            int owner = h.__dj_class_manager.owning_machine;
            if(owner != -1)
                throw new NetworkForwardRequest(owner);
        }

        int cref;
        int nval;
        do {
            cref = h.__dj_class_manager.reference_count;
            nval = cref + delta;
        } while(!unsafe.compareAndSwapInt(h.__dj_class_manager, ClassManager.class_manager_ref_field_offset, cref, nval));
        if(nval < 0) {
            throw new DJError();
        }
        if(nval == 0) {
            // then we no longer need the reference to this object
            InternalInterface.debug("should delete this object: "+id);
        }
    }

    static private void manageGCQueue() {
        while(true) {
            try {
                ClassManager ref = (ClassManager)gcRefQueue.remove();
                if(ref.owning_machine != -1) // this means that it is master?
                    InternalInterface.getInternalInterface().changeReferenceCount(ref.distributedObjectId, -1, ref.owning_machine);
            } catch(InterruptedException e) {}
        }
    }

    static public void makeCacheOfObject(ObjectBase self, int where) {
        int mode = self.__dj_class_mode;
        if(where == InternalInterface.getInternalInterface().getSelfId()) {
            // this is from self
            if((mode & CONSTS.REMOTE_READS) == 0) {
                // then we must already have a cache of this object
                return;
            }
            byte[] arr = getDistributedId(self).toArr();
            ByteBuffer buf = ByteBuffer.allocate(arr.length + 4);
            buf.putInt(where);
            buf.put(arr);
            throw new NotImplementedException();
            // return;
        } else {
            if((mode & CONSTS.IS_NOT_MASTER) != 0) {
                // then we need to redirect this request
                throw new NotImplementedException();
            }
            ByteBuffer so = SerializeManager.serialize(self, new SerializeManager.SerializationController() {
                @Override
                public SerializeManager.SerializationAction getAction(Object o) {
                    if (o == self)
                        return SerializeManager.SerializationAction.MAKE_OBJ_CACHE;
                    else
                        return SerializeManager.SerializationAction.MAKE_REFERENCE;
                    //return null;
                }
            }, 1, where);
            if(so.limit() != 0) {
                InternalInterface.getInternalInterface().sendSerializedObject(so, where);
                lastMovedSend = self;
                lastBufferSend = so;
                //sendObjsL.add(obj);
                sendObjs++;
                self.__dj_class_mode |= CONSTS.SERIALIZED_OBJ_SENT;
            }

//            int[] ncache;
//            int[] ocache;
//            boolean done_setting = false;
//            do {
//                ocache = self.__dj_class_manager.cached_copies;
//                if(ocache == null) {
//                    ncache = new int[]{where};
//                } else {
//                    ncache = new int[ocache.length + 1];
//                    for(int i = 0; i < ocache.length; i++) {
//                        // this machine is already listed in the cache list
//                        if(ocache[i] == where) {
//                            done_setting = true;
//                        }
//                        ncache[i] = ocache[i];
//                    }
//                    ncache[ocache.length] = where;
//                }
//            } while(!done_setting && !unsafe.compareAndSwapObject(self.__dj_class_manager,
//                    ClassManager.class_manager_cache_copies_offset, ocache, ncache));
//            if(!done_setting) {
//                int mode2 = self.__dj_class_mode;
//                if ((mode2 & CONSTS.IS_NOT_MASTER) != 0) {
//                    // this machine is no longer the master
//                    // so we need to redirect the request
//                }
//            }
//            // at this point we just need to send the content of this object
        }

    }

    static void recvMakeCacheObject(ByteBuffer buf) {
        int where = buf.getInt();
        DistributedObjectId id = new DistributedObjectId(buf);
        ObjectBase obj = getLocalObject(id.identifier);
        makeCacheOfObject(obj, where);
    }

    static public void removeCache(ObjectBase obj, int where) {
        int mode = obj.__dj_class_mode;
        if(where == InternalInterface.getInternalInterface().getSelfId()) {
            // then we are removing it from self
            if((mode & CONSTS.IS_NOT_MASTER) == 0)
                // this machine is currently the master
                return;
            // should use a compare and swap for the mode here
            int m = obj.__dj_class_mode;
            m &= ~(CONSTS.IS_CACHED_COPY);
            m |= CONSTS.REMOTE_READS | CONSTS.IS_PROXY_OBJ;
            obj.__dj_class_mode = m;
            unsafe.fullFence();
            obj.__dj_empty_obj();
            DistributedObjectId id = getDistributedId(obj);
            byte[] ida = id.toArr();
            ByteBuffer buf = ByteBuffer.allocate(ida.length + 4);
            buf.putInt(4);
            buf.put(ida);
            InternalInterface.getInternalInterface().sendRemoveCache(id.lastKnownHost, buf);
            return;
        }
        if((mode & CONSTS.IS_NOT_MASTER) == 0) {
            // then we are the master machine
            boolean successful_update = false;
            synchronized (SerializeManager.serializeLock) {
                if((obj.__dj_class_mode & CONSTS.IS_NOT_MASTER) == 0) {
                    int ocache[] = obj.__dj_class_manager.cached_copies;
                    if(ocache == null)
                        throw new DJError();
                    int ncache[] = new int[ocache.length - 1];
                    for(int i = 0, j = 0; i < ocache.length; i++) {
                        if(ocache[i] != where)
                            ncache[j++] = ocache[i];
                    }
                    obj.__dj_class_manager.cached_copies = ncache;
                    successful_update = true;
                }
                // we are no longer the master, and we are not the machine that we are looking for
            }
            if(successful_update) {
                DistributedObjectId id = getDistributedId(obj);
                byte[] ida = id.toArr();
                ByteBuffer buf = ByteBuffer.allocate(ida.length + 4);
                buf.putInt(where);
                buf.put(ida);
                InternalInterface.getInternalInterface().sendRemoveCache(where, buf);
                return;
            }
        }
        // then this is the wrong machine and we should send something to both the master and the client
        DistributedObjectId id = getDistributedId(obj);
        byte[] ida = id.toArr();
        ByteBuffer buf = ByteBuffer.allocate(ida.length + 4);
        buf.putInt(where);
        buf.put(ida);
        InternalInterface.getInternalInterface().sendRemoveCache(where, buf);
        if(where != id.lastKnownHost) {
            InternalInterface.getInternalInterface().sendRemoveCache(id.lastKnownHost, buf);
        }
    }

    static public void recvRemoveCache(ByteBuffer buf) {
        int where = buf.getInt();
        DistributedObjectId id = new DistributedObjectId(buf);
        ObjectBase obj = getLocalObject(id.identifier);
        if(where == InternalInterface.getInternalInterface().getSelfId()) {
            // then we are the one that has the cache of this object
            int mode = obj.__dj_class_mode;
            if((mode & CONSTS.IS_NOT_MASTER) != 0) {
                // then we need to remove the cache from this object
                int m;
                int oldm;
                do {
                    oldm = m = obj.__dj_class_mode;
                    m &= ~(CONSTS.IS_CACHED_COPY);
                    m |= CONSTS.REMOTE_READS | CONSTS.IS_PROXY_OBJ;
//                    obj.__dj_class_mode = m;
                } while(!unsafe.compareAndSwapInt(obj, DistributedObjectHelper.object_base_mode_field_offset, oldm, m));
                unsafe.fullFence();
                obj.__dj_empty_obj();
            }
        } else {
            // then we should be the master of this object
            synchronized (SerializeManager.serializeLock) {
                int mode = obj.__dj_class_mode;
                if((mode & CONSTS.IS_NOT_MASTER) != 0) {
                    // need to forward this to the master
                    int owner = obj.__dj_class_manager.owning_machine;
                    if(owner != -1)
                        throw new NetworkForwardRequest(owner);
                }
                int ocache[] = obj.__dj_class_manager.cached_copies;
                if(ocache.length == 1) {
                    assert(ocache[0] == where);
                    int m;
                    int oldm;
                    do {
                        oldm = m = obj.__dj_class_mode;
                        m &= ~(CONSTS.REMOTE_WRITES);
                    } while(!unsafe.compareAndSwapInt(obj, object_base_mode_field_offset, oldm, m));
                    obj.__dj_class_manager.cached_copies = null;
                }
                int ncache[] = new int[ocache.length - 1];
                for(int i = 0, j = 0; i < ocache.length; i++) {
                    if(ocache[i] != where) {
                        ncache[j++] = ocache[i];
                    }
                }
                obj.__dj_class_manager.cached_copies = ncache;
            }
        }
    }

    static private Object locateObjLock = new Object();

    static void locateObject(ByteBuffer buf) {
        synchronized (locateObjLock) {
            UUID id = new UUID(buf.getLong(), buf.getLong());
            ObjectBase obj = getLocalObject(id);
            if (obj == null) {
//            InternalInterface.debug("Object not located: "+id);
            } else {
                int mode = obj.__dj_class_mode;
                int self = InternalInterface.getInternalInterface().getSelfId();
//            InternalInterface.debug("Object located: "+id+" "+mode + " " +self);
                if ((mode & CONSTS.IS_NOT_MASTER) == 0) {
                    InternalInterface.getInternalInterface().updateObjectLocationAll(id, self);
                }
            }
        }
    }


    static private final Unsafe unsafe = InternalInterface.getInternalInterface().getUnsafe();

    static final int object_base_mode_field_offset;

    static {
        ThreadHelpers.runAsync(new Runnable() {
            @Override
            public void run() {
                DistributedObjectHelper.manageGCQueue();
            }
        });
        int v = -1;
        try {
            v = (int)unsafe.objectFieldOffset(ObjectBase.class.getDeclaredField("__dj_class_mode"));
        } catch(NoSuchFieldException e) {}
        object_base_mode_field_offset = v;

    }


}
