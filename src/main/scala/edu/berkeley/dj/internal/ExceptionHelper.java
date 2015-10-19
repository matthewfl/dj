package edu.berkeley.dj.internal;

import edu.berkeley.dj.internal.coreclazz.java.lang.System00DJ;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {})
public class ExceptionHelper {

    // on java.lang.Throwable
    public static void printStackTrace(Object self, java.io.PrintStream ps) {
        // need to unwrap the print stream somehow so that can use the typical printing streams?
        ((Throwable)self).printStackTrace(); // just print to typical channel I guess
        if(System00DJ.out != ps && System00DJ.err != ps) {
            // we are not printing to a standard print stream, idk where this is going
            // need to convert it to send it to the print stream that is given
            InternalInterface.debug("gg print stream");
            throw new NotImplementedException();
        }
    }
}
