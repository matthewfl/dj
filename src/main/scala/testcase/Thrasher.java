package testcase;

import edu.berkeley.dj.internal.DistributedRunner;
import edu.berkeley.dj.internal.InternalInterface;

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

        int dcnt = 10000;

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

        int[] allHosts =InternalInterface.getInternalInterface().getAllHosts();
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
                        incSlot(root, z);
                        return z;
                    }

                    private void incSlot(TData n, int slot) {
                        if (n == null)
                            return;
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
                int v = (i + 1) % num_hosts;
                callRes[i] = DistributedRunner.runOnRemote(allHosts[i], new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return checkSlot(root, v, cntf + 1);
                    }

                    private int checkSlot(TData n, int slot, int val) {
                        if (n == null)
                            return 0;
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
                failed += callRes[i].get();
            }
            System.out.println("done running all checks, failed: " + failed);
        }
    }

    static class TData {

        int val;

        int [] counts;

        TData[] children;

    }
}
