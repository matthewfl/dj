package edu.berkeley.dj.internal;

import java.lang.reflect.Field;

/**
 * Created by matthewfl
 */
public class AugmentedString {

    static public void packageStringConstructor(Object o, char[] c, boolean z) {
        String s = (String)o;
        try {
            Field f = s.getClass().getDeclaredField("value");
            f.setAccessible(true);
            f.set(s, c);
        }
        catch(NoSuchFieldException e) {}
        catch(IllegalAccessException e) {}
    }
}
