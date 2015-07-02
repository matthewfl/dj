package edu.berkeley.dj.internal;

//import edu.berkeley.dj.internal.Object;

import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by matthewfl
 */
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

    public void waitOnObject(byte[] obj, int machine) { throw new InterfaceException(); }

    public void removeWaitOnObject(byte[] obj, int machine) { throw new InterfaceException(); }

    public void acquireObjectMonitor(ByteBuffer obj, int machine) { throw new InterfaceException(); }

    public void releaseObjectMonitor(ByteBuffer obj, int machine) { throw new InterfaceException(); }

    public void typeDistributed(String name) { throw new InterfaceException(); }

    //protected ThreadLocal<Object> currentThread = new ThreadLocal<>();

    /*public Object threadGroup;

    public void setRootThreadGroup(Object tg) {
        threadGroup = tg;
    }

    public Object getRootThreadGroup() {
        return threadGroup;
    }*/

}

class InternalInterfaceWrap extends  InternalInterface {
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
    public void waitOnObject(byte[] obj, int machine) {
        invoke("waitOnObject", new Class[]{byte[].class, int.class}, obj, machine);
    }

    @Override
    public void removeWaitOnObject(byte[] obj, int machine) {
        invoke("removeWaitOnObject", new Class[]{byte[].class, int.class}, obj, machine);
    }

    @Override
    public void acquireObjectMonitor(ByteBuffer obj, int machine) {
        invoke("acquireObjectMonitor", new Class[]{ByteBuffer.class, int.class}, obj, machine);
    }

    @Override
    public void releaseObjectMonitor(ByteBuffer obj, int machine) {
        invoke("releaseObjectMonitor", new Class[]{ByteBuffer.class, int.class}, obj, machine);
    }

    @Override
    public void typeDistributed(String name) {
        invoke("typeDistributed", new Class[]{String.class}, name);
    }

    /*public void printStdout(int i) throws InterfaceException {
        // for use by the print stream
        invoke("printStdout", new Class[]{int.class}, i);
    }*/

    public Object callIn(Integer action, Object[] args) {
        switch(action) {
            case 0:
                // dummy test
                return "this works";
            case 1:
                // callback for creating a new thread
                ThreadHelpers.newThreadCallback(args[0]);
                return null;
            case 2:
                // callback for the existence of a new client
                return null;
            case 3:
                // callin to run a task on this machine as sent by another machine
                // this should be called in a new thread already so we can just start
                DistributedRunner.runRunnable((Integer)args[0], (byte[])args[1]);
                return null;
            case 4:
                // update the location for an object
                DistributedObjectHelper.updateObjectLocation((UUID)args[0], (int)args[1]);
                return null;
            case 5:
                // reading of fields
                return DistributedObjectHelper.readField((int)args[0], (ByteBuffer)args[1]);
            case 6:
                // writing of fields
                DistributedObjectHelper.writeField((int)args[0], (ByteBuffer)args[1]);
                return null;
            case 7:
                DistributedObjectHelper.waitingFrom((int)args[0], (ByteBuffer)args[1]);
                return null;
            case 8:
                return DistributedObjectHelper.lockMonitor((ByteBuffer)args[0], (boolean)args[1]);
            case 9:
                DistributedObjectHelper.unlockMonitor((ByteBuffer)args[0]);
                return null;

        }
        return null;
    }
}