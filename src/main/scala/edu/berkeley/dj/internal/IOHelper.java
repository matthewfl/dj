package edu.berkeley.dj.internal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.nio.ByteBuffer;

/**
 * Created by matthewfl
 */
public class IOHelper {

    @RewriteAddSerialization
    @RewriteAddArrayWrap
    @RewriteAllBut(nonModClasses = {})
    static class MethodParams {
        String[] argsTyp;
        Object[] args;
        String name;
        int id;
        byte[] selfId;
    }

    // these are actual arrays instead of wrapped arrays that can be set accross the network....
    public static int constructLocalIO(int target, String clsname, String[] argsTyp, Object[] args, Object self) {
        // return a int id for this object on the target machine
        if(InternalInterface.getInternalInterface().getSelfId() == target) {
            // then we are going to perform this here
            return InternalInterface.getInternalInterface().createIOObject(clsname, argsTyp, args,
                    DistributedObjectHelper.getDistributedId(self).toArr());
        }

        MethodParams p = new MethodParams();
        p.argsTyp = argsTyp;
        p.args = args;
        p.name = clsname;
        p.id = -1;
        p.selfId = DistributedObjectHelper.getDistributedId(self).toArr();

        throw new NotImplementedException();
//        return -1;
    }


    public static Object callMethod(int target, int id, String methodName, String[] argsTyp, Object[] args, Object self) {
        if(InternalInterface.getInternalInterface().getSelfId() == target) {
            return InternalInterface.getInternalInterface().callIOMethod(id, methodName, argsTyp, args);
        }

        MethodParams p = new MethodParams();
        p.argsTyp = argsTyp;
        p.args = args;
        p.name = methodName;
        p.id = id;
        p.selfId = DistributedObjectHelper.getDistributedId(self).toArr();

        throw new NotImplementedException();

//        return null;
    }

    static ByteBuffer recvConstructLocalIO(int from_machine, ByteBuffer b) {
        MethodParams params = (MethodParams)SerializeManager.deserialize(b);
//        return constructLocalIO(InternalInterface.getInternalInterface().getSelfId(),
//                params.name, params.argsTyp, params.args);

        int i = InternalInterface.getInternalInterface().createIOObject(params.name, params.argsTyp, params.args, params.selfId);
        ByteBuffer ret = ByteBuffer.allocate(4);
        ret.putInt(i);
        return ret;
    }

    static ByteBuffer recvCallMethod(int from_machine, ByteBuffer b) {
        MethodParams params = (MethodParams)SerializeManager.deserialize(b);
        Object r = InternalInterface.getInternalInterface().callIOMethod(params.id, params.name, params.argsTyp, params.args);
        return SerializeManager.serialize(r, SerializeManager.MoveController, 1, from_machine);
    }

}
