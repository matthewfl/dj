package edu.berkeley.dj.internal.coreclazz;

/**
 * Created by matthewfl
 *
 * A field that will not be distribuited between machines
 * Think thread local but for a field and to a machine
 *
 * Used when having to modify provided classes to be aware of local but also distributed nature
 */
public @interface RewriteLocalFieldOnly {
}
