package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * This class is used to replace methods such as forName on Class and loadClass on the classLoader
 */
@RewriteAllBut(nonModClasses = {})
public class AugmentedClassLoader {

    private AugmentedClassLoader() {}

    // On java.lang.Class
    public static Class<?> forName(String classname) throws ClassNotFoundException {
        Class<?> ret = Class.forName(classname);
        return ret;
    }

    public static Class<?> forName(String classname, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        Class<?> ret = Class.forName(classname, initialize, loader);
        return ret;
    }

    // on java.lang.ClassLoader
    public static Class<?> loadClass(Object self, String name) throws ClassNotFoundException {
        if(self instanceof ClassLoader) {
            Class<?> ret = ((ClassLoader)self).loadClass(name);
            return ret;
        }
        return null;
    }
}
