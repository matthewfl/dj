package testcase;

import edu.berkeley.dj.internal.InternalInterface;

/**
 * Created by matthewfl
 */
public class SimpleIOMain {

    static public void main(String[] args) {

        SimpleIOTarget t = new SimpleIOTarget(InternalInterface.getInternalInterface().getSelfId(), 5); //"/tmp/test");

        System.out.println(t.getInt());

    }
}
