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

    final private String[] extraExemptedClasses;

    final private String prefix;

    JClassMap(Manager man, Rewriter rw, String[] exc) {
        manager = man;
        rewriter = rw;
        extraExemptedClasses = exc;
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

            "java/lang/String", // this is also letting through StringBuilder....
            // TODO: custom string class

            /*
            "java/lang/Integer", // TODO: other types
            "java/lang/Long",
            "java/lang/Float",
            "java/lang/Double",
            "java/lang/Boolean",
            "java/lang/Short",
            "java/lang/Char",
            "java/lang/Void",
            "java/lang/Byte",

            "java/lang/Comparable",
            "java/lang/Number",
            "java/io/Serializable",
*/

            // given that there are some unchanged classes, then will only have
            // the common base of Object
            // To seralize them, may look at using standard java seralization...
            // or make them inmovable
            "java/lang/Object",

            // any exception that may be thrown by the jvm directly should be in here
            // we should not need to list these since we are looking up if the class is
            // inherited from one of the exception classes
            "java/lang/Throwable",
            "java/lang/Exception",
            "java/lang/RuntimeException",
            "java/io/IOException",

            "java/lang/Class",
            // These are here b/c lang/Class uses them,
            "java/lang/ClassLoader",
            "java/lang/reflect/Field",
            "java/lang/reflect/", // let all the reflect stuff through??

            // function-ish interface to native methods for math calls
            "java/lang/StrictMath",

            // the system explicitly looks for this decorator on the stack when reflecting to the caller
            //"sun/reflect/CallerSensitive"
    };

//    final static private String[] = new String[] {
//        ""
//    };

    @Override
    public Object get(Object jvn) {
        try {
            String name = (String) jvn;
            int gensuf = name.indexOf("<");
            String suffix = "";
            String nonTname = name;
            if(gensuf != -1) {
                suffix = name.substring(gensuf);
                nonTname = name.substring(0, gensuf);
            }
            if(name.startsWith(prefix)) {
                // remove the two suffix from the internal class names
                boolean shouldBeRewritten = false;
                for(String n : rewritePrefixes) {
                    if(name.startsWith(prefix + n)) {
                        shouldBeRewritten = true;
                        break;
                    }
                }
                if(!shouldBeRewritten) {
                    // we need to strip the prefix from this class since we aren't suppose to have it
                    nonTname = nonTname.substring(prefix.length());
                    name = name.substring(prefix.length());
                }
                if (nonTname.endsWith("00DJ")) {
                    return nonTname.substring(0, nonTname.length() - 4) + suffix;
                }
                if(nonTname.contains("00DJ$")) {
                    return nonTname.replace("00DJ$", "$") + suffix;
                }
                // check the Replace name with self annotation
                String nn = rewriter.forceClassRename(nonTname);
                if(nn != null)
                    return nn + suffix;
            }
            for (String n : rewritePrefixes) {
                if(name.startsWith(n)) {
                    // we have found the prefix, so unless this is exempted
                    // TODO: clean this up
                    if(!rewriter.canRewriteClass(name))
                        return name;
                    for(String e : exemptedClasses) {
                        // have to have start with as this is the "raw" class name, with template params
                        // eg: get stuff like "java/lang/Class<*>"
                        if(name.startsWith(e))
                            return name;
                    }
                    for(String e : extraExemptedClasses) {
                        if(name.startsWith(e))
                            return name;
                    }
                    //System.err.println("rewriting: "+name);
                    return prefix + name;
                    /*String nn = rewriter.forceClassRename(ret);
                    if(nn != null)
                        return nn + suffix;
                    else
                        return ret;
                        */
                }
            }
        } catch(ClassCastException e) {
            System.err.println("Some how the get method on the class rewriter failed to cast to string");
            throw new RuntimeException(e);
        }
        return super.get(jvn);
    }
}
