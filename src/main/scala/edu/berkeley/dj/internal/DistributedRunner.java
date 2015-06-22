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
            ThreadHelpers.startThread(r);
        } else {
            byte[] run_id = DistributedObjectHelper.getDistributedId(r).toArr();
            InternalInterface.getInternalInterface().runOnRemote(id, run_id);
        }
    }

    static public <T> Future<T> runOnRemote(int id, Callable<T> c) {
        DistributedFuture<T> ff = new DistributedFuture<>();

        //ff

        Runnable ru = new Runnable() {
            @Override
            public void run() {
                try {
                    ff.success(c.call());
                } catch (Throwable e) {
                    ff.failure(e);
                }
            }
        };



        if(id == -1 || id == InternalInterface.getInternalInterface().getSelfId()) {
            // have to run it asnyc
            throw new NotImplementedException();
        } else {
            //byte[] run_id = DistributedObjectHelper.getDistributedId(r).toArr();
            //InternalInterface.getInternalInterface()
        }

        throw new NotImplementedException();
    }

    static void runRunnable(int from_id, byte[] id) {
        Runnable r = (Runnable)DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(id));
        r.run();
    }


    private static class DistributedFuture<T> implements Future<T> {
        T value;
        Throwable err;
        boolean done = false;

        void success(T v) {
            value = v;
            done = true;
            notifyAll();
        }

        void failure(Throwable e) {
            err = e;
            done = true;
            notifyAll();
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
            if (done) {
                if (err != null)
                    throw new ExecutionException(err);
                return value;
            }
            // TODO: fix race condition here
            wait();
            return get();
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if(done) {
                return get();
            }
            wait(unit.toMillis(timeout));
            if(!done) throw new TimeoutException();
            return get();
        }
    }


}
