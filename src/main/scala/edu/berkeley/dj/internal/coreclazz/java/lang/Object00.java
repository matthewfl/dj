package edu.berkeley.dj.internal.coreclazz.java.lang;

import edu.berkeley.dj.internal.ClassManager;
import edu.berkeley.dj.internal.SeralizeManager;

/**
 * Created by matthewfl
 *
 * This is here so that interfaces and methods that are not on
 * the object directly can "implement" this and
 * it will still be able to use changed methods
 *
 * This class will replace the java.lang.Object class, the 2 is append to the name of this class
 * so that we can still reference original java.lang.Object when compiling, but the 2 suffix will
 * be removed when the class is loaded.
 */
public interface Object00 {

    int __dj_getClassMode();

    ClassManager __dj_getManager();

    int hashCode();

    boolean equals(Object obj);

    boolean equals(Object00 obj);

    void __dj_notify();

    void __dj_notifyAll();

    void __dj_wait(long timeout) throws InterruptedException;

    void __dj_wait(long timeout, int nanos) throws InterruptedException;

    void __dj_wait() throws InterruptedException;

    void __dj_monitorenter();

    void __dj_monitorexit();

    void __dj_seralize_obj(SeralizeManager man);

    void __dj_deseralize_obj(SeralizeManager man);
}

