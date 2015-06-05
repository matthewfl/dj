package edu.berkeley.dj.internal;


import edu.berkeley.dj.internal.coreclazz.java.lang.Object00;

import java.util.UUID;

/**
 * Created by matthewfl
 *
 * Represents the backing information for a class that is shared between multiple machines
 *
 */
public class ClassManager {

    protected UUID distribuitedObjectId; // "OB_HEX_UUID"

    // will need a week pointer to the object base
    protected Object00 managedObject;

    protected int getMode() { return  managedObject.__dj_getClassMode(); }

    public void writeField(int id, boolean v) {}

    public void writeField(int id, char v) {}

    public void writeField(int id, byte v) {}

    public void writeField(int id, short v) {}

    public void writeField(int id, int v) {}

    public void writeField(int id, long v) {}

    public void writeField(int id, float v) {}

    public void writeField(int id, double v) {}

    public void writeField(int id, Object v) {}



    // TODO: need to have a read and write field methods for all primitive types

    // there should be some seralization methods added to the class

}
