package edu.berkeley.dj.internal;

import edu.berkeley.dj.jit.JITInterface;

/**
 * Created by matthewfl
 */
public class PreMain {

    static public void premain(Object ii, String djitcls, String maincls, String[] args) throws Throwable {
        InternalInterface.setInternalInterface(ii);


        // load the distribuited jit
        Class<?> dcls = PreMain.class.getClassLoader().loadClass(djitcls);
        JITWrapper.setJIT((JITInterface)dcls.newInstance());


        Class<?> strcls = Class.forName("edu.berkeley.dj.internal.arrayclazz.java.lang.String_1");
        Object aargs = ArrayHelpers.makeDJArray(args);

        // load the main class
        Class cls = PreMain.class.getClassLoader().loadClass(maincls);

        // register this program on all remote machines
        InternalInterface.getInternalInterface().startNetwork();

        // call the main method
        cls.getDeclaredMethod("main", new Class[]{strcls}).invoke(null, new Object[]{ aargs });
        ThreadHelpers.exitThread();
    }
}
