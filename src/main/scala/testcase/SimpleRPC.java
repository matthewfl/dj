package testcase;

import edu.berkeley.dj.internal.InternalInterface;
import edu.berkeley.dj.internal.RewriteAsyncCall;

/**
 * Created by matthewfl
 */
public class SimpleRPC {

    public static void main(String[] args) {
        something(10);
        try {
            Thread.sleep(10000);
        } catch(InterruptedException e) {}
    }

    @RewriteAsyncCall
    public static void something(int a) {
        InternalInterface.debug("something async call "+a);
    }

}
