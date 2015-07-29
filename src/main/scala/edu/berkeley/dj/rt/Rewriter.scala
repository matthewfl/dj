package edu.berkeley.dj.rt

import java.lang.reflect.UndeclaredThrowableException
import javassist._
import javassist.bytecode.{Descriptor, MethodInfo}

import edu.berkeley.dj.internal._
import edu.berkeley.dj.rt.convert.{CodeConverter, _}
import edu.berkeley.dj.utils.Memo

import scala.collection.mutable

import java.lang.StringBuilder


/**
 * Created by matthewfl
 */
private[rt] class Rewriter (private val manager : MasterManager) {
  def config = manager.config

  def basePool = manager.pool

  def runningPool = manager.runningPool

  def classMode = manager.classMode

  private lazy val moveInterface = runningPool.get("edu.berkeley.dj.internal.Movable")

  private lazy val proxyInterface = runningPool.get("edu.berkeley.dj.internal.Proxied")

  private lazy val objectBase = runningPool.get("edu.berkeley.dj.internal.ObjectBase")

  private lazy val objectBaseInterface = runningPool.get(s"${config.coreprefix}java.lang.Object")

  private lazy val classMangerBase = runningPool.get("edu.berkeley.dj.internal.ClassManager")

  // these classes are noted as not being movable
  // this should contain items such as socket classes
  // and filesystem as we don't want to break network connections
  /*val NonMovableClasses = Set(
    "java.lang.Object"
  )*/

  // if these methods are called from
  // anywhere in a program
  // rewrite them to the new methods
  val rewriteMethodCalls = Map(
    ("notify","()V","java.lang.Object") -> ("notify", s"${config.internalPrefix}ObjectHelpers"),
    ("notifyAll", "()V", "java.lang.Object") -> ("notifyAll", s"${config.internalPrefix}ObjectHelpers"),
    ("wait", "()V", "java.lang.Object") -> ("wait", s"${config.internalPrefix}ObjectHelpers"),
    ("wait", "(J)V", "java.lang.Object") -> ("wait", s"${config.internalPrefix}ObjectHelpers"),
    ("wait", "(JI)V", "java.lang.Object") -> ("wait", s"${config.internalPrefix}ObjectHelpers"),


    // for rewriting the class loader
    ("forName", "(Ljava/lang/String;)Ljava/lang/Class;", "java.lang.Class") -> ("forName", s"${config.internalPrefix}AugmentedClassLoader"),
    ("forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", "java.lang.Class") -> ("forName", s"${config.internalPrefix}AugmentedClassLoader"),
    ("loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", "java.lang.ClassLoader") -> ("loadClass", s"${config.internalPrefix}AugmentedClassLoader"),
    ("getPrimitiveClass", "(Ljava/lang/String;)Ljava/lang/Class;", "java.lang.Class") -> ("getPrimitiveClass", s"${config.internalPrefix}AugmentedClassLoader")

    // rewrite the string init method since this is package private
    // this just gets the field inside the class and sets it to the char array
  )

  // if these methods are anywhere
  val rewriteMethodNames = Map(
    "finalize" -> "__dj_client_finalize"
  )

  //val replacedClasses = Map()

  /*private def isClassMovable(cls: CtClass) = {
    !NonMovableClasses.contains(cls.getName)
  }*/

  private def getUsableName(typ: CtClass): String = {
    if (typ.isArray) {
      getUsableName(typ.getComponentType) + "[]"
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

  //private lazy val descriptorLookup = Memo {}

  private[rt] def forceClassRename(classdesc: String): String = innerForceClassRename(classdesc)

  private lazy val innerForceClassRename = Memo[String,String,String] { case classdesc: String =>
    try {
      val cls = Descriptor.toCtClass(classdesc, basePool)
      val an = cls.getAnnotation(classOf[ReplaceSelfWithCls]).asInstanceOf[ReplaceSelfWithCls]
      if (an != null) {
        if(!an.name().isEmpty)
          an.name().replace('.','/')
        else
          an.cls().getName.replace('.','/')
      } else
        null
    } catch {
      case e: NotFoundException => null
    }
  }

  // basically if it is not an exception
  private[rt] def canRewriteClass(classdesc: String): Boolean = innerCanRewriteClass(classdesc)

  private lazy val innerCanRewriteClass = Memo[String,String,Boolean] { case classdesc: String =>
    // do a lookup of the class and check if it is a subclass of a good type
    var res = true
    try {
      var curcls = Descriptor.toCtClass(classdesc, basePool)
      while (curcls != null) {
        // we can not change a throwable class since we need to catch these items
        // and the jvm checks that it inherits from throwable etc
        // at lease these items will be seralizable....sigh
        if (curcls.getName == "java.lang.Throwable")
          res = false
        curcls = curcls.getSuperclass
      }
    } catch {
      case e: NotFoundException => {}
    }
    res
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

    codeConverter.addTransform(new FunctionCalls(codeConverter.prevTransforms, rewriteMethodCalls))
    codeConverter.addTransform(new SpecialConverter(codeConverter.prevTransforms))

    //codeConverter.addTransform(new Arrays(codeConverter.prevTransforms, config))

    codeConverter.addTransform(new FieldAccess(codeConverter.prevTransforms, config, this))
    codeConverter.addTransform(new Monitors(codeConverter.prevTransforms))

    val isInterface = Modifier.isInterface(cls.getModifiers)

    // basically if some exception type and not inherited from ObjectBase
    val canRewrite = canRewriteClass(cls.getName)
    //val cls_name = getUsuableName(cls)

    cls.instrument(codeConverter)

    if (!isInterface && canRewrite) {
      addAccessorMethods(cls)
      addSeralizeMethods(cls)
    }
    // TODO: need to handle interfaces that can have methods on them
  }

  private def monitorMethodsRewrite(cls: CtClass): Unit = {
    for(m <- cls.getDeclaredMethods) {
      //m.setWrappedBody()
    }
    // TODO:
    ???
  }

  private val fieldCount = new mutable.HashMap[String,Int]()

  private def getFieldCount(classname: String) = {
    if(classname == "edu.berkeley.dj.internal.ObjectBase")
      10
    else {
      if(fieldCount.contains(classname)) {
        fieldCount.getOrElse(classname, 10)
      } else {
        runningPool.get(classname)
        fieldCount.getOrElse(classname, 10)
      }
    }
  }

  private def addAccessorMethods(cls: CtClass): Unit = {
    // an interface in java can not have variables
    // so there is no need to add the accessor methods to the interface
    // also the super class of an interface will be Object so we can't use super.....
    // for calling the __dj_readFieldID_...
    if(Modifier.isInterface(cls.getModifiers))
      return

    val cls_name = getUsableName(cls)

    // if a method is on an interface it has to use another method of accessing the variables
    // but can't have variables on interface so not sure how useful this is atm
    val (cls_mode, cls_manager) = if (Modifier.isInterface(cls.getModifiers)) {
      ("__dj_getClassMode()", "__dj_getManager()")
    } else {
      ("__dj_class_mode", "__dj_class_manager")
    }

    val accessWrites = new mutable.HashMap[String,StringBuilder]()
    val accessReads = new mutable.HashMap[String,StringBuilder]()

    var nextFieldId = getFieldCount(cls.getSuperclass.getName)

    for(b <- CtClass.primitiveTypes.toSeq :+ null) {
      val (pt, uname) = if(b != null) {
        (b.asInstanceOf[CtPrimitiveType].getDescriptor.toString, getUsableName(b))
      } else {
        ("A", "java.lang.Object")
      }
      if(pt != "V") {
        accessWrites += (pt -> new StringBuilder( s"""
           public void __dj_writeFieldID_${pt}(int id, ${uname} val) {
             if(id < ${nextFieldId}) {
              super.__dj_writeFieldID_${pt}(id, val);
             } else {
               switch(id) {
         """)
          )
        accessReads += (pt ->
          new StringBuilder( s"""
           public ${uname} __dj_readFieldID_${pt}(int id) {
             if(id < ${nextFieldId}) {
               return super.__dj_readFieldID_${pt}(id);
             } else {
               switch(id) {
         """))
      }
    }

    for (field <- cls.getDeclaredFields) {
      val name = field.getName
      //println("field name: " + name)
      // TODO: manage arrays
      if (!name.startsWith(config.fieldPrefix)/* && !field.getFieldInfo.getDescriptor.contains("[") */) {
        //val typ = field.getType

        val typ_name = getUsableName(field.getType)
        val modifiers = field.getModifiers

        val field_id = nextFieldId
        nextFieldId += 1


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

        val redirect_method_type = if(field.getType.isPrimitive) {
          field.getType.asInstanceOf[CtPrimitiveType].getDescriptor.toString
        } else "A"

        if (!Modifier.isStatic(modifiers) /*&& cls.getName.contains("StringIndexer")*/ ) {
          val write_method =
            s"""
                  static ${accessMod} void ``${config.fieldPrefix}write_field_${name}`` (${cls_name} self, ${typ_name} val) {
                    //edu.berkeley.dj.internal.InternalInterface.debug("writing field ${name}");
                    if((self.${cls_mode} & 0x02) != 0) {
                      self.${cls_manager}.writeField_${redirect_method_type}(${field_id}, ${if(redirect_method_type=="A") "(java.lang.Object)" else ""} val);
                    } else {
                      self.``${name}`` = val;
                    }
                  }
                  """
          val read_method =
            s"""
               static ${accessMod} ${typ_name} ``${config.fieldPrefix}read_field_${name}`` (${cls_name} self) {
                 //edu.berkeley.dj.internal.InternalInterface.debug("reading field ${name}");
                 if((self.${cls_mode} & 0x01) != 0) {
                   return (${typ_name})self.${cls_manager}.readField_${redirect_method_type}(${field_id});
                 } else {
                   return self.``${name}``;
                 }
               }
                  """
          try {
            //println("\t\tadding method for: " + name + " to " + cls.getName + " type " + typ_name)
            //println(write_method)
            cls.addMethod(CtMethod.make(write_method, cls))
            cls.addMethod(CtMethod.make(read_method, cls))
          } catch {
            // TODO: remove
            case ee: Throwable => {
              println("Compile of method failed: " + ee)
              throw ee
            }
          }
          // add this field to the accessor methods
          accessReads(redirect_method_type).append(s"case ${field_id}: return this.``${name}``;\n")
          accessWrites(redirect_method_type).append(s"case ${field_id}: this.``${name}`` = (${typ_name})val; return;\n")
        } else {
          // TODO: static field
          val static_write_method =
            s"""
               static ${accessMod} void ``${config.fieldPrefix}write_static_field_${name}`` (${typ_name} val) {
                 ${cls_name}.``${name}`` = val;
                 edu.berkeley.dj.internal.StaticFieldHelper.writeField_${redirect_method_type}("${cls.getName}::${name}", val);
               }
             """
          cls.addMethod(CtMethod.make(static_write_method, cls))
        }
      }
    }
    fieldCount.put(cls.getName, nextFieldId)

    try {
      // create the accessor methods
      for (t <- accessReads) {
        val f = t._2
        f.append(" default: throw new edu.berkeley.dj.internal.DJError(); } } }")
        cls.addMethod(CtMethod.make(f.toString, cls))
      }
      for (t <- accessWrites) {
        val f = t._2
        f.append(" default: throw new edu.berkeley.dj.internal.DJError(); } } }")
        cls.addMethod(CtMethod.make(f.toString, cls))
      }
    } catch {
      case ee: Throwable => {
        println("comp of methoded failed: "+ee)
        throw ee
      }
    }

  }

  private def addSeralizeMethods(cls: CtClass) = {
    // TODO:
    var serialize_obj_method =
      """
            public void __dj_serialize_obj(edu.berkeley.dj.internal.SerializeManager man) {
            super.__dj_serialize_obj(man);
      """
    var deserialize_obj_method =
      """
            public void __dj_deserialize_obj(edu.berkeley.dj.internal.SerializeManager man) {
            super.__dj_deserialize_obj(man);
      """
  }

  def modifyStaticInit(cls: CtClass): Unit = {
    if(cls.isInterface)
      return
    val clsinit = cls.makeClassInitializer()
    var addInitMethod = false
    var exists_init = false

    cls.getDeclaredFields.foreach(f => {
      if(Modifier.isStatic(f.getModifiers)) {
        addInitMethod = true
        // the jvm checks that final fields are set only in the static constructor
        // and we are renaming that method and optionally not calling it
        // so remove all the final field references
        // the compiler should have already check that we aren't modifying the final fields
        // so this shouldn't change the behavior of the program and only remove some possibly optimizations
        if(Modifier.isFinal(f.getModifiers)) {
          f.setModifiers(f.getModifiers & ~Modifier.FINAL)
        }
      }
    })

    if(clsinit != null) {
      clsinit.insertBefore(s"""if(!edu.berkeley.dj.internal.StaticFieldHelper.initStaticFields("${cls.getName}")) return;""")
    }
  }

  def addRPCRedirects(cls: CtClass): Unit = {
    val (cls_mode, cls_manager) = if (Modifier.isInterface(cls.getModifiers)) {
      ("__dj_getClassMode()", "__dj_getManager()")
    } else {
      ("__dj_class_mode", "__dj_class_manager")
    }


    val mode = manager.classMode.getMode(cls.getName)

    for(mth <- cls.getDeclaredMethods) {
      val modifiers = mth.getModifiers
      if (!Modifier.isStatic(modifiers) && !Modifier.isAbstract(modifiers) && !mth.getName.startsWith(config.fieldPrefix)) {
        val id = mth.getName + mth.getSignature
        if (mode.addMethodRedirect(id)) {
          val (return_cast, return_type) = if (mth.getReturnType.isPrimitive) {
            ("", mth.getReturnType.asInstanceOf[CtPrimitiveType].getDescriptor.toString)
          } else {
            (s"(${getUsableName(mth.getReturnType)})", "A")
          }
          val check = mode.addMethodRedirectCheck(id)
          val ctparams = mth.getParameterTypes
          val params = if (ctparams.length == 0) {
            "null"
          } else {
            s"""new String[] { ${ctparams.map(c => "\"" + c.getName + "\"").mkString(", ")} }"""
          }

          val code =
            s"""
             if((${cls_mode} & 0x20) != 0 ${if (check) s""" && edu.berkeley.dj.internal.RPCHelpers.checkPerformRPC("${cls.getName}","${id}") """ else ""}) {
              ${if(mth.getReturnType == CtClass.voidType) "" else s"return ${return_cast}"} edu.berkeley.dj.internal.RPCHelpers.call_${return_type} (this, "${cls.getName}", "${mth.getName}", ${params}, $$args);
              ${if(mth.getReturnType == CtClass.voidType) "return ;" else  "" }
             }
           """

          mth.insertBefore(code)
        }
      }
    }
  }

  def modifyArrays(cls: CtClass): Unit = {
    for(f <- cls.getDeclaredFields) {
      if(f.getType.isArray) {
        var typ = f.getType
        var cnt = 0
        while(typ.isArray) {
          typ = typ.asInstanceOf[CtArray].getComponentType
          cnt += 1
        }
        val cname = config.arrayprefix + typ.getName + "_" + cnt
        f.setType(runningPool.get(cname))
      }
    }
  }


  private def modifyClass(cls: CtClass): Unit = {
    //println("rewriting class: " + cls.getName)
    val mods = cls.getModifiers
    //println("modifiers: " + Modifier.toString(mods))
    reassociateClass(cls)

    modifyStaticInit(cls)
    rewriteUsedClasses(cls)
    addRPCRedirects(cls)
    modifyArrays(cls)
    transformClass(cls)
  }

  private def modifyInternalClass(cls: CtClass): Unit ={
    // the internal classes have special annotations on them to control how they are rwriten
    var clsa = cls
    // if the class is internal to something, we still want the annotations for the file to be "active"

    var rewriteUseAccessor = false
    val classMode = manager.classMode.getMode(cls.getName)

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
          case accessM: RewriteAddAccessorMethods => {
            addAccessorMethods(cls)
          }
          case _: RewriteUseAccessorMethods => {
            rewriteUseAccessor = true
          }
          case sp: SetSuperclass => {
            // force the super class to be something else
            if(clsa == cls)
              cls.setSuperclass(cls.getClassPool.get(sp.superclass()))
          }
          case _ => {} // nop
        }
      }
      clsa = clsa.getDeclaringClass
    }
    val codeConverter = new CodeConverter

    if(rewriteUseAccessor) {
      classMode.distributedCopies = true
      codeConverter.addTransform(new FieldAccess(codeConverter.prevTransforms, config, this))
    }

    cls.instrument(codeConverter)


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
      s"new ${getUsableName(cls.getComponentType)}[0]"
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
    // For now if
    // something is going to need a native method, we can just manually overwrite the native method
    // calls, otherwise this is going to end up becoming a really complicated system

    val rwMembers = cls.getDeclaredMethods.filter(m=>Modifier.isNative(m.getModifiers))

    rwMembers.foreach(m=> {
      cls.removeMethod(m)
    })

    val orgClassName = cls.getName.substring(config.coreprefix.length)

    rwMembers.foreach(m=>{
      //val args = getArguments(m.getSignature)
      val args = Descriptor.getParameterTypes(m.getSignature, cls.getClassPool)
      // TODO: deal with the fact we have stuff rewritten into internal.coreclazz namespace
      val static = if(Modifier.isStatic(m.getModifiers)) "static" else ""

      // ${if (m.getReturnType != CtClass.voidType) "return" else ""} ${makeDummyValue(m.getReturnType)} ;

      // work around javassist bug
      val (cls_types, arg_vals) = if(args.length == 0) {
        ("new java.lang.String[0]", "new java.lang.Object[0]")
      } else {
        (s"new ``java``.``lang``.``String`` [] { ${
          args.map(v => {
            if(v.isArray) {
              "\"" + Descriptor.toJvmName(v).replace('/', '.') + "\""
            } else {
              "\"" + v.getName + "\""
            }
          }).mkString(", ")
        } }",
          s"new java.lang.Object [] { ${args.zipWithIndex.map(v => {
            if(v._1.isPrimitive) {
              // we need to manually box this
              s"${v._1.asInstanceOf[CtPrimitiveType].getWrapperName}.valueOf( a${v._2} )"
            } else {
              s"a${v._2}"
            }
          }).mkString(", ")} }")
      }

      var rt_type = m.getReturnType

      // work around for javassist not automatically undoing boxing
      val (cast_prefix, cast_suffix) = if(rt_type.isPrimitive) {
        (s"((${rt_type.asInstanceOf[CtPrimitiveType].getWrapperName})", s").${rt_type.getName}Value()")
      } else {
        (s"(${getUsableName(rt_type)})", "")
      }

      val mth_code = s"""
           ${getAccessControl(m.getModifiers)} ${static} ${getUsableName(m.getReturnType)} ``${m.getName}`` (${args.zipWithIndex.map(v => getUsableName(v._1) + " a" + v._2).mkString(", ")}) {
             edu.berkeley.dj.internal.InternalInterface.getInternalInterface().simplePrint("\t\tcall native: ${cls.getName} ${m.getName}");
               ${if (rt_type != CtClass.voidType) s"return $cast_prefix " else ""}
               edu.berkeley.dj.internal.ProxyHelper.invokeProxy(
                 ${if(Modifier.isStatic(m.getModifiers)) "null" else "this"} ,
                 "${orgClassName}",
                 ${getUsableName(cls)}.class ,
                 $cls_types,
                 "${m.getName}",
                 $arg_vals
                 ) ${if(rt_type != CtClass.voidType) cast_suffix else "" } ;
           }
         """
      try {
        cls.addMethod(CtMethod.make(mth_code, cls))
      } catch {
        case e: Throwable => {
          println(e)
          throw e
        }
      }
    })
  }

  private def makeArrayClass(clsname: String, baseType: String, cnt: Int): CtClass = {
    val cls = runningPool.makeClass(clsname, runningPool.get(config.arrayprefix + "Base"))
    val wrapType = baseType match {
      case "Byte" => "byte"
      case s => s
    }

    if(cnt > 1) {
      cls.addField(CtField.make(s"private ${config.arrayprefix + baseType + "_" + (cnt - 1)} ir[];", cls))
    } else {
      cls.addField(CtField.make(s"private ${wrapType} ir[];", cls))
    }
    val length_mth =
      s"""
         public int length() { return ir.length; }
       """
    cls.addMethod(CtMethod.make(length_mth, cls))

    if(cnt > 1) {
      val get_mth =
        s"""
           public ${config.arrayprefix + baseType + "_" + (cnt - 1)} get(int i) {
             return ir[i];
           }
         """
      cls.addMethod(CtMethod.make(get_mth, cls))

      val set_mth =
        s"""
           public void (int i, ${config.arrayprefix + baseType + "_" (cnt - 1)} v) {
             ir[i] = v;
           }
          """
      cls.addMethod(CtMethod.make(set_mth, cls))
    } else {
      val get_mth =
        s"""
           public ${baseType} get(int i) {
             return ir[i];
           }
         """
      cls.addMethod(CtMethod.make(get_mth, cls))

      val set_mth =
        s"""
           public void set(int i, ${baseType} v) {
             ir[i] = v;
           }
         """
      cls.addMethod(CtMethod.make(set_mth, cls))
    }
    val static_constructor =
      s"""
         public static ${clsname} makeInstance_1(int i) {
           ${clsname} ret = new ${clsname}();
           ret.ir = new ${wrapType}[i];
           return ret;
         }
       """
    cls.addMethod(CtMethod.make(static_constructor, cls))

    cls
  }

  private def checkIsAThrowable(cls: CtClass): Boolean = {
    var at = cls
    while(at != null) {
      if(at.getName == "java.lang.Throwable") {
        return true
      }
      at = at.getSuperclass
    }
    false
  }

  private def findBaseClass(classname: String): CtClass = {
    try {
      basePool get classname
    } catch {
      case e: NotFoundException => {
        try {
          if(classname.startsWith(config.coreprefix)) {
            val c = basePool get (classname + "00")
            c.setName(classname)
            c
          } else null
        } catch {
          case e: NotFoundException => {
            if(classname.contains("$")) {
              // There is some dollar sign in the class name so try to change the containing class
              try {
                val c = basePool.get(classname.replaceAll("(\\.[^\\.\\$]+)\\$", "$100\\$"))
                c.setName(classname)
                c
              } catch {
                case e: NotFoundException => null
              }
            } else null
          }
        }
      }
    }
  }

  def createCtClass(classname: String): CtClass = {
    MethodInfo.doPreverify = true

    if(classname.startsWith(config.coreprefix) && classname.endsWith("00")) {
      // we should not loading these classes with 00 suffix
      // something somewhere else in the rewriter must have gone wrong to cause us to load this
      throw new ClassNotFoundException(classname)
    }

    if (classname.startsWith("edu.berkeley.dj.rt")) {
      // do not allow loading the runtime into the runtime
      throw new ClassNotFoundException(classname)
    }

    if(classname.endsWith("[]")) {
      // this is some array type, so treat it as such
      // in the future we should rewrite arrays with our methods,
      return new CtArray(classname, runningPool)
    }


    val cls = findBaseClass(classname)
    //println("create class name:" + classname)


    if(classname.startsWith(config.arrayprefix)) {
      if(cls != null) {
        reassociateClass(cls)
        modifyInternalClass(cls)
        return cls
      }
      val uindx = classname.lastIndexOf("_")
      val sp = classname.substring(0, uindx).drop(config.arrayprefix.size)
      val cnt = classname.substring(uindx + 1).toInt
      return makeArrayClass(classname, sp, cnt)

    }

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
      if(!checkIsAThrowable(cls))
        modifyClass(cls)
      else {
        println("??")
      }
    } else if (classname.startsWith(config.internalPrefix)) {
      modifyInternalClass(cls)
    }
    cls
  }

}
