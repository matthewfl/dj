package testcase.demo;

import edu.berkeley.dj.internal.DJIO;
import edu.berkeley.dj.internal.DJIOTargetMachineArgPosition;
import org.eclipse.jetty.server.Server;

/**
 * Created by matthewfl
 */
@DJIO
public class HttpServer {

    private Server server;
    private Thread runner;

    @DJIOTargetMachineArgPosition(1)
    HttpServer(int target, int port) {
        /*server = new Server(port);
        server.setHandler(this);
        runner = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server.start();
                } catch(Exception e) {
                    System.err.println("Exception from the http server");
                    e.printStackTrace();
                }
            }
        });*/
    }

}
