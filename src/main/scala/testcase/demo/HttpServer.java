package testcase.demo;

import edu.berkeley.dj.internal.DJIO;
import edu.berkeley.dj.internal.DJIOTargetMachineArgPosition;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by matthewfl
 */
@DJIO
public final class HttpServer {

    private TrueHTTPServer server;

    @DJIOTargetMachineArgPosition(1)
    public HttpServer(int target, int port, SimpleSite.RequestHandler handler) {
        server = new TrueHTTPServer(port, handler);
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

class TrueHTTPServer extends AbstractHandler {

    private Server server;
    private Thread runner;
    SimpleSite.RequestHandler handler;

    TrueHTTPServer(int port, SimpleSite.RequestHandler handler) {
        this.handler = handler;
        server = new Server(port);
        server.setHandler(this);
        runner = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server.start();
                } catch (Exception e) {
                    System.err.println("Exception from the http server");
                    e.printStackTrace();
                }
            }
        });
        runner.start();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {

        SimpleSite.RequestManager manager = handler.newManager(target);

        response.setContentType(manager.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            response.getOutputStream().write(manager.getContent());
        } catch(Exception e) {
            System.err.println("error with writing to stream");
            e.printStackTrace();
        }

        baseRequest.setHandled(true);
    }
}
