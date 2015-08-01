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
        // TODO: convert the arguments from the base array type to
        Class<?> strcls = Class.forName("edu.berkeley.dj.internal.arrayclazz.java.lang.String_1");
        Object aargs = ArrayHelpers.makeDJArray(args);
        cls.getDeclaredMethod("main", new Class[]{strcls}).invoke(null, new Object[]{ aargs });
        ThreadHelpers.exitThread();
    }
}
