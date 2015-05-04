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

    final private Rewriter rewriter;

    final private String prefix;

    JClassMap(Manager man, Rewriter rw) {
        manager = man;
        rewriter = rw;
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
            "java/lang/Integer", // TODO: other types
            "java/lang/Long",
            "java/lang/Float",
            "java/lang/Double",
            "java/lang/Boolean",
            "java/lang/Short",
            "java/lang/Char",
            "java/lang/Throwable",
            "java/lang/Exception",
            "java/lang/RuntimeException",
            "java/lang/Class"

            // tmp here until fixed issue with native methods
            //"java/lang/System"
    };

    @Override
    public Object get(Object jvn) {
        try {
            String name = (String) jvn;
            for (String n : rewritePrefixes) {
                if(name.startsWith(n)) {
                    // we have found the prefix, so unless this is exempted
                    /*if(!rewriter.canRewriteClass(name))
                        return name;
                        */
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
