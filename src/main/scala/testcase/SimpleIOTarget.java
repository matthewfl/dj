package testcase;

import edu.berkeley.dj.internal.DJIO;
import edu.berkeley.dj.internal.DJIOTargetMachineArgPosition;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by matthewfl
 */
@DJIO
public final class SimpleIOTarget {

    private String fname;

    private int q;

    private SimpleIOMain.RequestHandler rq;

    @DJIOTargetMachineArgPosition(1)
    public SimpleIOTarget(int target, int q, String fname, SimpleIOMain.RequestHandler rq) { //String fname) {
        //this.fname = fname;
        this.q = q;
        System.out.println("fname: "+fname);
        this.rq = rq;
    }

    public String getContent() {
        return "test";
    }

    public int getInt() { return 123; }

    private JettyHelloWorld server;

    public void startServer() {
        try {
            server = new JettyHelloWorld(this);
        } catch(Exception e) {}
    }

    void reqHandle(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {

    }

    String simpleReq(String path) {
        //return path + "gg";
        return rq.res(path);
    }

}


class JettyHelloWorld extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(this.target.simpleReq(target));

//        response.getWriter().println("<h1>Hello World</h1>");

        baseRequest.setHandled(true);
    }

    private SimpleIOTarget target;
    public Server server;

    public JettyHelloWorld(SimpleIOTarget self) throws Exception {
        target = self;
        server = new Server(8080);
        server.setHandler(this);
        server.start();
        //server.join();
    }

}