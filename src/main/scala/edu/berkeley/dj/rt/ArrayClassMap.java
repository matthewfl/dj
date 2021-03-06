package edu.berkeley.dj.rt;

import javassist.ClassMap;

/**
 * Created by matthewfl
 *
 * Transform references to array based objects into our array types
 */
public class ArrayClassMap extends ClassMap {

    final private Rewriter rewriter;

    ArrayClassMap(Rewriter rw) {
        rewriter = rw;
    }

    @Override
    public Object get(Object jvn) {
        try {
            String name = (String)jvn;
            if(name.contains("[")) {
                return rewriter.rewriteArrayType(name, true);
            }
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public String getM(String jvn, boolean addL) {
        if(jvn.contains("[")) {
            return rewriter.rewriteArrayType(jvn, addL);
        }
        return null;
    }
}
