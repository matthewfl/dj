package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.java.lang.Thread00;

import java.util.HashMap;

/**
 * Created by matthewfl
 */

@RewriteAllBut(nonModClasses = {"java/lang/Thread", "java/lang/ThreadLocal"})
public class ThreadHelpers {

    private ThreadHelpers() {}

    public static ThreadLocal<Thread00> currentThread = new ThreadLocal<>();

    public static DistributedVariable<HashMap<Long, Thread00>> allThreads = new DistributedVariable<>("DJ_allThreads", new HashMap<>());

    static public void setCurrentThread(Thread00 t) {
        currentThread.set(t);
    }

    static public Thread00 getCurrentThread() {
        return currentThread.get();
    }

    static public void startThread(Runnable r) {
        InternalInterface.getInternalInterface().startThread(r);
    }

    static public void newThreadCallback(Object r) {
        ((Runnable)r).run();
    }


    static public void init() {
        // set the main thread
        try {
            Class.forName("edu.berkeley.dj.internal.coreclazz.java.lang.Thread");
        } catch (ClassNotFoundException e) {}
        /*Thread00 t = new Thread00("DJ main thread");
        allThreads.get().put(1L, t);
        setCurrentThread(t);
    */
    }



}
