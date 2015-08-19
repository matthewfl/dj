package testcase;

import edu.berkeley.dj.internal.InternalInterface;

import java.util.ArrayList;

/**
 * Created by matthewfl
 */
public class SimpleScratch {

    static byte[] test;

    public static void main(String[] args) throws Throwable {

        //Thread.sleep(1000);



        //System.out.println("hello");

        //Thread.sleep(60000);

        InternalInterface.debug("hello ii");


        test = new byte[6];

        int[] qq = new int[5];

        boolean[] vv = new boolean[10];

        qq[0] = 22;

        int a = qq[0];
        System.out.println("something");

        ArrayList<Integer> ee = new ArrayList<>();
        ee.add(123);

        Object[] oo = new Object[5];
        oo[0] = ee;

        assert(23 == 22);
        //assert(qq[0] == 22);

        //vv[1] = true;
    }

}
