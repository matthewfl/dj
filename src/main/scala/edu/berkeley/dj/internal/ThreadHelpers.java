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


}
