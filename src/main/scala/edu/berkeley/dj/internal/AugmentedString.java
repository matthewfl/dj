package edu.berkeley.dj.internal;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

/**
 * Created by matthewfl
 */
@RewriteAddArrayWrap
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


    public static String make(char[] c, boolean z) {
        return new String(c);
    }

    public static String make(char [] c) {
        return new String(c);
    }

    public static String make(char[] c, int offset, int length) {
        return new String(c, offset, length);
    }

    public static String make(String s) {
        return new String(s);
    }

    public static String make() {
        return new String();
    }

    public static String make(int[] a, int offset, int length) {
        return new String(a, offset, length);
    }

    @SuppressWarnings( "deprecation" )
    public static String make(byte[] ascii, int hb, int offset, int count) {
        return new String(ascii, hb, offset, count);
    }

    @SuppressWarnings( "deprecation" )
    public static String make(byte[] ascii, int hb) {
        return new String(ascii, hb);
    }

    public static String make(byte[] b, int offset, int length, String charset) throws UnsupportedEncodingException {
        return new String(b, offset, length, charset);
    }

    public static String make(byte[] b, int offset, int length, Charset cs) {
        return new String(b, offset, length, cs);
    }

    public static String make(byte[] b, String cs) throws UnsupportedEncodingException {
        return new String(b, cs);
    }

    public static String make(byte[] b, Charset cs) {
        return new String(b, cs);
    }

    public static String make(byte[] b, int o, int l) {
        return new String(b, o, l);
    }

    public static String make(byte[] b) {
        return new String(b);
    }

    public static String make(StringBuffer b) {
        return new String(b);
    }

    public static String make(StringBuilder b) {
        return new String(b);
    }

    public static void getChars(Object self, int a, int b, char[] arr, int c) {
        ((String)self).getChars(a,b,arr,c);
    }

    public static char[] toCharArray(Object self) {
        return ((String)self).toCharArray();
    }
}
