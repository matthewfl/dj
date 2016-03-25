package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class ObjectNotLocal extends DJException {

    public final Object obj;
    public final int where;

    public ObjectNotLocal(Object obj, int where) {
        super("Object is not local");
        this.obj = obj;
        this.where = where;
    }
}
