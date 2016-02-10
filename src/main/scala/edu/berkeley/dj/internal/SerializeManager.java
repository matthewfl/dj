package edu.berkeley.dj.internal;

import scala.Array;
import scala.NotImplementedError;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.HashSet;

/**
 * Created by matthewfl
 *
 * This class represent that an object or a set of objects
 * are getting serialize or deserialized
 *
 *
 */
public class SerializeManager {

    static Unsafe unsafe = InternalInterface.getInternalInterface().getUnsafe();

    public int depth_left;

    public void enter() { depth_left--; }

    public void leave() { depth_left++; }

    public boolean shouldStub() {
        return depth_left <= 0;
    }

    public void register_size(int prim_size, int num_objs) {
        // register the size of this object
    }

    public Object put_object(Object o) {
        // return the new value of the object
        // in case we want to null out this field for the gc as we might be emptying this object
        return o;
    }

    public Object get_object(Object o) {
        // get the current value of this field as it might
        return o;
    }

    public void put_object_head(DistributedObjectHelper.DistributedObjectId id) {
        // the object_id for a the object
    }


    // TODO: putter and setter values for primitves
    // maybe make these final so that the jvm can know to inline
    public void put_value_Z(boolean v) {}

    public void put_value_C(char v) {}

    public void put_value_B(byte v) {}

    public void put_value_S(short v) {}

    public void put_value_I(int v) {}

    public void put_value_J(long v) {}

    public void put_value_F(float v) {}

    public void put_value_D(double v) {}

    public boolean get_value_Z() { return false; }

    public char get_value_C() { return 'C'; }

    public byte get_value_B() { return 0; }

    public short get_value_S() { return 0; }

    public int get_value_I() { return 0; }

    public long get_value_J() { return 0; }

    public float get_value_F() { return 0; }

    public double get_value_D() { return 0; }


    SerializeManager () {}

//    //static SerializeManager makeDeserialize(ByteBuffer b) {
//        return new Deserialization(b);
//    }

