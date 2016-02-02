package testcase.demo;

import edu.berkeley.dj.internal.InternalInterface;

/**
 * Created by matthewfl
 *
 * Demo of having a simple website that is lisiting on all hosts
 */
public class SimpleSite {

    public static void main(String[] args) {
        int id = InternalInterface.getInternalInterface().getSelfId();
        RequestHandler rh = new RequestHandler(id);
        HttpServer server = new HttpServer(id, 7777, rh);

        // the thing is serving, so just keep this thread alive
        try {
            while(true) {
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {}
    }

    // wrapper that is called back intp
    public static class RequestHandler {

        int id;

        RequestHandler(int id) {
            this.id = id;
        }


        public RequestManager newManager(String path) {
            return new RequestManager(id, path);
        }

    }

    // manage a single request
    public static class RequestManager {

        private FileWrapper fwrap;
        private boolean isDir;
        private String path;

        RequestManager(int machine, String path) {
            fwrap = new FileWrapper(machine, path);
            isDir = fwrap.isDirectory();
            if(path.endsWith("/"))
                this.path = path.substring(0, path.length()-1);
            else
                this.path = path;
        }


        public boolean isIndex() {
            return isDir;
        }

        public String getContentType() {
            if(isIndex()) {
                return "text/html";
            } else {
                // we are going to be returning some rendered image
//                return "image";
                return "text";
            }
        }

        public byte[] getContent() {
            if(isIndex()) {
                // then create a list of all the images
                StringBuilder sb = new StringBuilder();
                sb.append("<ul>\n");
                for(String s : fwrap.list()) {
                    sb.append("<li><a href=\""+path+"/"+s+"\">"+s+"</li>\n");
                }
                sb.append("</ul>");
                return sb.toString().getBytes();
            } else {
                return new String("test123").getBytes();
            }
        }
    }

}
