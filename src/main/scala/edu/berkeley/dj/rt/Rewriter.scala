package edu.berkeley.dj.rt

import java.lang.reflect.UndeclaredThrowableException
import javassist._
import javassist.bytecode.{Descriptor, MethodInfo, SignatureAttribute}

import edu.berkeley.dj.internal._
import edu.berkeley.dj.rt.convert.CodeConverter
import edu.berkeley.dj.rt.convert._
import edu.berkeley.dj.utils.Memo


/**
 * Created by matthewfl
 */
private[rt] class Rewriter (private val manager : Manager) {
  def config = manager.config

  def basePool = manager.pool

  def runningPool = manager.runningPool

  private lazy val moveInterface = runningPool.get("edu.berkeley.dj.internal.Movable")

  private lazy val proxyInterface = runningPool.get("edu.berkeley.dj.internal.Proxied")

  private lazy val objectBase = runningPool.get("edu.berkeley.dj.internal.ObjectBase")

  private lazy val objectBaseInterface = runningPool.get(s"${config.coreprefix}java.lang.Object")

  private lazy val classMangerBase = runningPool.get("edu.berkeley.dj.internal.ClassManager")

  // these classes are noted as not being movable
  // this should contain items such as socket classes
  // and filesystem as we don't want to break network connections
  val NonMovableClasses = Set(
    "java.lang.Object"
  )

  // if these methods are called from
  // anywhere in a program
  // rewrite them to the new methods
  val rewriteMethodCalls = Map(
    // TODO: add the method signatures onto these items
    // and don't use
    // TODO: the maps for when they have argument types
    ("notify","()V","java.lang.Object") -> ("__dj_nofity", s"${config.coreprefix}java.lang.Object"),
    ("notifyAll", "()V", "java.lang.Object") -> ("__dj_notifyAll", s"${config.coreprefix}java.lang.Object"),
    ("wait", "()v", "java.lang.Object") -> ("__dj_wait", s"${config.coreprefix}java.lang.Object")

  )

  // if these methods are anywhere
  val rewriteMethodNames = Map(
    "finalize" -> "__dj_client_finalize"
  )

  //val replacedClasses = Map()

  private def isClassMovable(cls: CtClass) = {
    !NonMovableClasses.contains(cls.getName)
  }

  private def getUsuableName(typ: CtClass): String = {
    if (typ.isArray) {
      getUsuableName(typ.getComponentType) + "[]"
    } else if (typ.isPrimitive) {
      typ.getName
    } else {
      typ.getName.split("\\.").map("``" + _ + "``").mkString(".")
    }
  }

  private def reassociateClass(cls: CtClass) = {
    if(cls.getClassPool != runningPool) {
      // prime the cache of the class before we move it to a new pool
      cls.getClassFile
      cls.setClassPool2(runningPool)
      // cache this class before we have fully rewritten it in hopes that we don't loop
      runningPool.setClass(cls.getName, cls)
    }
  }

  lazy val jclassmap = new JClassMap(manager, this, Array[String]())

  private[rt] def forceClassRename(classdesc: String): String = {
    val cls = Descriptor.toCtClass(classdesc, basePool)
    val an = cls.getAnnotation(classOf[ReplaceSelfWithCls])
    if(an != null) {
      "test"
    } else
      null
  }



  /*val exemptedClassesDesc = Set(
    "java/lang/String",
    "java/lang/Integer",
    "java/lang/Long"
  )*/

  private[rt] def canRewriteClass(classdesc: String): Boolean = {
    /*if(exemptedClassesDesc.contains(classdesc))
      return false
    */
    // do a lookup of the class and check if it is a subclass of a good type

    try {
      var curcls = Descriptor.toCtClass(classdesc, basePool)
      while (curcls != null) {
        // we can not change a throwable class since we need to catch these items
        // and the jvm checks that it inherits from throwable etc
        // at lease these items will be seralizable....sigh
        if (curcls.getName == "java.lang.Throwable")
          return false
        curcls = curcls.getSuperclass
      }
    } catch {
      case e: NotFoundException => {}
    }
    true
  }

  private def rewriteUsedClasses(cls: CtClass, jcm: JClassMap): Unit = {
    val isInterface = cls.isInterface
    val superClass = cls.getSuperclass.getName
    //val useObjectBase = cls.getSuperclass.getName == "java.lang.Object"
    cls.replaceClassName(jcm)

    if(isInterface) {
      // interfaces need to inherit from java.lang.Object, but we changed that when re rewrote the
      // references, and the normal methods check if it is an interface, and makes it a nop, so do it this way
      cls.getClassFile.setSuperclass("java.lang.Object")
      cls.addInterface(objectBaseInterface)
    } else if(superClass == "java.lang.Object") {
      cls.setSuperclass(objectBase)
    } else {
      // some issue with setting the super class using replaceClassName
      val sclass = jcm.get(superClass.replace(".","/")).asInstanceOf[String]
      if(sclass != null)
        cls.getClassFile.setSuperclass(sclass.replace("/","."))
    }
  }

  private def rewriteUsedClasses(cls: CtClass): Unit = {
    rewriteUsedClasses(cls, jclassmap)
  }

  private def transformClass(cls: CtClass) = {
    //val manager = runningPool.makeClass("edu.berkeley.dj.internal.managers."+cls.getName, classMangerBase)
    //rewriteUsedClasses(cls)
    // TODO: actually determine if this class is movable before adding the move interface
    cls.addInterface(moveInterface)
    val codeConverter = new CodeConverter
    /*rewriteMethodCalls.foreach(v => {
      val mth = cls.getMethods.filter(_.getName == v._2)
      if(!mth.isEmpty)
        codeConverter.redirectMethodCall(v._1, mth(0))
    })*/
    /*codeConverter.addTransform(new FunctionCalls(codeConverter.prevTransforms, rewriteMethodCalls.map(n => {
      // TODO: can't use this as it is causing issues with circular references to
      // classes that aren't loaded yet
      val mths = cls.getMethods.filter(_.getName == n._2)
      if (!mths.isEmpty)
        Map(n._1 -> mths(0))
      else
        Map[String, CtMethod]()
    }).reduce(_ ++ _)))*/
    //codeConverter.addTransform(new FunctionCalls(codeConverter.prevTransforms, rewriteMethodCalls))

    //codeConverter.addTransform(new Arrays(codeConverter.prevTransforms, config))

    //codeConverter.addTransform(new FieldAccess(codeConverter.prevTransforms, config))
    //codeConverter.addTransform(new Monitors(codeConverter.prevTransforms))

    val isInterface = Modifier.isInterface(cls.getModifiers)
    if (!isInterface) {
      cls.instrument(codeConverter)
      for (field <- cls.getDeclaredFields) {
        val name = field.getName
        println("field name: " + name)
        // TODO: manage arrays
        if (!name.startsWith(config.fieldPrefix) && !field.getFieldInfo.getDescriptor.contains("[")) {
          //val typ = field.getType

          val typ_name = "int"//getUsuableName(typ)
          val modifiers = field.getModifiers

          //SignatureAttribute.toFieldSignature(field.getGenericSignature)

          val accessMod =
            if (Modifier.isPublic(modifiers))
              "public"
            else if (Modifier.isProtected(modifiers))
              "protected"
            else if (Modifier.isPrivate(modifiers))
              "private"
            else // must be isPackage
              ""

          val finalField = Modifier.isFinal(modifiers)

          if (finalField) {
            // we don't want any final fields as we might want to change their values later?
            // also it makes it harder to overwrite the access to field since the writes can't happen
            // outside the constructor
            // todo:? should the system check if a class is inited and then raise some error in case of a final field
            // or just assume that the final field nature will already be check by other systems during compilation
            // someone could always just use reflection to set final fields, so it isn't like it is imossible
            //field.setModifiers(modifiers & ~Modifier.FINAL)
          }

          // TODO: problem right now that there is an issue dealing with templated
          // I wonder if this has more to deal with the fact that it should use invoke special rather then invokevirtual
          // since it is a method on the current class, so if the value is package private or private then
          // the invoke method used should change

          // TODO: deal with static variables
          if (!Modifier.isStatic(modifiers) /*&& cls.getName.contains("StringIndexer")*/ ) {
            val write_method =
              s"""
              ${accessMod} void ``${config.fieldPrefix}write_field_${name}`` (${typ_name} val) {
                if((this.__dj_class_mode & 0x02) != 0) {
                  //System.out.println("Imagine doing something remote here on ${cls.getName}");
                //  this.__dj_class_manager.writeField(0, val);
                }
                //} else {
                 // this.``${name}`` = val;
                 this.``${name}`` = val;
                //}
             }
              """
            val read_method =
              s"""
           ${accessMod} ${typ_name} ``${config.fieldPrefix}read_field_${name}`` () {
             //if(this.__dj_class_mode & 0x01 != 0) {
               System.out.println("reading field ${name}");
             //  return this.``${name}``;
             //} else {
               return this.``${name}``;
             //}
           }
              """
            try {
              println("\t\tadding method for: " + name + " to " + cls.getName + " type " + typ_name)
              //cls.addMethod(CtMethod.make(write_method, cls))
              //cls.addMethod(CtMethod.make(read_method, cls))
            } catch {
              // TODO: remove
              case e: Throwable => {
                println("Compile of method failed: " + e)
              }
            }
          } else {
            // TODO: static field
          }
        }
      }
      /*for(method <- cls.getMethods) {
          if(!method.getName.startsWith(config.fieldPrefix) && Modifier.isSynchronized(method)) {
            method.setWrappedBody()
          }
        }*/


      var seralize_obj_method =
        """
          public void __dj_seralize_obj(edu.berkeley.dj.internal.SeralizeManager man) {
          super.__dj_seralize_obj(man);
        """
      var deseralize_obj_method =
        """
          public void __dj_deseralize_obj(edu.berkeley.dj.internal.SeralizeManager man) {
          super.__dj_deseralize_obj(man);
        """
      /*for (field <- cls.getDeclaredFields) {
        if (field.getType.isPrimitive) {
          seralize_obj_method +=
            s"""
              man.put_value_${field.getType.getName}(this.``${field.getName}``);
            """
          deseralize_obj_method +=
            s"""
             this.``${field.getName}`` = man.get_value_${field.getType.getName}();
           """
        } else {
          // for object should check if the object is movable first
          if (isClassMovable(field.getType)) {

          } else {
            // TODO: seralize a proxy object so we can still use this later
          }
        }
      }*/


    }
    // TODO: need to handle interfaces that can have methods on them
  }



  private def modifyClass(cls: CtClass): Unit = {
    println("rewriting class: " + cls.getName)
    val mods = cls.getModifiers
    println("modifiers: " + Modifier.toString(mods))
    reassociateClass(cls)

    /*val sc = cls.getSuperclass
    if(sc.getName != "java.lang.Object") {
      // we want to make sure that we have loaded any classes that this depends on
      // so that we have already overwritten their methods
      //runningPool.get(sc.getName)
    } else if(!Modifier.isInterface(mods)) {
      // this comes directly off the object class
      //cls.setSuperclass(objectBase)
    }*/
    //if(cls.getName.contains("testcase"))
    transformClass(cls)
    rewriteUsedClasses(cls)
  }

  private def modifyInternalClass(cls: CtClass): Unit ={
    // the internal classes have special annotations on them to control how they are rwriten
    var clsa = cls
    // if the class is internal to something, we still want the annotations for the file to be "active"

    while(clsa != null) {
      val anns = clsa.getAnnotations
      for (ann <- anns) {
        ann match {
          case norerw: RewriteAllBut => {
            // rewrite all but a few types using the annotation
            rewriteUsedClasses(cls, new JClassMap(manager, this, norerw.nonModClasses()))
          }
          case nrw: RewriteClassRef => {
            // replace a single class name
            cls.replaceClassName(nrw.oldName(), nrw.newName())
          }
          case nrw: RewriteClassRefCls => {
            try {
              cls.replaceClassName(nrw.oldCls().getName, nrw.newName());
            } catch {
              case e: UndeclaredThrowableException => {
                println(e.getCause)
                throw e.getCause()
              }
            }
          }
          case sp: SetSuperclass => {
            // force the super class to be something else
            cls.setSuperclass(cls.getClassPool.get(sp.superclass()))
          }
          case _ => {} // nop
        }
      }
      clsa = clsa.getDeclaringClass
    }


    // TODO: maybe replace fields and method declerations
    // that may become complicated if this would have static inits and methods calls
    /*for(mth <- cls.getDeclaredMethods) {

    }*/

    //rewriteUsedClasses(cls, new JClassMap(manager, this, nonrwcls.toArray))

    if(cls.isInterface) {
      // WTF: the base of an interface always needs to be java.lang.Object, but somehow this is being overwritten
      cls.getClassFile.setSuperclass("java.lang.Object")
    }
  }

  private def hasNativeMethods(cls: CtClass): Boolean = {
    // if this clsas contains some native methods then we can't
    // directly instaniate this class, as it will need the native methods
    // so instead we will make proxy methods for all of its public and protected methods
    cls.getDeclaredMethods.foreach(m => {
      if(Modifier.isNative(m.getModifiers))
        return true
    })
    false
  }

  private def makeDummyValue(cls: CtClass) = {
    if(cls.isArray) {
      s"new ${getUsuableName(cls.getComponentType)}[0]"
    } else if(cls.isPrimitive) {
      // need to determine what primitive type this is, and return some dummy value for that
      import CtClass._
      cls match {
        case `voidType` => "" // ???
        case `booleanType` => "false"
        case `byteType` => "0"
        case `charType` => "' '"
        case `shortType` => "0"
        case `intType` => "0"
        case `longType` => "0L"
        case `floatType` => "0.0f"
        case `doubleType` => "0.0"
        case _ => "gg" // unknown?
      }
    } else {
      "null" // this is just going to be some pointer to an object, so just return null
    }
  }

  /*private def makeProxyCls(cls: CtClass): CtClass = {
    // if there is a public field then there is nothing we can do directly to manage that
    // we can instead make the __dj_ methods that are used for reading and writing the field

    val ret = runningPool.makeClass(config.coreprefix + cls.getName)
    ret.addInterface(proxyInterface)
    ret.setSuperclass(runningPool.get(config.coreprefix + cls.getSuperclass.getName))

    ret.addField(CtField.make(s"private ${getUsuableName(cls)} __dj_ProxiedObject;", ret))

    val ret_proxy_obj_mth =
      """
         public Object __dj_getRawProxyPointer() { return __dj_ProxiedObject; }
      """
    ret.addMethod(CtMethod.make(ret_proxy_obj_mth, ret))

    // TODO: make it be able to access protected methods/fields
    // this is going to have to make some subclass of the proxy class that exposes all of the protected methods

    def getArguments(sig: String): Seq[CtClass] = {
      // sig string like: (Ljava/lang/String;)V
      val ar = sig.substring(sig.indexOf("(") + 1, sig.indexOf(")"))
      val args = ar.split(";")
      if(args.length == 1 && args(0).isEmpty) {
        // there were no arguments
        Seq()
      } else {
        for (i <- 0 until args.length) yield {
          Descriptor.toCtClass(args(i) + ";", cls.getClassPool)
        }
      }
    }

    cls.getConstructors.foreach(m => {
      val sig = m.getSignature
      // TODO: same sort of method creation for the constructors, once I figure this out.....
      //println(sig)
    })

    try {
      // we need to overwrite all the methods, not just the ones that are declared on this object
      // since there may be methods that were inherited
      // there is also the potential issue with methods such as wait and getting monitors since they will need
      // to be shared with the base object

      /*cls.getMethods.filter(m => {
        Modifier.isPublic(m.getModifiers) && m.getDeclaringClass.getName != "java.lang.Object"
      }).groupBy()*/

      cls.getMethods.foreach(m => {
        if ( /*Modifier.isProtected(m.getModifiers) ||*/ Modifier.isPublic(m.getModifiers) && true) {
          // TODO: get the
          val args = getArguments(m.getSignature)
          // work in progress code to call the native function
          // will need to cast back and forth between the correct types for this
          // __dj_ProxiedObject.``${m.getName}`` (${(0 until args.size).map(" a" + _).mkString(", ")}) ;
          val meth_code =
            s"""
              public ${getUsuableName(m.getReturnType)} ``${m.getName}`` (${args.zipWithIndex.map(v => getUsuableName(v._1) + " a" + v._2).mkString(", ")}) {
                ${if (m.getReturnType != CtClass.voidType) "return" else ""} ${makeDummyValue(m.getReturnType)} ;
              }
           """
          ret.addMethod(CtMethod.make(meth_code, ret))
        }
      })
    } catch {
      case e: Exception => {
        println(e)
        throw e
      }
    }
    cls.getFields.foreach(f => {
      if(/*Modifier.isProtected(f.getModifiers) ||*/ Modifier.isPublic(f.getModifiers)) {

      }
    })
    ret
  }*/

  private def getAccessControl(mod: Int) = {
    if(Modifier.isPublic(mod)) {
      "public"
    } else if(Modifier.isProtected(mod)) {
      "protected"
    } else if(Modifier.isPrivate(mod)) {
      "private"
    } else if(Modifier.isPackage(mod)) {
      ""
    }
  }

  private def overwriteNativeMethods(cls: CtClass) = {
    /*def getArguments(sig: String): Seq[CtClass] = {
      // sig string like: (Ljava/lang/String;)V
      val ar = sig.substring(sig.indexOf("(") + 1, sig.indexOf(")"))
      val args = ar.split(";")
      if(args.length == 1 && args(0).isEmpty) {
        // there were no arguments
        Seq()
      } else {
        for (i <- 0 until args.length) yield {
          Descriptor.toCtClass(args(i) + ";", cls.getClassPool)
        }
      }
    }*/

    val rwMembers = cls.getDeclaredMethods.filter(m=>Modifier.isNative(m.getModifiers))

    rwMembers.foreach(m=> {
      cls.removeMethod(m)
    })

    rwMembers.foreach(m=>{
      //val args = getArguments(m.getSignature)
      val args = Descriptor.getParameterTypes(m.getSignature, cls.getClassPool)
      // TODO: deal with the fact we have stuff rewritten into internal.coreclazz namespace
      val static = if(Modifier.isStatic(m.getModifiers)) "static" else ""

      val mth_code = s"""
           ${getAccessControl(m.getModifiers)} ${static} ${getUsuableName(m.getReturnType)} ``${m.getName}`` (${args.zipWithIndex.map(v => getUsuableName(v._1) + " a" + v._2).mkString(", ")}) {
             edu.berkeley.dj.internal.InternalInterface.getInternalInterface().simplePrint("\t\tcall native: ${cls.getName} ${m.getName}");
             ${if (m.getReturnType != CtClass.voidType) "return" else ""} ${makeDummyValue(m.getReturnType)} ;
           }
         """
      cls.addMethod(CtMethod.make(mth_code, cls))
    })
  }


  def createCtClass(classname: String): CtClass = {
    MethodInfo.doPreverify = true

    if (classname.startsWith("edu.berkeley.dj.rt")) {
      // do not allow loading the runtime into the runtime
      throw new ClassNotFoundException(classname)
    }

    /*if(classname == "edu.berkeley.dj.internal.ObjectBase") {
      return objectBaseRaw
    }*/

    if(classname.endsWith("[]")) {
      // this is some array type, so treat it as such
      // in the future we should rewrite arrays with our methods,
      return new CtArray(classname, runningPool)
    }

    val cls: CtClass = try {
      basePool get classname
    } catch {
      case e: NotFoundException => {
        try {
          if(classname.startsWith(config.coreprefix)) {
            val c = basePool get (classname + "2")
            c.setName(classname)
            c
          } else null
        } catch {
          case e: NotFoundException => null
        }
      }
    }
    println("create class name:" + classname)

    // for edu.berkeley.dj.internal.coreclazz.
    if (classname.startsWith(config.coreprefix)) {
      if (cls != null) {
        reassociateClass(cls)
        modifyInternalClass(cls)
        return cls
      }
      var orgName = classname.drop(config.coreprefix.size)
      val clso = basePool get orgName
      /*if(hasNativeMethods(clso)) {
        return makeProxyCls(clso)
      }*/
      reassociateClass(clso)
      clso.setName(classname)
      if(hasNativeMethods(clso)) {
        overwriteNativeMethods(clso)
      }
      modifyClass(clso)
      return clso
    }

    if(classname.startsWith(config.proxysubclasses)) {
      // TODO:
      throw new NotImplementedError()
    }

    if (cls == null)
      return null

    if(cls.isPrimitive) {
      return cls
    } else if(cls.isArray) {
      // TODO: some custom handling for array types

      // don't think tha this is ever used

    } else if (!classname.startsWith("edu.berkeley.dj.internal.")) {
      modifyClass(cls)
    } else if (classname.startsWith(config.internalPrefix)) {
      modifyInternalClass(cls)
    }
    cls
  }

}
