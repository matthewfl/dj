package edu.berkeley.dj.internal;

//import edu.berkeley.dj.internal.Object;

import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by matthewfl
 */
@RewriteAddArrayWrap
public class InternalInterface {

    InternalInterface() {}

    static private InternalInterface ii = null;

    static private boolean isMaster = true;

    static void _setIsClient() {
        isMaster = false;
    }

    static public boolean isMaster() { return isMaster; }

    static void setInternalInterface(Object o) {
        if(!o.getClass().getName().equals("edu.berkeley.dj.rt.RunningInterface"))
            throw new RuntimeException("Invalid class for running interface");
        if(ii != null)
            throw new RuntimeException("Running interface already set");
        ii = new InternalInterfaceWrap(o);

        // TODO: this is wrong, we only want to do this on the main one rather then on the clients as well
        // set the main thread
        ThreadHelpers.init();
    }

    public static InternalInterface getInternalInterface() {
        return ii == null ? new InternalInterface() : ii;
    }

    public String toString() { return "InternalInterface(Empty)"; }

    public String getUUID() throws InterfaceException { return null; }

    public void printStdout(int i) throws InterfaceException {
        System.out.write(i);
    }

    public void printStderr(int i) throws InterfaceException {
        System.err.write(i);
    }

    /*public edu.berkeley.dj.internal.coreclazz.java.lang.Thread getCurrentThread() {
        // maybe just return some uid
    }*/

    public void currentThreadSleep(long millis) {}

    public Unsafe getUnsafe() throws InterfaceException { return null; }

    public void simplePrint(String p) {
        System.out.println(p);
    }

    public String classRenamed(String name) {
        return null;
    }

    public static void debug(String s) {
        System.out.println("Internal interface debug: "+s);
        System.out.flush();
    }

    public static void debug(int i) {
        debug("called into debug method with: "+i);
        int j  = 1 + 1;
        //(new Throwable()).printStackTrace();
    }

    public boolean lock(String name) {
        throw new InterfaceException();
    }

    public void unlock(String name) {
        throw new InterfaceException();
    }

    public void setDistributed(String name, byte[] value) {
        throw new InterfaceException();
    }

    public byte[] getDistributed(String name) {
        throw new InterfaceException();
    }

    public long threadId() {
        // this will be overwritten such that we have a global usable thread id
        return Thread.currentThread().getId();
    }

    public void startThread(Object r) {
        throw new InterfaceException();
    }

    public void exit(int i) {
        throw new InterfaceException();
    }

    public void registerClient() { throw new InterfaceException(); }

    public int getSelfId() { throw new InterfaceException(); }

    public int[] getAllHosts() { throw new InterfaceException(); }

    public void runOnRemote(int id, byte[] arr) { throw new InterfaceException(); }

    public ByteBuffer readField(ByteBuffer req, int op, int machine) { throw new InterfaceException(); }

    public void writeField(ByteBuffer req, int op, int machine) { throw new InterfaceException(); }

    public void waitOnObject(byte[] obj, int machine, int notify_cnt) { throw new InterfaceException(); }

    public void removeWaitOnObject(byte[] obj, int machine) { throw new InterfaceException(); }

    public boolean acquireObjectMonitor(ByteBuffer obj, int machine) { throw new InterfaceException(); }

    public void releaseObjectMonitor(ByteBuffer obj, int machine, int notify_cnt) { throw new InterfaceException(); }

    public void typeDistributed(String name) { throw new InterfaceException(); }

     // for the master to send a notification to a copy of an object
    public void sendNotifyOnObject(byte[] obj, int machine) { throw new InterfaceException(); }

    // send to all machines that there is some update for a static field of an object
    public void staticFieldUpdate(byte[] obj) { throw new InterfaceException(); }

    public byte[] loadStaticFields(String clsname) { throw new InterfaceException(); }

    public boolean checkClassIsLoaded(String clsname) { throw new InterfaceException(); }

    public boolean checkShouldRedirectMethod(String clsname, String id) { throw new InterfaceException(); }

