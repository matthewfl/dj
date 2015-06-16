package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.coreclazz.java.lang.Object00;

import java.util.UUID;

/**
 * Created by matthewfl
 *
 * Represents the backing information for a class that is shared between multiple machines
 *
 */
@RewriteAllBut(nonModClasses = {"java/util/UUID"}) // tmp
public class ClassManager {

    UUID distribuitedObjectId = null; // "OB_HEX_UUID"

    int owning_machine = -1; // signify self

    // will need a week pointer to the object base
    Object00 managedObject;

    ClassManager(Object00 o) {
        managedObject = o;
        distribuitedObjectId = UUID.randomUUID();
    }

    ClassManager(Object00 o, UUID id, int owner) {
        managedObject = o;
        distribuitedObjectId = id;
        owning_machine = owner;
    }

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




    // TODO: need to have a read and write field methods for all primitive types

    // there should be some seralization methods added to the class

}
