package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 */
public @interface RewriteClassRefCls {

    Class<?> oldCls();

    String newName();
}
