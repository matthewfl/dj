package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {})
public class ExceptionHelper {

    // on java.lang.Throwable
    public static void printStackTrace(Object self, java.io.PrintStream ps) {
        // need to unwrap the print stream somehow so that can use the typical printing streams?
        ((Throwable)self).printStackTrace(); // just print to typical channel I guess
        InternalInterface.debug("gg print stream");
    }
}
