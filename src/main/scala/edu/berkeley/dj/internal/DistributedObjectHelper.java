package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.java.lang.Object00;
import edu.berkeley.dj.internal.coreclazz.sun.misc.Unsafe00;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {"java/util/HashMap", "java/nio/ByteBuffer"})
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

        DistributedObjectId() {}

        DistributedObjectId(UUID u, int h, String cn) {
            this.identifier = u;
            this.lastKnownHost = h;
            this.classname = cn;
        }

        public byte[] toArr() {
            byte[] cn = classname.getBytes();
            ByteBuffer ret = ByteBuffer.allocate(cn.length + 20);
            ret.putInt(lastKnownHost);
            ret.putLong(identifier.getMostSignificantBits());
            ret.putLong(identifier.getLeastSignificantBits());
            ret.put(cn);
            return ret.array();
        }

        public DistributedObjectId(byte[] arr) {
            ByteBuffer b = ByteBuffer.wrap(arr);
            lastKnownHost = b.getInt();
            identifier = new UUID(b.getLong(), b.getLong());
            classname = new String(arr, 20, arr.length - 20);
        }
    }

    static private HashMap<UUID, Object00> localDistribuitedObjects = new HashMap<>();

    // give the Object some uuid so that it will be distribuited
    static public void makeDistribuited(ObjectBase o) {
        if(o.__dj_class_manager == null) {
            o.__dj_class_manager = new ClassManager(o);
            synchronized (localDistribuitedObjects) {
                localDistribuitedObjects.put(o.__dj_class_manager.distribuitedObjectId, o);
            }
        }
    }

    static public DistributedObjectId getDistribuitedId(ObjectBase o) {
        makeDistribuited(o);
        int h = o.__dj_class_manager.owning_machine;
        if(h == -1)
            h = InternalInterface.getInternalInterface().getSelfId();
        return new DistributedObjectId(o.__dj_class_manager.distribuitedObjectId, h, o.getClass().getName());
    }

    static public Object getObject(DistributedObjectId id) {
        synchronized (localDistribuitedObjects) {
            Object00 h = localDistribuitedObjects.get(id.identifier);
            if(h != null)
                return h;
            // we do not have some proxy of this object locally so we need to construct some proxy for it
            try {
                Class<?> cls = AugmentedClassLoader.forName(id.classname);
                ObjectBase obj = (ObjectBase) Unsafe00.getUnsafe().allocateInstance(cls);
                obj.__dj_class_manager = new ClassManager(obj, id.identifier, id.lastKnownHost);
                obj.__dj_class_mode |= CONSTS.OBJECT_INITED | CONSTS.REMOTE_READS | CONSTS.REMOTE_WRITE;
                localDistribuitedObjects.put(id.identifier, obj);
                return obj;
            } catch(ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch(InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
