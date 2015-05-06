package edu.berkeley.dj.internal.coreclazz.java.lang;

import edu.berkeley.dj.internal.RewriteAllBut;

import java.security.AccessControlContext;
import java.util.Map;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {})
public class Thread implements Runnable {

    /**
     * The minimum priority that a thread can have.
     */
    public final static int MIN_PRIORITY = 1;

    /**
     * The default priority that is assigned to a thread.
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * The maximum priority that a thread can have.
     */
    public final static int MAX_PRIORITY = 10;

    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return the currently executing thread.
     */
    public static Thread currentThread() {
        // TODO:
        return null;
    }

    public static void yield() {
    }

    public static void sleep(long millis) throws InterruptedException {
    }

    public static void sleep(long millis, int nanos)
            throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public Thread() {
    }

    public Thread(Runnable r) {
    }

    public Thread(ThreadGroup group, Runnable target) {
    }


    public Thread(String name) {
    }

    public Thread(ThreadGroup group, String name) {
    }

    public Thread(Runnable target, String name) {
    }

    public Thread(ThreadGroup group, Runnable target, String name) {
    }

    public Thread(ThreadGroup group, Runnable target, String name,
                  long stackSize) {
    }

    public void start() {}

    @Override
    public void run() {
    /*    if (target != null) {
            target.run();
        }*/
    }

    @Deprecated
    public final void stop() {
    }

    @Deprecated
    public final synchronized void stop(Throwable obj) {
    }

    public void interrupt() {
    }

    public static boolean interrupted() {
        return false;
    }

    public boolean isInterrupted() {
        return false;
    }

    @Deprecated
    public void destroy() {
        throw new NoSuchMethodError();
    }


    public final boolean isAlive() { return true; }

    @Deprecated
    public final void suspend() {
    }

    public final void setPriority(int newPriority) {
    }

    public final int getPriority() {
        return 0;
    }

    public final void setName(String name) {
    }

    public final ThreadGroup getThreadGroup() {
        return null;
    }

    public static int activeCount() {
        return 1;
    }

    public static int enumerate(Thread tarray[]) {
        return 0;
    }

    @Deprecated
    public int countStackFrames() { return 1; }

    public final void join(long millis)
            throws InterruptedException {
    }

    public final synchronized void join(long millis, int nanos)
            throws InterruptedException {

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        join(millis);
    }


    public final void join() throws InterruptedException {
        join(0);
    }

    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    public final void setDaemon(boolean on) {
    }

    public final boolean isDaemon() {
        return false;
    }

    public final void checkAccess() {
    }

    public String toString() {
        return "djThread";
    }

    //@CallerSensitive
    public ClassLoader getContextClassLoader() {
        return null;
    }

    public static boolean holdsLock(Object obj) {
        return false;
    }

    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        return null;
    }

    public long getId() {
        return 0;
    }

    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED;
    }

    public State getState() {
        return State.NEW;
    }

    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        /**
         * Method invoked when the given thread terminates due to the
         * given uncaught exception.
         * <p>Any exception thrown by this method will be ignored by the
         * Java Virtual Machine.
         * @param t the thread
         * @param e the exception
         */
        void uncaughtException(Thread t, Throwable e);
    }


    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {

    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return null;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return null;
    }

}
