package edu.berkeley.dj.internal;

//import edu.berkeley.dj.internal.Object;

import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;

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
        }
        catch(NoSuchMethodException e) {
            throw new InterfaceException(name);
        }
        catch(IllegalAccessException e) {
            throw new InterfaceException(name);
        }
        catch(InvocationTargetException e) {
            throw new InterfaceException(name);
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
        if(!(o instanceof ObjectBase)) {
            throw new InterfaceException("can not set distributed of non object base");
        }
        invoke("setDistributed", new Class[]{String.class, Object.class}, name, o);
    }

    @Override
    public byte[] getDistributed(String name) {
        return invoke("getDistributed", new Class[]{String.class}, name);
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

    @Override
    public int getSelfId() {
        return (int)invoke("getSelfId", new Class[]{});
    }

    @Override
    public int[] getAllHosts() {
        return (int[])invoke("getAllHosts", new Class[]{});
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

        }
        return null;
    }
}