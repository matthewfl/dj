package edu.berkeley.dj.rt;

import java.lang.reflect.Field;

/**
 * Created by matthew on 5/21/15.
 */
public class Unsafe {

    final static sun.misc.Unsafe theUnsafe;

    static {
        sun.misc.Unsafe v = null;
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            v = (sun.misc.Unsafe) f.get(null);
            f.setAccessible(false);
        }
        catch (NoSuchFieldException e) {}
        catch (IllegalArgumentException e) {}
        catch (IllegalAccessException e) {}
        theUnsafe = v;
    }

}
