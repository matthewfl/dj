package edu.berkeley.dj.rt;

import javassist.ClassPool;
import javassist.Loader;

/**
 * Created by matthewfl
 */
public class Main {

    public static void main(String[] args) throws Throwable {
        RealMain.main(args);
        //new Loader().run("edu.berkeley.dj.rt.RealMain", args);
        //RealMain.main(args);
        //new ClassLoader().loadClass("edu.berkeley.dj.rt.RealMain").getDeclaredMethod("main", new Class[] { String.class })
        //        .invoke(null, args);
    }

}
