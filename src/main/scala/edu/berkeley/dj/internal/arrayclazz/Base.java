package edu.berkeley.dj.internal.arrayclazz;

import edu.berkeley.dj.internal.RewriteAllBut;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {})
public interface Base /*extends Object00 */ {

    int length();

    Object get(int i);

    void set(int i, Object v);

    static int length(Base b) { return b.length(); }

}

