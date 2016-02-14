package edu.berkeley.dj.internal.arrayclazz;

import edu.berkeley.dj.internal.RewriteAllBut;

/**
 * Created by matthewfl
 */

@RewriteAllBut(nonModClasses = {})
public interface Base {

    // will return the correct value, even perform network calls etc
    int length();

    // will return the local value or error, no network calls
    int raw_length();

    Object get_java_lang_Object(int i);

    void set_java_lang_Object(int i, Object v);

    static int length(Base b) {
        long time = System.nanoTime();
        try {
            return b.length();
        } catch(NullPointerException e) {
            throw e;
        }
    }


}