    public ByteBuffer redirectMethod(ByteBuffer req, int machine) { throw new InterfaceException(); }

    public void sendMoveObject(ByteBuffer req, int machine) { throw new InterfaceException(); }

    public void sendSerializedObject(ByteBuffer req, int machine) { throw new InterfaceException(); }

    public int createIOObject(String clsname, String[] argsTyp, Object[] args, byte[] self) { throw new InterfaceException(); }

    public Object callIOMethod(int objectId, String methodName, String[] argsTyp, Object[] args) { throw new InterfaceException(); }

    public int proxyCreateIOObject(int target, ByteBuffer buf) { throw new InterfaceException(); }

    public ByteBuffer proxyCallIOMethod(int target, ByteBuffer buf) { throw new InterfaceException(); }

    public void updateObjectLocation(UUID id, int target, int to) { throw new InterfaceException(); }

    public void changeReferenceCount(UUID id, int delta, int to) { throw new InterfaceException(); }

    public void moveObjectFieldRef(int target, ByteBuffer buf) { throw new InterfaceException(); }

    public void sendMakeCache(int target, ByteBuffer buf) { throw new InterfaceException(); }

    public void sendRemoveCache(int target, ByteBuffer buf) { throw new InterfaceException(); }

    //protected ThreadLocal<Object> currentThread = new ThreadLocal<>();

    /*public Object threadGroup;

    public void setRootThreadGroup(Object tg) {
        threadGroup = tg;
    }

    public Object getRootThreadGroup() {
        return threadGroup;
    }*/

}

final class InternalInterfaceWrap extends InternalInterface {
    // because we are not fully loading the classes from the runtime in
    // it appears that we can't directly call the methods etc,
    // so keep it as an object and use invoke instead
    private Object base;
    private Class cls;


    InternalInterfaceWrap(Object o) {
        base = o;
        cls = base.getClass();

        try {
            invoke("setCallIn", new Class[]{Object.class}, this);
        } catch(InterfaceException e) {
            throw new RuntimeException("setting call in failed");
        }
    }

    private Object invoke(String name, Class[] sig, Object... obj) throws InterfaceException {
        try {
            return cls.getMethod(name, sig).invoke(base, obj);
        } catch(NoSuchMethodException|
                IllegalAccessException|
                InvocationTargetException e) {
            throw new InterfaceException(name, e);
        }
    }


    public String getUUID() throws InterfaceException {
        return (String) invoke("getUUID", new Class[]{});
    }

    @Override
    public Unsafe getUnsafe() throws InterfaceException {
        return (Unsafe) invoke("getUnsafe", new Class[]{});
    }

    @Override
    public String classRenamed(String name) throws InterfaceException {
        return (String) invoke("classRenamed", new Class[]{String.class}, name);
    }

    @Override
    public String toString() {
        try {
            return (String) invoke("toString", new Class[]{});
        } catch(InterfaceException e) {
            return "interface exception";
        }
    }

    @Override
    public boolean lock(String name) {
        return ((Boolean)invoke("lock", new Class[]{String.class}, name)).booleanValue();
    }

    @Override
    public void unlock(String name) {
        invoke("unlock", new Class[]{String.class}, name);
    }

    @Override
    public void setDistributed(String name, byte[] o) {
        invoke("setDistributed", new Class[]{String.class, byte[].class}, name, o);
    }

    @Override
    public byte[] getDistributed(String name) {
        return (byte[])invoke("getDistributed", new Class[]{String.class}, name);
    }

    @Override
    public void startThread(Object r) {
        invoke("startThread", new Class[]{Object.class}, r);
    }

    @Override
    public void exit(int i) {
        invoke("exit", new Class[]{int.class}, i);
    }

    @Override
    public void registerClient() {
        invoke("registerClient", new Class[]{});
    }

    // cache the self id
    private int selfId = -2;

    @Override
    public int getSelfId() {
        if(selfId != -2)
            return selfId;
        return selfId = (int)invoke("getSelfId", new Class[]{});
    }

    @Override
    public int[] getAllHosts() {
        return (int[])invoke("getAllHosts", new Class[]{});
    }

