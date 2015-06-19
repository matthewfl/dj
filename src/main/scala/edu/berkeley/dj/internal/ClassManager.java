package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.coreclazz.java.lang.Object00;

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

    public void writeField_Z(int id, boolean v) {}

    public void writeField_C(int id, char v) {}

    public void writeField_B(int id, byte v) {}

    public void writeField_S(int id, short v) {}

    public void writeField_I(int id, int v) {}

    public void writeField_J(int id, long v) {}

    public void writeField_F(int id, float v) {}

    public void writeField_D(int id, double v) {}

    public void writeField_A(int id, Object v) {}

    public boolean readField_Z(int id) { return false; }

    public char readField_C(int id) { return ' '; }

    public byte readField_B(int id) { return 0; }

    public short readField_S(int id) { return 0; }

    public int readField_I(int id) {
        return requestRead(id, 11);
        //return 123;
    }

    public long readField_J(int id) { return 0L; }

    public float readField_F(int id) { return 0.0f; }

    public double readField_D(int id) { return 0.0; }

    public Object readField_A(int id) { return null; }


    private ByteBuffer requestRead(int fid, int op) {
        ByteBuffer bb = ByteBuffer.allocate(20);
        bb.putLong(distributedObjectId.getMostSignificantBits());
        bb.putLong(distributedObjectId.getLeastSignificantBits());
        bb.putInt(fid);
        return InternalInterface.getInternalInterface().readField(bb, op, owning_machine);
    }



    // TODO: need to have a read and write field methods for all primitive types

    // there should be some seralization methods added to the class

}
