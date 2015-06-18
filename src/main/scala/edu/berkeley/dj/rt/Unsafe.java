package edu.berkeley.dj.rt;

import java.lang.reflect.Field;

/**
 * Created by matthewfl
 */
public class Unsafe {

    final static sun.misc.Unsafe theUnsafe;

    static {
        sun.misc.Unsafe v = null;
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            v = (sun.misc.Unsafe) f.get(null);
        }
        catch (NoSuchFieldException e) {}
        catch (IllegalArgumentException e) {}
        catch (IllegalAccessException e) {}
        theUnsafe = v;
    }

}