    @Override
    public void runOnRemote(int id, byte[] arr) {
        invoke("runOnRemote", new Class[]{int.class, byte[].class}, id, arr);
    }

    @Override
    public ByteBuffer readField(ByteBuffer req, int op, int to) {
        return (ByteBuffer)invoke("readField", new Class[]{ByteBuffer.class, int.class, int.class}, req, op, to);
    }

    @Override
    public void writeField(ByteBuffer req, int op, int to) {
        invoke("writeField", new Class[]{ByteBuffer.class, int.class, int.class}, req, op, to);
    }

    @Override
    public void waitOnObject(byte[] obj, int machine, int notify_cnt) {
        invoke("waitOnObject", new Class[]{byte[].class, int.class, int.class}, obj, machine, notify_cnt);
    }

    @Override
    public void removeWaitOnObject(byte[] obj, int machine) {
        invoke("removeWaitOnObject", new Class[]{byte[].class, int.class}, obj, machine);
    }

    @Override
    public boolean acquireObjectMonitor(ByteBuffer obj, int machine) {
        return (boolean)invoke("acquireObjectMonitor", new Class[]{ByteBuffer.class, int.class}, obj, machine);
    }

    @Override
    public void releaseObjectMonitor(ByteBuffer obj, int machine, int notify_cnt) {
        invoke("releaseObjectMonitor", new Class[]{ByteBuffer.class, int.class, int.class}, obj, machine, notify_cnt);
    }

    @Override
    public void typeDistributed(String name) {
        invoke("typeDistributed", new Class[]{String.class}, name);
    }

    @Override
    public void sendNotifyOnObject(byte[] obj, int machine) {
        // TODO: ?? have a count on the number of notifications that a client is going to get??
        // that would only be used in the case not notifyAll and then there would have to be more logic on the client
        // for the system to track which item owns the object and then return that to the master once all the threads are
        // done with the notifications.......so lot of extra code and maybe not that much win
        invoke("sendNotifyOnObject", new Class[]{byte[].class, int.class, int.class}, obj, machine, 1);
    }

    @Override
    public void staticFieldUpdate(byte[] update) {
        invoke("staticFieldUpdate", new Class[]{byte[].class}, update);
    }

    @Override
    public byte[] loadStaticFields(String clsname) {
        return (byte[])invoke("loadStaticFields", new Class[]{String.class}, clsname);
    }

    @Override
    public boolean checkClassIsLoaded(String clsname) {
        return (boolean)invoke("checkClassIsLoaded", new Class[]{String.class}, clsname);
    }

    @Override
    public boolean checkShouldRedirectMethod(String clsname, String id) {
        return (boolean)invoke("checkShouldRedirectMethod", new Class[]{String.class,String.class}, clsname, id);
    }

    @Override
    public ByteBuffer redirectMethod(ByteBuffer req, int machine) {
        return (ByteBuffer)invoke("redirectMethod", new Class[]{ByteBuffer.class,int.class}, req, machine);
    }

    @Override
    public void sendMoveObject(ByteBuffer req, int machine) {
        // send a request to move some object
        invoke("sendMoveObject", new Class[]{ByteBuffer.class, int.class}, req, machine);
    }

    @Override
    public void sendSerializedObject(ByteBuffer req, int machine) {
        // send the serialized object information to another machine
        // either for moving or caching an object
        invoke("sendSerializedObject", new Class[]{ByteBuffer.class, int.class}, req, machine);
    }

    @Override
    public int createIOObject(String clsname, String[] argsTyp, Object[] args, byte[] self) {
        return (int) invoke("createIOObject",
                new Class[]{String.class, String[].class, Object[].class, byte[].class},
                clsname, argsTyp, args, self);
    }

    @Override
    public Object callIOMethod(int objectId, String methodName, String[] argsTyp, Object[] args) {
        return invoke("callIOMethod",
                new Class[]{int.class, String.class, String[].class, Object[].class},
                objectId, methodName, argsTyp, args);
    }

