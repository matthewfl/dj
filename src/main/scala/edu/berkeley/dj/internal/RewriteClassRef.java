package edu.berkeley.dj.internal;

/**
 * Created by matthewfl
 *
 * Change a single class name from oldName -> newName
 */
public @interface RewriteClassRef {

    String oldName();

    String newName();
}
