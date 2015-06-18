package edu.berkeley.dj.internal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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

    static public <T> Future<T> runOnRemote(int id, Callable<T> r) {
        throw new NotImplementedException();
    }

    static void runRunnable(int from_id, byte[] id) {
        Runnable r = (Runnable)DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(id));
        r.run();
    }




}