    @Override
    public int proxyCreateIOObject(int target, ByteBuffer buf) {
        return (int) invoke("proxyCreateIOObject", new Class[]{int.class, ByteBuffer.class}, target, buf);
    }

    @Override
    public ByteBuffer proxyCallIOMethod(int target, ByteBuffer buf) {
        return (ByteBuffer) invoke("proxyCallIOMethod", new Class[]{int.class, ByteBuffer.class}, target, buf);
    }

    @Override
    public void updateObjectLocation(UUID id, int target, int to) {
        invoke("updateObjectLocation", new Class[]{UUID.class,int.class,int.class}, id, target, to);
    }

    @Override
    public void changeReferenceCount(UUID id, int delta, int to) {
        invoke("changeReferenceCount", new Class[]{UUID.class, int.class, int.class}, id, delta, to);
    }

    @Override
    public void moveObjectFieldRef(int target, ByteBuffer buf) {
        invoke("moveObjectFieldRef", new Class[] {int.class, ByteBuffer.class}, target, buf);
    }

    public void sendMakeCache(int target, ByteBuffer buf) {
        invoke("sendMakeCache", new Class[]{int.class, ByteBuffer.class}, target, buf);
    }

    /*public void printStdout(int i) throws InterfaceException {
        // for use by the print stream
        invoke("printStdout", new Class[]{int.class}, i);
    }*/

    public Object callIn(Integer action, Object[] args) {
        try {
            switch (action) {
                case 0:
                    // dummy test
                    return "this works";
                case 1:
                    // callback for creating a new thread
                    ThreadHelpers.newThreadCallback(args[0]);
                    return null;
                case 2:
                    // callback for the existence of a new client
                    JITWrapper.registerNewClient((int) args[0]);
                    return null;
                case 3:
                    // callin to run a task on this machine as sent by another machine
                    // this should be called in a new thread already so we can just start
                    DistributedRunner.runRunnable((Integer) args[0], (byte[]) args[1]);
                    return null;
                case 4:
                    // update the location for an object
                    DistributedObjectHelper.updateObjectLocation((UUID) args[0], (int) args[1]);
                    return null;
                case 5:
                    // reading of fields
                    return DistributedObjectHelper.readField((int) args[0], (int) args[1], (ByteBuffer) args[2]);
                case 6:
                    // writing of fields
                    DistributedObjectHelper.writeField((int) args[0], (int) args[1], (ByteBuffer) args[2]);
                    return null;
                case 7:
                    DistributedObjectHelper.waitingFrom((int) args[0], (ByteBuffer) args[1]);
                    return null;
                case 8:
                    return DistributedObjectHelper.lockMonitor((ByteBuffer) args[0], (boolean) args[1]);
                case 9:
                    DistributedObjectHelper.unlockMonitor((ByteBuffer) args[0]);
                    return null;
                case 10:
                    DistributedObjectHelper.recvNotify((ByteBuffer) args[0]);
                    return null;
                case 11:
                    return StaticFieldHelper.getAllStaticFields((String) args[0]);
                case 12:
                    // write of static field
                    StaticFieldHelper.recvWriteField((ByteBuffer) args[0]);
                    return null;
                case 13:
                    return RPCHelpers.recvRemoteCall((ByteBuffer) args[0]);
                case 14:
                    DistributedObjectHelper.recvMoveReq((ByteBuffer) args[0]);
                    return null;
                case 15:
                    DistributedObjectHelper.recvMovedObject((ByteBuffer) args[0]);
                    return null;
                case 16:
                    return IOHelper.recvConstructLocalIO((int) args[0], (ByteBuffer) args[1]);
                case 17:
                    return IOHelper.recvCallMethod((int) args[0], (ByteBuffer) args[1]);
                case 18:
                    DistributedObjectHelper.changeReferenceCount((ByteBuffer) args[0]);
                    return null;
                case 19:
                    DistributedObjectHelper.recvMoveFiedReq((ByteBuffer)args[0]);
                    return null;
                case 20:
                    DistributedObjectHelper.recvMakeCacheObject((ByteBuffer)args[0]);
                    return null;
            }
            return null;
        } catch(NetworkForwardRequest e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}