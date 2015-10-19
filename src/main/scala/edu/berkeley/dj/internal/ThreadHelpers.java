package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.java.lang.Thread00DJ;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {"java/lang/Thread", "java/lang/ThreadLocal"})
public class ThreadHelpers {

    private ThreadHelpers() {}

    public static ThreadLocal<Thread00DJ> currentThread = new ThreadLocal<>();

    public static DistributedVariable<HashMap<Long, Thread00DJ>> allThreads = new DistributedVariable<>("DJ_allThreads", new HashMap<>());

    static public void setCurrentThread(Thread00DJ t) {
        currentThread.set(t);
    }

    static public Thread00DJ getCurrentThread() {
        return currentThread.get();
    }

    // start a thread on the current machine
    static public void runAsync(Runnable r) {
        InternalInterface.getInternalInterface().startThread(r);
    }

    // place this thread somewhere on the cluster
    // since this is a thread create this should happen immediately
    static public void runAsyncCluster(Runnable r) {
        int target_machine = JITWrapper.placeThread(r);
        // if this is set to the local machine then this just calls runAsync
        DistributedRunner.runOnRemote(target_machine, r);
    }

    // a task that can be run when ever the cluster scheduler wants
    static public void runTaskCluster(Runnable r) {
        JITWrapper.queueScheduledWork(r);
    }

    static void newThreadCallback(Object r) {
        // The thread's run method is responsible for setting the current running thread
        ((Runnable)r).run();
    }


    static void init() {
        // set the main thread
        try {
            // this is directly calling the method on class, however that should be ok
            // since we have already prefixed the class that we are loading
            Class.forName("edu.berkeley.dj.internal.coreclazz.java.lang.Thread");
        } catch (ClassNotFoundException e) {}
        /*Thread00 t = new Thread00("DJ main thread");
        allThreads.get().put(1L, t);
        setCurrentThread(t);
    */
    }

    public static DistributedVariable<AtomicInteger> nonDaemonThreadCount = new DistributedVariable("DJ_nonDaemonThreadCount", new AtomicInteger());

    static public void incNonDaemon() {
        nonDaemonThreadCount.get().incrementAndGet();
    }

    static public void decNonDaemon() {
        int c = nonDaemonThreadCount.get().decrementAndGet();
        if(c == 0) {
            // Then we should kill this thing
            InternalInterface.debug("----THIS THING SHOULD DIE----");
            InternalInterface.getInternalInterface().exit(0);
        }
    }

    static public void exitThread() {
        Thread00DJ.currentThread().__dj_exit();
    }

    static public void sleep(long u) throws InterruptedException {
        Thread.sleep(u);
    }

    static public void registerWorkerThread() {
        // this is just some task that is running on this now, so we just want to register some worker task
        currentThread.set(new Thread00DJ(0L));
    }

    static public void unregisterWorkerThread() {
        // TODO: have some pool of these items
        currentThread.set(null);
    }



}
