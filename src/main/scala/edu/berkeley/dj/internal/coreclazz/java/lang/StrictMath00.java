package edu.berkeley.dj.internal.coreclazz.java.lang;

import edu.berkeley.dj.internal.ReplaceSelfWithCls;

/**
 * Created by matthewfl
 *
 * Prevent this class from being replace
 */
@ReplaceSelfWithCls(
        cls = java.lang.StrictMath.class
)
public class StrictMath00 {
}
