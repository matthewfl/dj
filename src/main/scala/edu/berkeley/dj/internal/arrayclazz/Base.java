package edu.berkeley.dj.internal.arrayclazz;

import edu.berkeley.dj.internal.RewriteAllBut;

/**
 * Created by matthewfl
 */
@RewriteAllBut(nonModClasses = {})
public interface Base /*extends Object00 */ {

    int length();

    static int length(Base b) { return b.length(); }

}

