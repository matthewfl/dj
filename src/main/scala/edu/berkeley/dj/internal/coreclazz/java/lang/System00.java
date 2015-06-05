package edu.berkeley.dj.internal.coreclazz.java.lang;

import edu.berkeley.dj.internal.InterfaceException;
import edu.berkeley.dj.internal.InternalInterface;
import edu.berkeley.dj.internal.RewriteAllBut;

import java.io.*;
import java.nio.channels.Channel;
import java.util.Properties;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {"java/lang/System"})
public class System00 {

    private System00() {}

    public final static PrintStream out = new PrintStream(new OutputStream(){
        @Override
        public void write(int b) throws IOException {
            try {
                InternalInterface.getInternalInterface().printStdout(b);
            } catch(InterfaceException e) {
                throw new IOException("Unable to write to stdout", e);
            }
        }
    }, true);

    public final static PrintStream err = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            try {
                InternalInterface.getInternalInterface().printStderr(b);
            } catch (InterfaceException e) {
                throw new IOException("Unable to write to stderr", e);
            }
        }
    }, true);

    public final static InputStream in = null;

    public static void setIn(InputStream in) {}

    public static void setOut(PrintStream out) {}

    public static void setErr(PrintStream err) {}

    public static Console console() { return null; }

    public static Channel inheritedChannel() throws IOException {
        throw new IOException();
    }

    public static void setSecurityManager(final SecurityManager s) {}

    public static SecurityManager getSecurityManager() {
        // TODO: have some wrapper for the security manager
        return null;
    }

    public static long currentTimeMillis() {
        // we should be able to directly access the normal system class from this context
        return java.lang.System.currentTimeMillis();
    }

    public static long nanoTime() {
        return java.lang.System.nanoTime();
    }

    public static void arraycopy(Object src,  int  srcPos,
                                 Object dest, int destPos,
                                 int length) {
        java.lang.System.arraycopy(src, srcPos, dest, destPos, length);
    }

    public static int identityHashCode(Object x) {
        return java.lang.System.identityHashCode(x);
    }

    public static Properties getProperties() {
        return null;
    }

    public static String lineSeparator() {
        // always a unix system now
        return "\n";
    }

    public static void setProperties(Properties props) { }

    public static String getProperty(String key) {
        // TODO: make this get teh value from the "master machine"
        return System.getProperty(key);

        //return null;
    }

    public static String getProperty(String key, String def) {
        return System.getProperty(key, def);
    }

    public static String setProperty(String key, String value) { return null; }

    public static String clearProperty(String key) { return null; }

    public static String getenv(String name) { return null; }

    public static java.util.Map<String,String> getenv() { return null; }

    public static void exit(int status) {
        // TODO: make this into a rtcall
    }

    public static void gc() { }

    public static void runFinalization() {}

    @Deprecated
    public static void runFinalizersOnExit(boolean value) {}

    public static void load(String filename) { }

    public static void loadLibrary(String libname) {}

    public static String mapLibraryName(String libname) { return null; }


    static {
        //java.lang.System.out.println("internal system inited");
    }


}
