package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public class PreMain {

    static public void premain(Object ii, String maincls, String[] args) throws Throwable {
        //InternalInterface.ii = (InternalInterface) ii;
        //String ts = (String)ii.getClass().getMethod("toString", new Class[]{}).invoke(ii);
        InternalInterface.setInternalInterface(ii);
        //System.out.println("hello world early ");//+ts);
        Class cls = PreMain.class.getClassLoader().loadClass(maincls);
        cls.getDeclaredMethod("main", new Class[]{String[].class}).invoke(null, new Object[]{args});
        ThreadHelpers.exitThread();
    }
}