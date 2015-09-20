package edu.berkeley.dj.internal.arrayclazz;

import edu.berkeley.dj.internal.RewriteAllBut;

/**
 * Created by matthewfl
 */

@RewriteAllBut(nonModClasses = {})
public interface Base {

    int length();

    Object get_java_lang_Object(int i);

    void set_java_lang_Object(int i, Object v);

    static int length(Base b) { return b.length(); }

}

