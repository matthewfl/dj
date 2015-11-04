package edu.berkeley.dj.internal;

import java.nio.ByteBuffer;
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

    public int depth_left;

    public void enter() { depth_left--; }

    public void leave() { depth_left++; }

    public boolean shouldStub() {
        return depth_left <= 0;
    }

    public void register_size(int prim_size, int num_objs) {
        // register the size of this object
    }

    public void put_object(Object o) {

    }

    public Object get_object(Object o) {
        // get the current value of this field as it might
        return o;
    }

    public void put_object_head(DistributedObjectHelper.DistributedObjectId id) {
        // the object idea for a the object
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

    static SerializeManager makeDeserialize(ByteBuffer b) {
        return new Deserialization(b);
    }

    static int computeSize(Object base, int depth) {
        ComputeBufSize c = new ComputeBufSize(depth);
        ObjectBase ob = (ObjectBase)base;
        ob.__dj_serialize_obj(c);
        return c.field_size + c.num_objects_heads + c.num_stub_fields;
    }
}


class Deserialization extends SerializeManager {

    private ByteBuffer buff;

    Deserialization(ByteBuffer b) {
        buff = b;
        depth_left = b.getInt();
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
        return o;
    }
}

class Serialization extends SerializeManager {

    private ByteBuffer buff;

    Serialization(ByteBuffer b) {
        buff = b;
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

    public void put_object(Object o) {
        enter();
        try {
            if (shouldStub()) {
                DistributedObjectHelper.DistributedObjectId id = DistributedObjectHelper.getDistributedId(o);
                num_stub_fields ++;
                object_head_size += id.toArr().length;
            } else {
                if(o instanceof ObjectBase) {
                    ObjectBase ob = (ObjectBase)o;
                    ob.__dj_serialize_obj(this);
                } else {
                    // this is a final type, so we depend on the distributed object id
                    object_head_size += DistributedObjectHelper.getDistributedId(o).toArr().length;
                }
            }
        } finally {
            leave();
        }
    }

    public void put_object_head(DistributedObjectHelper.DistributedObjectId id) {
        // the object idea for a the object
        num_objects_heads ++;
        object_head_size += id.toArr().length;
    }

}

