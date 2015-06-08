package edu.berkeley.dj.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public static Class<?> getPrimitiveClass(String name) {
        // this method is package private, but we are rewriting some classes that need to use it
        try {
            Method mth = Class.class.getDeclaredMethod("getPrimitiveClass", new Class[]{String.class});
            mth.setAccessible(true);
            return (Class<?>)mth.invoke(null, name);
        }
        catch(NoSuchMethodException e) {}
        catch(IllegalAccessException e) {}
        catch(InvocationTargetException e) {}
        return null;
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

    public static void checkClassLoaderPermission(ClassLoader cl, Class<?> caller) {
        // this is package private, but we want to access it
        // TODO:
    }

}
