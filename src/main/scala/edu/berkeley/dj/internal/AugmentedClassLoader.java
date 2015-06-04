package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * This class is used to replace methods such as forName on Class and loadClass on the classLoader
 */
@RewriteAllBut(nonModClasses = {})
public class AugmentedClassLoader {

    private AugmentedClassLoader() {}

    static Class<?> forName(String classname) throws ClassNotFoundException {
        Class<?> ret = Class.forName(classname);
        return ret;
    }

    static Class<?> forName(String classname, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        Class<?> ret = Class.forName(classname, initialize, loader);
        return ret;
    }

    static Class<?> loadClass(Object o, String name) throws ClassNotFoundException {
        if(o instanceof ClassLoader) {
            Class<?> ret = ((ClassLoader)o).loadClass(name);
            return ret;
        }
        return null;
    }
}
