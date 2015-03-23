package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * Represents the backing information for a class that is shared between multiple machines
 *
 */
public class ClassManager {

    protected String DistribuitedObjectId; // "OB_HEX_UUID"

    // will need a week pointer to the object base
    protected ObjectBase ManagedObject;

    protected int getMode() { return ManagedObject.__dj_class_mode; }

    // there should be some seralization methods added to the class

}
