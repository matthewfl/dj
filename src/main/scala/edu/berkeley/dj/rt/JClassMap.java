package edu.berkeley.dj.rt;

import javassist.ClassMap;

/**
 * Created by matthewfl
 *
 * Written in java b/c the scala compiler is throwing a hissy fit when trying to extend this class
 *
 * Change all the references to protected namespaces such as java.* to our internal namespace
 * this is because we can not reload or rewrite classes in the protected namespaces
 */
public class JClassMap extends ClassMap {

    final private Manager manager;

    final private String prefix;

    JClassMap(Manager man) {
        manager = man;
        // this should end with a ".", so that means that we will end with a slash
        prefix = manager.config().coreprefix().replace(".", "/");
    }

    final static private String[] rewritePrefixes = new String[] {
            "java/",
            "javax/",
            "sun/",
            "com/sun/",
            "org/s3c/",
            "org/xml/"
    };

    final static private String[] exemptedClasses = new String[] {
            "java/lang/String",
            "java/lang/Integer" // TODO: other types
    };

    public Object get(Object jvn) {
        try {
            String name = (String) jvn;
            for (String n : rewritePrefixes) {
                if(name.startsWith(n)) {
                    // we have found the prefix, so unless this is exempted
                    for(String e : exemptedClasses) {
                        if(e.equals(name))
                            return name;
                    }
                    System.err.println("rewriting: "+name);
                    return prefix + name;
                }
            }
        } catch(ClassCastException e) {
            System.err.println("Some how the get method on the class rewriter failed to cast to string");
        }
        return super.get(jvn);
    }
}
