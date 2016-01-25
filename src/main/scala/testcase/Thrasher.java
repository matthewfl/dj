package testcase;

import edu.berkeley.dj.internal.DistributedRunner;
import edu.berkeley.dj.internal.InternalInterface;
import edu.berkeley.dj.internal.ObjectBase;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by matthewfl
 *
 * Test program for moving data around while it is getting updated
 */
public class Thrasher {

    public static void main(String[] args) throws Exception {
        int num_hosts = 2;

        long rnd = 1;

        int dcnt = 500; //10000;

        // wait until we have enough hosts to start this system
        while(InternalInterface.getInternalInterface().getAllHosts().length < num_hosts) {
            Thread.sleep(1000);
        }

        TData root = new TData();
        root.counts = new int[num_hosts];
        root.children = new TData[1];

        LinkedList<TData> bqueue = new LinkedList<>();
        bqueue.add(root);

        // construct a simi random tree with random sized children
        for(int i = 0; i < dcnt; i++) {
            TData parent = bqueue.poll();
            TData d = new TData();
            d.counts = new int[num_hosts];
            rnd = rnd * 12349798908174L + 11;
            d.children = new TData[Math.abs((int)rnd % 5) + 2];
            bqueue.add(d);
            for(int j = 0; j < parent.children.length; j++) {
                if(parent.children[j] == null) {
                    parent.children[j] = d;
                    if(j + 1 != parent.children.length)
                        bqueue.add(parent);
                    break;
                }
            }
        }

        // now we are going to run tasks on all the nodes which will go through everything and increment the values

        int[] allHosts = InternalInterface.getInternalInterface().getAllHosts();
        Future<Integer> callRes[] = new Future[num_hosts];

        for(int cnt = 0; cnt < 10; cnt++) {
            final int cntf = cnt;
            System.out.println("inc all slots");
            for (int i = 0; i < num_hosts; i++) {
                //if(allHosts[i] != InternalInterface.getInternalInterface().getSelfId()) {
                // start something up on this host
                final int z = i;
                callRes[i] = DistributedRunner.runOnRemote(allHosts[i], new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        try {
                            InternalInterface.getInternalInterface().debug("worker task starting: " + z);
                            incSlot(root, z);
                            return z;
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw e;
                        }
                    }

                    int c = 0;

                    private void incSlot(TData n, int slot) {
                        if (n == null)
                            return;
                        if((++c) % 200 == 0) {
                            InternalInterface.getInternalInterface().debug("worker task "+z+" at "+c);
                        }
                        n.counts[slot]++;
                        for (int j = 0; j < n.children.length; j++) {
                            incSlot(n.children[j], slot);
                        }
                    }
                });
                //}
            }
            System.out.println("waiting on all worker threads");
            for (int i = 0; i < num_hosts; i++) {
                callRes[i].get();
            }
            System.out.println("checking value of all slots");
            for (int i = 0; i < num_hosts; i++) {
                final int v = (i + 1) % num_hosts;
                callRes[i] = DistributedRunner.runOnRemote(allHosts[i], new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        InternalInterface.getInternalInterface().debug("checker task starting: "+v);
                        try {
                            return checkSlot(root, v, cntf + 1);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw e;
                        }
                    }

                    int c = 0;

                    private int checkSlot(TData n, int slot, int val) {
                        if (n == null)
                            return 0;
                        if((c++) % 200 == 0) {
                            InternalInterface.getInternalInterface().debug("checker task "+v+" at "+c);
                        }
                        int r = 0;
                        if (n.counts[slot] != val) {
                            r++;
                            System.out.println("Failed when checking slot " + slot + " on instance: " + n);
                        }
                        for (int j = 0; j < n.children.length; j++) {
                            r += checkSlot(n.children[j], slot, val);
                        }
                        return r;
                    }
                });
            }
            System.out.println("waiting on all checking worker threads");
            int failed = 0;
            for (int i = 0; i < num_hosts; i++) {
                Future<Integer> ff = null;
                Integer vv = null;
                int mode = 0;
                int mode2 = 0;
                try {
                    ff = callRes[i];
                    mode = ((ObjectBase)ff).__dj_class_mode;
                    vv = ff.get();
                    mode2 = ((ObjectBase)ff).__dj_class_mode;
                    failed += vv;
                } catch (java.lang.NullPointerException e) {
                    e.printStackTrace();
                    System.err.println(ff);
                    System.err.println(vv);
                    System.err.println(mode);
                    System.err.println(mode2);
                    throw e;
                }
            }
            System.out.println("done running all checks, failed: " + failed + " "+cnt);
        }
    }

    static class TData {

        int val;

        int [] counts;

        TData[] children;

    }
}
