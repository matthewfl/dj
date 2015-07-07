package edu.berkeley.dj.internal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.*;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {})
public class DistributedRunner {

    private DistributedRunner() {}

    /**
     * Async call to run r on another machine id
     * @param id machine to run on
     * @param r what to run on external machine
     */
    static public void runOnRemote(int id, Runnable r) {
        if(id == -1 || id == InternalInterface.getInternalInterface().getSelfId()) {
            // we want to run this on the local machine
            // so optimize and just run it async
            ThreadHelpers.runAsync(r);
        } else {
            byte[] run_id = DistributedObjectHelper.getDistributedId(r).toArr();
            InternalInterface.getInternalInterface().runOnRemote(id, run_id);
        }
    }

    @RewriteAddAccessorMethods
    @RewriteUseAccessorMethods
    static private class dFutureRunner<T> extends ObjectBase implements Runnable {

        private DistributedFuture<T> f;
        private Callable<T> c;

        dFutureRunner(DistributedFuture<T> f, Callable<T> c) {
            this.f = f;
            this.c = c;
        }

        @Override
        public void run() {
            try {
                f.success(c.call());
            } catch (Throwable e) {
                f.failure(e);
            }
        }
    }

    static public <T> Future<T> runOnRemote(int id, Callable<T> c) {
        DistributedFuture<T> ff = new DistributedFuture<>();

        Runnable ru = new dFutureRunner<T>(ff, c);

        if(id == -1 || id == InternalInterface.getInternalInterface().getSelfId()) {
            // have to run it asnyc
            ThreadHelpers.runAsync(ru);
        } else {
            byte[] run_id = DistributedObjectHelper.getDistributedId(ru).toArr();
            InternalInterface.getInternalInterface().runOnRemote(id, run_id);

        }

        return ff;
    }

    static void runRunnable(int from_id, byte[] id) {
        ThreadHelpers.registerWorkerThread();
        Runnable r = (Runnable)DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(id));
        r.run();
        ThreadHelpers.unregisterWorkerThread();
    }


    @RewriteAddAccessorMethods
    @RewriteUseAccessorMethods
    private static class DistributedFuture<T> extends ObjectBase implements Future<T> {
        T value;
        Throwable err;
        boolean done = false;

        void success(T v) {
            ObjectHelpers.monitorEnter(this);
            try {
                value = v;
                done = true;
                ObjectHelpers.notifyAll(this);
            } finally {
                ObjectHelpers.monitorExit(this);
            }
        }

        void failure(Throwable e) {
            ObjectHelpers.monitorEnter(this);
            try {
                err = e;
                done = true;
                ObjectHelpers.notifyAll(this);
            } finally {
                ObjectHelpers.monitorExit(this);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new NotImplementedException();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return done;
        }

        public T get() throws InterruptedException, ExecutionException {
            ObjectHelpers.monitorEnter(this);
            try {
                if (done) {
                    if (err != null)
                        throw new ExecutionException(err);
                    return value;
                }
                ObjectHelpers.wait(this);
                return get();
            } finally {
                ObjectHelpers.monitorExit(this);
            }
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            ObjectHelpers.monitorEnter(this);
            try {
                if(done) {
                    return get();
                }
                ObjectHelpers.wait(this, unit.toMillis(timeout));
                if (!done) throw new TimeoutException();
            } finally {
                ObjectHelpers.monitorExit(this);
            }
            return get();
        }
    }


}
