package testcase;

import edu.berkeley.dj.internal.InternalInterface;

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

        byte[] qq = new byte[5];

        boolean[] vv = new boolean[10];

        qq[0] = (byte)22;

        assert(qq[0] == 22);

        vv[1] = true;
    }

}