    static Object deserialize(ByteBuffer b) {
        try {
            return new Deserialization(b).run();
        } catch(Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static int computeSize(Object base, int depth) {
        ComputeBufSize c = new ComputeBufSize(depth);
        ObjectBase ob = (ObjectBase)base;
        ob.__dj_serialize_obj(c);
        return c.field_size + c.num_objects_heads + c.num_stub_fields;
    }


    static Object serializeLock = new Object();

    public static ByteBuffer serialize(Object base, SerializationController controller, int depth, int target_machine) {
        synchronized (serializeLock) {
            try {
                ByteBuffer buff = ByteBuffer.allocate(1024 * 1024); // TODO: compute the proper size
                ObjectBase ob = (ObjectBase) base;
                Serialization s = new Serialization(buff, controller, depth, target_machine);

                s.run(ob);
//            InternalInterface.debug("buff size:" + buff.position());
                ByteBuffer ret = ByteBuffer.allocate(buff.position());
                ret.put(buff.array(), 0, buff.position());
                return ret;
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }

        //return buff;
    }

    public enum SerializationAction {
        // will raise an error if the object is currently locked
        MOVE_OBJ_MASTER,
        MOVE_OBJ_MASTER_LEAVE_CACHE,
        MAKE_OBJ_CACHE,
        // will silently fail if there is a problem, no exceptions
        TRY_MOVE_OBJ_MASTER,
        // will block until the object is unlocked
        MOVE_OBJ_BLOCK_TIL_READY,
        // will simply create a remote object reference
        MAKE_REFERENCE,
        // simply computing the size of an object
        COMPUTE_SIZE,
    }

    static final SerializationAction[] SerializationActionList = SerializationAction.values();

    public SerializationAction getCurrentAction() {
        return SerializationAction.COMPUTE_SIZE;
    }

    public interface SerializationController {

        /**
         * @param o Object for what action we should take
         * @return what action to take for this object
         */
        SerializationAction getAction(Object o);

    }

    public static final SerializationController CacheController = new SerializationController() {
        @Override
        public SerializationAction getAction(Object o) {
            return SerializationAction.MAKE_OBJ_CACHE;
        }
    };

    public static final SerializationController MoveController = new SerializationController() {
        @Override
        public SerializationAction getAction(Object o) {
            return SerializationAction.MOVE_OBJ_MASTER;
        }
    };

}



class Deserialization extends SerializeManager {

    private ByteBuffer buff;

    private SerializationAction current_action;

    Deserialization(ByteBuffer b) {
        buff = b;
        //depth_left = b.getInt();
    }

    Object run() {
        boolean first = true;
        Object ret = null;
        while(buff.position() < buff.limit()) {
            int action_i = buff.getInt();
            SerializationAction act = SerializationActionList[action_i];
            current_action = act;
            DistributedObjectHelper.DistributedObjectId id = new DistributedObjectHelper.DistributedObjectId(buff);
            Object o = DistributedObjectHelper.getObject(id);
            if(first) {
                ret = o;
                first = false;
            }
            if(o instanceof ObjectBase) {
                ObjectBase ob = (ObjectBase)o;
                ob.__dj_class_mode |= CONSTS.CURRENTLY_DESERIALIZING;
                assert(ob.__dj_class_manager.distributedObjectId.equals(id.identifier));
                ob.__dj_deserialize_obj(this);
                if(act == SerializationAction.MOVE_OBJ_MASTER || act == SerializationAction.TRY_MOVE_OBJ_MASTER) {
                    ob.__dj_class_manager.dj_deserialize_obj(this, act);
                    int m = ob.__dj_class_mode;
                    if(ob.__dj_class_manager.cached_copies == null) {
                        m &= ~(CONSTS.IS_NOT_MASTER | CONSTS.REMOTE_READS | CONSTS.REMOTE_WRITES | CONSTS.IS_PROXY_OBJ);
                    } else {
                        m &= ~(CONSTS.IS_NOT_MASTER | CONSTS.REMOTE_READS | CONSTS.IS_PROXY_OBJ);
                    }
                    m |= CONSTS.DESERIALIZED_HERE;
                    unsafe.fullFence();
                    ob.__dj_class_mode = m;
                    ob.__dj_class_manager.owning_machine = -1; // signify self
                } else if(act == SerializationAction.MOVE_OBJ_MASTER_LEAVE_CACHE) {
                    ob.__dj_class_manager.dj_deserialize_obj(this, act);
                    //int m = ob.__dj_class_mode;
                    // we know that there must have been a cache left behind, so will still have "remote_writes"
                    int m = ob.__dj_class_mode;
                    m &= ~(CONSTS.IS_NOT_MASTER | CONSTS.REMOTE_READS | CONSTS.IS_PROXY_OBJ);
                    m |= CONSTS.DESERIALIZED_HERE;
                    unsafe.fullFence();
                    ob.__dj_class_mode = m;
                    ob.__dj_class_manager.owning_machine = -1; // signify self
                } else if(act == SerializationAction.MAKE_OBJ_CACHE) {
                    int m = ob.__dj_class_mode;
                    if((m & CONSTS.REMOTE_READS) == 0) {
                        throw new DJError();
                    }
                    m |= CONSTS.IS_CACHED_COPY | CONSTS.DESERIALIZED_HERE;
                    m &= ~(CONSTS.REMOTE_READS | CONSTS.IS_PROXY_OBJ);
                    unsafe.fullFence();
                    ob.__dj_class_mode = m;
                } else if(act == SerializationAction.MAKE_REFERENCE) {
                    // this should never happen since we are just expecting a reference
                    throw new RuntimeException();
                }
                ob.__dj_class_mode &= ~CONSTS.CURRENTLY_DESERIALIZING;
            } else {
                // this should never happen since the heads of an object should always be an objectBase type
                throw new DJError();
            }
        }
        return ret;
    }

    public boolean get_value_Z() { return buff.get() == 1; }

    public char get_value_C() { return buff.getChar(); }

    public byte get_value_B() { return buff.get(); }

    public short get_value_S() { return buff.getShort(); }

    public int get_value_I() { return buff.getInt(); }

    public long get_value_J() { return buff.getLong(); }

    public float get_value_F() { return buff.getFloat(); }

    public double get_value_D() { return buff.getDouble(); }

    public Object get_object(Object o) {
        // TODO:
        DistributedObjectHelper.DistributedObjectId id = new DistributedObjectHelper.DistributedObjectId(buff);
        if(o == null) {
            return DistributedObjectHelper.getObject(id);
        } else {
            if(o instanceof ObjectBase) {
                DistributedObjectHelper.DistributedObjectId cid = DistributedObjectHelper.getDistributedId(o);
                if(!id.equals(cid)) {
                    // we somehow have an inconsistency here between what is set and what this object currently is
                    throw new DJError();
                }
                return o;
            } else {
                if(id.isFinalObj()) {
                    return DistributedObjectHelper.getDistributedId(id);
                } else {
                    // if the object is final then we should have an exact copy of it
                    throw new DJError();
                }
            }
        }
    }

    public SerializationAction getCurrentAction() {
        return current_action;
    }
}

// want to check that actually the same obj, not that they think are the same
// also using the objects hashcode method could invoke remote accesses and be slow
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

class Serialization extends SerializeManager {

    private ByteBuffer buff;

    private HashSet<ObjectHashCodeWrap> seen = new HashSet<>();

    private ArrayDeque<ObjectBase> nextObj = new ArrayDeque<>();

    private SerializationController controller;

    private SerializationAction current_action;

    private int target_machine;

    Serialization(ByteBuffer b, SerializationController controller, int depth, int target_machine) {
        buff = b;
        depth_left = depth;
        this.controller = controller;
        this.target_machine = target_machine;
    }

    // TODO: need a better way to make this not race with the reading and writing threads
    // I suppose that it could first write onto all the objects that are getting moved the new flags
    // and then use a thread yield or something to try ensure that other threads drain, however that still sounds like
    // it would race
    //
    // I suppose that I could perform a check after the read/write is performed
    // if there is now some flag that indicates that the read/write should be remote, then it could perform the redirect
    // maybe it would be better to update the local object write and then check if the object is actually remote
    // that way if it is racing then it would have written the field and also send a remote update to the other machine
    // the field update would eventually be reflected
    //
    // The field update redirect could also use the static indirect field access methods
    // that way the state of the object has change then it would still end up getting redirected correctly

    void run(ObjectBase base) {
        seen.add(new ObjectHashCodeWrap(base));
        nextObj.add(base);
        for(int i = 0; i < depth_left; i++) {
            ArrayDeque<ObjectBase> objs = nextObj;
            nextObj = new ArrayDeque<>();
            for(ObjectBase o : objs) {
                DistributedObjectHelper.DistributedObjectId id = DistributedObjectHelper.getDistributedId(o);
                SerializationAction act = controller.getAction(o);
                if(!o.__dj_class_manager.isLocal()) {
                    if(act != SerializationAction.MAKE_REFERENCE && act != SerializationAction.TRY_MOVE_OBJ_MASTER)
                        throw new SerializeException("object is not local, can't serialize", o);
                    continue;
                }
                o.__dj_class_mode |= CONSTS.CURRENTLY_SERIALIZING;
                current_action = act;
                buff.putInt(act.ordinal());
                unsafe.fullFence();
                // TODO: locking or something in here
                if(act == SerializationAction.MOVE_OBJ_BLOCK_TIL_READY) {
                    while(true) {
                        synchronized (o) {
                            if(o.__dj_class_manager.monitor_lock_count == 0) {
                                // the object is ready to be serialized
                                unsafe.loadFence();
                                int m;
                                int oldm;
                                do {
                                    oldm = m = o.__dj_class_mode;
                                    m |= CONSTS.IS_NOT_MASTER | CONSTS.REMOTE_READS | CONSTS.REMOTE_WRITES | CONSTS.SERIALIZED_HERE;
                                    InternalInterface.debug("moved blocked:"+id);
                                    o.__dj_class_manager.owning_machine = target_machine;
                                    //if(o.__dj_class_manager.monitor_lock_count)
//                                    o.__dj_class_mode = m;
                                } while(!unsafe.compareAndSwapInt(o, DistributedObjectHelper.object_base_mode_field_offset, oldm, m));

                                unsafe.fullFence();
                                o.__dj_serialize_obj(this);
                                o.__dj_class_manager.dj_serialize_obj(this, act);
                                // indicates that the object should be empty, so we write it after we have performed the
                                // serialization step otherwise we may null out a field before we get to serialize it
                                o.__dj_class_mode |= CONSTS.IS_PROXY_OBJ;
                                unsafe.storeFence();
                                break;
                            }
                        }
                        try { Thread.sleep(2); } catch (InterruptedException e) {}
                    }
                    o.__dj_empty_obj();
                } else if(act == SerializationAction.MOVE_OBJ_MASTER || act == SerializationAction.TRY_MOVE_OBJ_MASTER) {
                    synchronized (o) {
                        int lock_cnt;// = o.__dj_class_manager.monitor_lock_count;
                        if((lock_cnt = o.__dj_class_manager.monitor_lock_count) != 0) {
                            if(act != SerializationAction.TRY_MOVE_OBJ_MASTER)
                                throw new SerializeException("Object is currently locked", o);
                            else {
                                buff.position(buff.position() - 4); // remove the act ind that was added to buff
                                o.__dj_class_mode |= CONSTS.WAS_LOCKED;
                                continue;
                            }
                        }
                        unsafe.loadFence();
                        int m;
                        int oldm;
                        do {
                            oldm = m = o.__dj_class_mode;
                            m |= CONSTS.IS_NOT_MASTER | CONSTS.REMOTE_READS | CONSTS.REMOTE_WRITES | CONSTS.SERIALIZED_HERE;
//                        InternalInterface.debug("moved throw:"+id);
                            o.__dj_class_manager.owning_machine = target_machine;
                            //if(o.__dj_class_manager.monitor_lock_count)
//                            o.__dj_class_mode = m;
                        } while(!unsafe.compareAndSwapInt(o, DistributedObjectHelper.object_base_mode_field_offset, oldm, m));
                        unsafe.fullFence();
                        o.__dj_serialize_obj(this);
                        o.__dj_class_manager.dj_serialize_obj(this, act);
                        o.__dj_class_mode |= CONSTS.IS_PROXY_OBJ;
                        unsafe.storeFence();
                    }
                    o.__dj_empty_obj();
                } else if(act == SerializationAction.MOVE_OBJ_MASTER_LEAVE_CACHE) {
                    synchronized (o) {
                        if(o.__dj_class_manager.monitor_lock_count != 0) {
                            throw new SerializeException("Object is currently locked", o);
                        }
                        if (o.__dj_class_manager.cached_copies == null) {
                            o.__dj_class_manager.cached_copies = new int[]{InternalInterface.getInternalInterface().getSelfId()};
                        } else {
                            throw new NotImplementedError();
                        }
                        int m;
                        int oldm;
                        do {
                            oldm = m = o.__dj_class_mode;
                            m |= CONSTS.IS_NOT_MASTER | CONSTS.IS_CACHED_COPY | CONSTS.REMOTE_WRITES | CONSTS.SERIALIZED_HERE;
                            InternalInterface.debug("move leave cache: "+id);
                            o.__dj_class_manager.owning_machine = target_machine;
//                            o.__dj_class_mode = m;
                        } while(!unsafe.compareAndSwapInt(o, DistributedObjectHelper.object_base_mode_field_offset, oldm, m));
                        unsafe.fullFence();
                        o.__dj_serialize_obj(this);
                        o.__dj_class_manager.dj_serialize_obj(this, act);
                    }
                } else if(act == SerializationAction.MAKE_OBJ_CACHE) {
                    if(o.__dj_class_manager.cached_copies == null) {
                        o.__dj_class_manager.cached_copies = new int[] {target_machine};
                        int m;
                        int oldm;
                        do {
                            oldm = m = o.__dj_class_mode;
                            m |= CONSTS.REMOTE_WRITES | CONSTS.SERIALIZED_HERE;
//                            o.__dj_class_mode = m;
                        } while(!unsafe.compareAndSwapInt(o, DistributedObjectHelper.object_base_mode_field_offset, oldm, m));
                    } else {
                        int[] na = new int[o.__dj_class_manager.cached_copies.length];
                        Array.copy(o.__dj_class_manager.cached_copies, 0, na, 0, o.__dj_class_manager.cached_copies.length);
                        na[na.length - 1] = target_machine;
                        o.__dj_class_manager.cached_copies = na;
                    }
                    unsafe.fullFence();
                    o.__dj_serialize_obj(this);
                    InternalInterface.debug("make cache: "+id);
                } else if(act == SerializationAction.MAKE_REFERENCE) {
                    //o.__dj_serialize_obj(this);
                } else {
                    throw new DJError();
                }
                o.__dj_class_mode &= ~CONSTS.CURRENTLY_SERIALIZING;
            }
        }
    }

    public Object put_object(Object o) {
        // there is going to be a fair amount of duplication here since the D_ID will contain the classes names
        // multiple times
        DistributedObjectHelper.DistributedObjectId id = DistributedObjectHelper.getDistributedId(o);
        buff.put(id.toArr());
        ObjectHashCodeWrap ohw = new ObjectHashCodeWrap(o);
        if(!seen.contains(ohw)) {
            seen.add(ohw);
            if(o instanceof ObjectBase)
                nextObj.add((ObjectBase)o);
        }
//        if(current_action == SerializationAction.MOVE_OBJ_MASTER ||
//                current_action == SerializationAction.MOVE_OBJ_BLOCK_TIL_READY) {
//            // we are emptying out this object and moving the master elsewhere
//            return null;
//        }
        return o;
    }

    public void put_object_head(DistributedObjectHelper.DistributedObjectId id) {
        // the object idea for a the object
        buff.put(id.toArr());
    }

    public void put_value_Z(boolean v) {
        if (v)
            buff.put((byte)1);
        else
            buff.put((byte)0);
    }

    public void put_value_C(char v) {
        buff.putChar(v);
    }

    public void put_value_B(byte v) {
        buff.put(v);
    }

    public void put_value_S(short v) {
        buff.putShort(v);
    }

    public void put_value_I(int v) {
        buff.putInt(v);
    }

    public void put_value_J(long v) {
        buff.putLong(v);
    }

    public void put_value_F(float v) {
        buff.putFloat(v);
    }

    public void put_value_D(double v) {
        buff.putDouble(v);
    }

    public SerializationAction getCurrentAction() {
        return current_action;
    }
}


class ComputeBufSize extends SerializeManager {

    private HashSet<Object> seen = new HashSet<>();

    int field_size = 0;

    int num_stub_fields = 0;

    int num_objects_heads = 0;

    int object_head_size = 0; // size of the object heads since these can be variable due to the name length differences

    ComputeBufSize(int depth) {
        depth_left = depth;
    }

    public void register_size(int prim_size, int num_objs) {
        field_size += prim_size;
    }

    public Object put_object(Object o) {
        enter();
        // we will always just place the object id header at this point
        // the the objects are just placed flat with their headers at the top
        try {
            DistributedObjectHelper.DistributedObjectId id = DistributedObjectHelper.getDistributedId(o);
            field_size += id.toArr().length;
            if (shouldStub()) {
                num_stub_fields++;
                //num_objects_heads ++;
            } else {
                if (o instanceof ObjectBase) {
                    if (!seen.contains(o)) {
                        seen.add(o);
                        ObjectBase ob = (ObjectBase) o;
                        // TODO: get the action for this object
                        ob.__dj_serialize_obj(this);

                    }
                } else {
                    // this is a final type, so we depend on the distributed object id
                    //object_head_size += DistributedObjectHelper.getDistributedId(o).toArr().length;
                }
            }
        } finally {
            leave();
        }
        return o;
    }

    public void put_object_head(DistributedObjectHelper.DistributedObjectId id) {
        // the object idea for a the object
        num_objects_heads ++;
        object_head_size += id.toArr().length;
    }

    public SerializationAction getCurrentAction() {
        return SerializationAction.COMPUTE_SIZE;
    }
}

