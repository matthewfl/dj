package edu.berkeley.dj.internal;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by matthewfl
 */
public class ReflectionHelper {

    // from sun.reflect.Reflection but that requires that stuff is loaded by the system loader
    static public Class<?> getCallerClass() {
        // this is such a hack...
        Throwable t = new Throwable();
        StackTraceElement[] st = t.getStackTrace();

        try {
            return Class.forName(st[2].getClassName());
        } catch (ClassNotFoundException e) {}
        throw new NotImplementedException();
    }
}
