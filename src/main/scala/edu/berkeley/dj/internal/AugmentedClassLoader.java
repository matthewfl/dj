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
        String ncn = InternalInterface.getInternalInterface().classRenamed(classname);
        Class<?> ret;
        if(ncn != null) {
            ret = Class.forName(ncn);
        } else {
            ret = Class.forName(classname);
        }
        return ret;
    }

    public static Class<?> forName(String classname, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        String ncn = InternalInterface.getInternalInterface().classRenamed(classname);
        Class<?> ret;
        if(ncn != null) {
            ret = Class.forName(ncn, initialize, loader);
        } else {
            ret = Class.forName(classname, initialize, loader);
        }
        return ret;
    }

    // on java.lang.ClassLoader
    public static Class<?> loadClass(Object self, String name) throws ClassNotFoundException {
        String ncn = InternalInterface.getInternalInterface().classRenamed(name);
        if(self instanceof ClassLoader) {
            Class<?> ret;
            if(ncn != null) {
                ret = ((ClassLoader)self).loadClass(ncn);
            } else {
                ret = ((ClassLoader)self).loadClass(name);
            }
            return ret;
        }
        throw new RuntimeException("Unexpected self type for java.lang.ClassLoader:loadClass");
    }
}
