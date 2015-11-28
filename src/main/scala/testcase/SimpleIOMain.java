package testcase;

import edu.berkeley.dj.internal.InternalInterface;


/**
 * Created by matthewfl
 */
public class SimpleIOMain {

    static public void main(String[] args) {

        SimpleIOTarget t = new SimpleIOTarget(InternalInterface.getInternalInterface().getSelfId(), 5); //"/tmp/test");

        System.out.println(t.getInt());

        t.startServer();

        System.out.println("server started");

        try {
            Thread.sleep(120 * 1000);
        } catch (InterruptedException e) {}

    }

    static class RequestWrapper {



    }

    static class RequestHandler {

        RequestWrapper getNewWrapper() {
            return new RequestWrapper();
        }

    }
}



