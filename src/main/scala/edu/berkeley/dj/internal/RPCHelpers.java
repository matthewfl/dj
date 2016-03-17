package edu.berkeley.dj.internal;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Created by matthewfl
 */
public class RPCHelpers {

    private RPCHelpers() {}

    public static boolean checkPerformRPC(String clsname, String id) {
        return InternalInterface.getInternalInterface().checkShouldRedirectMethod(clsname, id);
    }

    public static Object call_A(Object self, String clsname, String name, String[] params, Object[] args) {
        ByteBuffer b = callRemote(self, clsname, name, params, args);
        return DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(b));
    }

    public static boolean call_Z(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).get() == 1;
    }

    public static char call_C(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).getChar();
    }

    public static byte call_B(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).get();
    }

    public static short call_S(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).getShort();
    }

    public static int call_I(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).getInt();
    }

    public static long call_J(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).getLong();
    }

    public static float call_F(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).getFloat();
    }

    public static double call_D(Object self, String clsname, String name, String[] params, Object[] args) {
        return callRemote(self, clsname, name, params, args).getDouble();
    }

    public static void call_V(Object self, String clsname, String name, String[] params, Object[] args) {
        callRemote(self, clsname, name, params, args);
    }

    // TODO: make this more efficient and avoid so many copies

    public static ByteBuffer callRemote(Object self_, String clsname, String name, String[] params, Object[] args) {
        InternalLogger.countRPC();
        ObjectBase self = (ObjectBase)self_;

        int params_length = 0;
        if(params != null) {
            params_length = params.length;
            assert(params.length == args.length);
        } else {
            assert(args.length == 0);
        }

        byte[][] sendreq = new byte[params_length * 2 + 3][];

        DistributedObjectHelper.DistributedObjectId selfId = DistributedObjectHelper.getDistributedId(self);

        sendreq[0] = selfId.toArr();
        sendreq[1] = clsname.getBytes();
        sendreq[2] = name.getBytes();

        for(int i = 0; i < params_length; i++) {
            DistributedObjectHelper.DistributedObjectId argId = DistributedObjectHelper.getDistributedId(args[i]);
            sendreq[i + 3] = params[i].getBytes();
            sendreq[i + 3 + params.length] = argId.toArr();
        }

        int target_machine = self.__dj_class_manager.owning_machine;
        assert(target_machine != -1);

        ByteBuffer buf = fromArrays(sendreq);

        // send notification to JIT
        JITWrapper.recordRemoteRPC(self, name, target_machine);

        ByteBuffer ret = InternalInterface.getInternalInterface().redirectMethod(buf, target_machine);

        return ret;
    }

    static ByteBuffer recvRemoteCall(ByteBuffer buf) {
        try {
            byte[][] req = fromBB(buf);

            Object self = DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(req[0]));
            String cls_name = new String(req[1]);
            String method_name = new String(req[2]);

            Class<?> cls = Class.forName(cls_name);

            int arg_length = (req.length - 3) / 2;
            Class<?>[] argsTypes = new Class<?>[arg_length];
            Object[] argValues = new Object[arg_length];
            for (int i = 0; i < arg_length; i++) {
                //argsTypes[i] = Class.forName(new String(req[i + 3]));
                argsTypes[i] = AugmentedClassLoader.getClassA(new String(req[i + 3]));
                argValues[i] = DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(req[i + 3 + arg_length]));
            }

            Method mth = cls.getDeclaredMethod(method_name, argsTypes);
            mth.setAccessible(true);
            Object res = mth.invoke(self, argValues);

            Class<?> rtype = mth.getReturnType();
            ByteBuffer ret;
            if (rtype == boolean.class) {
                ret = ByteBuffer.allocate(1);
                if ((boolean) res) {
                    ret.put((byte) 1);
                } else {
                    ret.put((byte) 0);
                }
            } else if (rtype == byte.class) {
                ret = ByteBuffer.allocate(1);
                ret.put((byte) res);
            } else if (rtype == short.class) {
                ret = ByteBuffer.allocate(2);
                ret.putShort((short) res);
            } else if (rtype == char.class) {
                ret = ByteBuffer.allocate(4);
                ret.putChar((char) res);
            } else if (rtype == int.class) {
                ret = ByteBuffer.allocate(4);
                ret.putInt((int) res);
            } else if (rtype == long.class) {
                ret = ByteBuffer.allocate(8);
                ret.putLong((long) res);
            } else if (rtype == float.class) {
                ret = ByteBuffer.allocate(4);
                ret.putFloat((float) res);
            } else if (rtype == double.class) {
                ret = ByteBuffer.allocate(8);
                ret.putDouble((double) res);
            } else {
                ret = DistributedObjectHelper.getDistributedId(res).toBB();
            }
            return ret;

        } catch (ReflectiveOperationException e) {
            throw new InterfaceException("recv rpc", e);
        }
    }

    static ByteBuffer fromArrays(byte[][] arr) {
        int length = 4;
        length += 4 * arr.length;
        for(int i = 0; i < arr.length; i++) {
            length += arr[i].length;
        }
        ByteBuffer ret = ByteBuffer.allocate(length);
        ret.putInt(arr.length);
        for(int i = 0; i < arr.length; i++) {
            ret.putInt(arr[i].length);
        }
        for(int i = 0; i < arr.length; i++) {
            ret.put(arr[i]);
        }
        return ret;
    }

    static byte[][] fromBB(ByteBuffer buf) {
        byte[][] ret = new byte[buf.getInt()][];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = new byte[buf.getInt()];
        }
        for(int i = 0; i < ret.length; i++) {
            buf.get(ret[i]);
        }
        return ret;
    }


    static public void makeMethodRPC(String clsname, String methodSignature) {
        InternalInterface.getInternalInterface().makeMethodRPC(clsname, methodSignature);
        // TODO: set the flags on the instance to enable this method being rpc

//        throw new NotImplementedException();

    }



}
