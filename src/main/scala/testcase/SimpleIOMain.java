package testcase;

import edu.berkeley.dj.internal.InternalInterface;


/**
 * Created by matthewfl
 */
public class SimpleIOMain {

    static public void main(String[] args) {

        RequestHandler rq = new RequestHandler();

        SimpleIOTarget t = new SimpleIOTarget(
                InternalInterface.getInternalInterface().getSelfId(), 5,
                "/tmp/test", rq);

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

        public RequestWrapper getNewWrapper() {
            return new RequestWrapper();
        }

        public String res(String path) {
            return path + "gg2";
        }

    }
}



