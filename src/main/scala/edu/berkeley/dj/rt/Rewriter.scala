package edu.berkeley.dj.rt

import java.lang.StringBuilder
import java.lang.reflect.UndeclaredThrowableException
import javassist._
import javassist.bytecode.analysis.{Frame, Analyzer}
import javassist.bytecode.{BadBytecode, Descriptor, MethodInfo}

import edu.berkeley.dj.internal._
import edu.berkeley.dj.internal.coreclazz.RewriteLocalFieldOnly
import edu.berkeley.dj.rt.convert.{CodeConverter, _}
import edu.berkeley.dj.utils.Memo

import scala.collection.mutable


/**
 * Created by matthewfl
 */

private[rt] trait RewriterInterface {

  def createCtClass(classname: String, addToCache: CtClass => Unit): CtClass

}

private[rt] class Rewriter (private val manager : MasterManager) extends RewriterInterface {

  def config = manager.config

  def basePool = manager.pool

  def runningPool = manager.runningPool

  def classMode = manager.classMode

//  private lazy val moveInterface = runningPool.get("edu.berkeley.dj.internal.Movable")

  //private lazy val proxyInterface = runningPool.get("edu.berkeley.dj.internal.Proxied")

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
    ("getPrimitiveClass", "(Ljava/lang/String;)Ljava/lang/Class;", "java.lang.Class") -> ("getPrimitiveClass", s"${config.internalPrefix}AugmentedClassLoader"),
    ("desiredAssertionStatus", "()Z", "java.lang.Class") -> ("desiredAssertionStatus", s"${config.internalPrefix}AugmentedClassLoader"),
    ("getComponentType", "()Ljava/lang/Class;", "java.lang.Class") -> ("getComponentType", s"${config.internalPrefix}AugmentedClassLoader"),
    ("getDeclaredFields", "()Ledu/berkeley/dj/internal/arrayclazz/java/lang/reflect/Field_1;", "java.lang.Class") -> ("getDeclaredFields", s"${config.internalPrefix}AugmentedClassLoader"),
    ("getResourceAsStream", "(Ljava/lang/String;)Ledu/berkeley/dj/internal/coreclazz/java/io/InputStream;", "java.lang.Class") -> ("getResourceAsStream", s"${config.internalPrefix}AugmentedClassLoader"),

    // reflection stuff
    // don't replace this method since it need to look at the call stack directly, and it is static so doesn't matter
    ("getCallerClass", "()Ljava/lang/Class;", "edu.berkeley.dj.internal.coreclazz.sun.reflect.Reflection") -> ("getCallerClass", s"${config.internalPrefix}ReflectionHelper"),

    // some string stuff
    ("getChars", "(IILedu/berkeley/dj/internal/arrayclazz/Character_1;I)V", "java.lang.String") -> ("getChars", s"${config.internalPrefix}AugmentedString"),
    ("toCharArray", "()Ledu/berkeley/dj/internal/arrayclazz/Character_1;", "java.lang.String") -> ("toCharArray", s"${config.internalPrefix}AugmentedString"),

    // array internal stuff
  // TODO: this method is the wrong one to replace since it is internal to the reflect class which we are not rewriting....
    ("newInstance", "(Ljava/lang/Class;I)Ljava/lang/Object;", "java.lang.reflect.Array") -> ("newInstance", s"${config.internalPrefix}ArrayHelpers"),
    ("newInstance", "(Ljava/lang/Class;Ledu/berkeley/dj/internal/arrayclazz/Integer_1;)Ljava/lang/Object;", "java.lang.reflect.Array") -> ("newInstance", s"${config.internalPrefix}ArrayHelpers"),

  // throwable
    ("printStackTrace", "(Ledu/berkeley/dj/internal/coreclazz/java/io/PrintStream;)V", "java.lang.Throwable") -> ("printStackTrace", s"${config.internalPrefix}ExceptionHelper")

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

    //cls.addInterface(moveInterface)
    val codeConverter = new CodeConverter

    codeConverter.addTransform(new FunctionCalls(codeConverter.prevTransforms, rewriteMethodCalls))
    codeConverter.addTransform(new SpecialConverter(codeConverter.prevTransforms))

    //if(cls.getName.contains("Scratch")) // TODO: remove
    //  codeConverter.addTransform(new Arrays(codeConverter.prevTransforms, config))

    codeConverter.addTransform(new FieldAccess(codeConverter.prevTransforms, config, this))
    codeConverter.addTransform(new Monitors(codeConverter.prevTransforms))

    val isInterface = Modifier.isInterface(cls.getModifiers)

    // basically if some exception type and not inherited from ObjectBase
    val canRewrite = canRewriteClass(cls.getName)
    //val cls_name = getUsuableName(cls)

    cls.instrument(codeConverter)

    if (!isInterface && canRewrite) {
      addAccessorMethods(cls)
      addSerializeMethods(cls)
    }
    // TODO: need to handle interfaces that can have methods on them
  }

  private def monitorMethodsRewrite(cls: CtClass): Unit = {
    for(m <- cls.getDeclaredMethods) {
      if(Modifier.isSynchronized(m.getModifiers)) {
      }

    }
    // TODO:
    ???
  }

  private def baseFieldCount = 10

  private val fieldCount = new mutable.HashMap[String,Int]()

  // maybe use the source pool to determine the number of fields that a class has
  private def getFieldCount(classname: String): Int = {
    if(classname == "edu.berkeley.dj.internal.ObjectBase")
      baseFieldCount
    else {
      if(fieldCount.contains(classname)) {
        fieldCount.get(classname).get
      } else {
        val c = runningPool.get(classname)
        if(!fieldCount.contains(classname)) {
          // there must be some circular defintion for this class where it is importing itself
          //if(classname.startsWith(config.coreprefix)) {
            // we can compute the number of fields that this class should have
            val supC = getFieldCount(c.getSuperclass.getName)
            val selfC = c.getDeclaredFields.length
            fieldCount.put(classname, supC + selfC)
            return supC + selfC
          //} else ???
        }
        fieldCount.get(classname).get
      }
    }
  }

//  private def getFieldCount2(classname: String): Int = {
//    if(classname == "edu.berkeley.dj.internal.ObjectBase" || classname == "java.lang.Object")
//      baseFieldCount
//    else {
//      if(fieldCount.contains(classname)) {
//        fieldCount.get(classname).get
//      } else {
//        val cls = basePool.get(classname)
//        val r = cls.getDeclaredFields.length + getFieldCount2(cls.getSuperclass.getName)
//        fieldCount.update(classname, r)
//        r
//      }
//    }
//  }

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

    //runningPool.get(cls.getSuperclass.getName) // make sure that the super class is loaded so that we know the field count
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
          field.setModifiers(modifiers & ~Modifier.FINAL)
        }

        val redirect_method_type = if(field.getType.isPrimitive) {
          field.getType.asInstanceOf[CtPrimitiveType].getDescriptor.toString
        } else "A"

        val isLocalOnly = field.getAnnotation(classOf[RewriteLocalFieldOnly]) != null

        if (!Modifier.isStatic(modifiers) /*&& cls.getName.contains("StringIndexer")*/ ) {
          val (write_method, read_method) = if(!isLocalOnly) {
            (// write method
              s"""
                  static ${accessMod} void ``${config.fieldPrefix}write_field_${name}`` (${cls_name} self, ${typ_name} val) {
                    //edu.berkeley.dj.internal.InternalInterface.debug("writing field ${name}");
                    if((self.${cls_mode} & 0x02) != 0) {
                      self.${cls_manager}.writeField_${redirect_method_type}(${field_id}, ${if(redirect_method_type=="A") "(java.lang.Object)" else ""} val);
                    } else {
                      self.``${name}`` = val;
                    }
                  }
                  """,
              // read method
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
              )
          } else {
            (// write method
              s"""
                  static ${accessMod} void ``${config.fieldPrefix}write_field_${name}`` (${cls_name} self, ${typ_name} val) {
                    self.``${name}`` = val;
                  }
             """,
              // read method
              s"""
                 static ${accessMod} ${typ_name} ``${config.fieldPrefix}read_field_${name}`` (${cls_name} self) {
                   return self.``${name}``;
                 }
               """)
          }

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
    if(fieldCount.contains(cls.getName)) {
      assert(fieldCount.get(cls.getName).get == nextFieldId)
    } else {
      fieldCount.put(cls.getName, nextFieldId)
    }

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

  private def addSerializeMethods(cls: CtClass) = {
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
    var empty_obj_method =
    """
      public void __dj_empty_obj() {
        super.__dj_empty_obj();
    """

    var prim_size = 0
    var num_objs = 0
    for(f <- cls.getDeclaredFields) {
      if(!Modifier.isStatic(f.getModifiers)) {
        if (f.getType.isPrimitive) {
          prim_size += f.getType.asInstanceOf[CtPrimitiveType].getDataSize
        } else {
          num_objs += 1
        }
      }
    }
    serialize_obj_method +=   s"man.register_size(${prim_size}, ${num_objs});\n"
    deserialize_obj_method += s"man.register_size(${prim_size}, ${num_objs});\n"
    for(f <- cls.getDeclaredFields) {
      if(!Modifier.isStatic(f.getModifiers)) {
        if (f.getType.isPrimitive) {
          val p = f.getType.asInstanceOf[CtPrimitiveType]
          serialize_obj_method += s"man.put_value_${p.getDescriptor} ( this.${f.getName} );\n"
          deserialize_obj_method += s"this.${f.getName} = man.get_value_${p.getDescriptor} ();\n"
        }
      }
    }
    for(f <- cls.getDeclaredFields) {
      if(!Modifier.isStatic(f.getModifiers)) {
        if (!f.getType.isPrimitive) {
          serialize_obj_method += s"this.${f.getName} = (${getUsableName(f.getType)}) man.put_object( this.${f.getName} );\n"
          deserialize_obj_method += s"this.${f.getName} = (${getUsableName(f.getType)}) man.get_object( this.${f.getName} ); \n"
          empty_obj_method += s"this.${f.getName} = null; \n"
        }
      }
    }

    serialize_obj_method += "}"
    deserialize_obj_method += "}"
    empty_obj_method += "}"

    try {
      cls.addMethod(CtMethod.make(serialize_obj_method, cls))
      cls.addMethod(CtMethod.make(deserialize_obj_method, cls))
      cls.addMethod(CtMethod.make(empty_obj_method, cls))
    } catch {
      case e: Throwable => {
        println(e)
        throw e
      }
    }

  }

  def modifyStaticInit(cls: CtClass, mana: MethodAnalysis): Unit = {
    if(cls.isInterface)
      return
    // the method init is already going to exist in the case that there are static variables
    // that are getting values set, this will either get that method or create a new one
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

    // TODO: need to record how big the gap that we are inserting is

    if(clsinit != null) {
      if(!mana.m.contains(clsinit.getMethodInfo)) {
        mana.addMethod(cls, clsinit.getMethodInfo)
      }
      val gap = clsinit.insertBefore(
        s"""if(!edu.berkeley.dj.internal.StaticFieldHelper.initStaticFields("${cls.getName}")) return;""")
      mana.addOffset(clsinit.getMethodInfo, 0, gap.length)
    }
  }

  def addRPCRedirects(cls: CtClass): Unit = {
    val (cls_mode, cls_manager) = if (Modifier.isInterface(cls.getModifiers)) {
      ("this.__dj_getClassMode()", "this.__dj_getManager()")
    } else {
      ("this.__dj_class_mode", "this.__dj_class_manager")
    }


    val mode = manager.classMode.getMode(cls.getName)

    // TODO: remove fcking hack
    // this causes something internally to get cached, without it this crashes
    cls.toString

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

          //val ctparams = mth.getParameterTypes
          val ctparams = mth.getParameterTypesNames
          val params = if (ctparams.length == 0) {
            "null"
          } else {
            s"""new String[] { ${ctparams.map(c => "\"" + c + "\"").mkString(", ")} }"""
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

  def modifyArrays(cls: CtClass, mana: MethodAnalysis): Unit = {
    val codeConverter = new CodeConverter
    codeConverter.addTransform(new Arrays(codeConverter.prevTransforms, config, mana, jclassmap, this))

    cls.replaceClassName(new ArrayClassMap(this))

    for(i <- 0 until cls.getClassFile.getConstPool.getSize) {
      try {
        val ci = cls.getClassFile.getConstPool.getClassInfo(i)
        if(ci != null && ci.contains(";") && ci.startsWith("L")) {
          println("fail2 "+ci)
        }
      } catch { case _: ClassCastException => {}}
    }

    cls.instrument(codeConverter)

    for(i <- 0 until cls.getClassFile.getConstPool.getSize) {
      try {
        val ci = cls.getClassFile.getConstPool.getClassInfo(i)
        if(ci != null && ci.contains(";") && ci.startsWith("L")) {
          println("fail "+ci)
        }
      } catch { case _: ClassCastException => {}}
    }

  }

  private val arrayTypeRegex = """(\[+)([ZCBSIJFD]|L[^;]+?;)""".r

  // TODO: this is crashing for some reason inside the scala lib implementation
  /*private[rt] def rewriteArrayTypeFail(typ: String, addL: Boolean) = {
    arrayTypeRegex.replaceAllIn(typ, mt => {
      val arr_depth = mt.group(1).length
      val typ = if(mt.group(2).length == 1) {
        mt.group(2) match {
          case "Z" => "Boolean"
          case "C" => "Character"
          case "B" => "Byte"
          case "I" => "Integer"
          case "J" => "Long"
          case "F" => "Float"
          case "D" => "Double"
          case "S" => "Short"
          case _ => throw new NotImplementedError()
        }
      } else {
        val s = mt.group(2)
        s.substring(1, s.length - 1)//.replace("/", ".")
      }
      val r = (config.arrayprefix + typ + "_" + arr_depth).replace(".", "/")
      //"L"+
      if(addL)
        s"L$r;"
      else
        r
    })
  }*/

  // fcking haxs, some issue sometimes with the scala findallandreplace
  private[rt] def rewriteArrayType(typ: String, addL: Boolean) = {
    val matches = arrayTypeRegex.findAllMatchIn(typ)
    var ret = typ
    for(mt <- matches) {
      val arr_depth = mt.group(1).length
      val typ = if(mt.group(2).length == 1) {
        mt.group(2) match {
          case "Z" => "Boolean"
          case "C" => "Character"
          case "B" => "Byte"
          case "I" => "Integer"
          case "J" => "Long"
          case "F" => "Float"
          case "D" => "Double"
          case "S" => "Short"
          case _ => ???
        }
      } else {
        val s = mt.group(2)
        s.substring(1, s.length - 1)//.replace("/", ".")
      }
      val typ_mapped = {
        val r = jclassmap.get(typ.replace('.', '/'))
        if(r != null)
          r.asInstanceOf[String]
        else
          typ
      }
      val r = (config.arrayprefix + typ_mapped + "_" + arr_depth).replace(".", "/")
      //"L"+
      val rs = if(addL)
        s"L$r;"
      else
        r
      //println(ret)
      // TODO: this is wrong and may fail in a case like [I[[I
      ret = ret.replace(mt.group(0), rs)
      //println(ret)
    }
    //println(ret)
    ret
  }

  private def isInheritedFromBase(cls: CtClass) = {
    /*if(cls.isInterface) {
      cls.subtypeOf(objectBaseInterface)
    } else {
      cls.subtypeOf()
    }*/
    cls.subtypeOf(objectBaseInterface)
  }

  private def makePublic(cls: CtClass): Unit = {
    cls.setModifiers((cls.getModifiers | Modifier.PUBLIC) & ~(Modifier.PRIVATE | Modifier.PROTECTED))
  }

  /*private def addMethods(cls: CtClass, methods: Iterable[CtMethod]): Unit = {
    for(m <- methods) {
      cls.addMethod(m)
    }
  }*/

  private def modifyClass(cls: CtClass, mana: MethodAnalysis, overrideNative: Boolean=false): Unit = {
    //println("rewriting class: " + cls.getName)
    val mods = cls.getModifiers
    //println("modifiers: " + Modifier.toString(mods))

    //val addedMethods = new collection.mutable.MutableList[CtMethod]()

    val nativeM = if(overrideNative && hasNativeMethods(cls)) {
      findNativeMethodsToOverwrite(cls)
      //addedMethods ++= makeOverwrittenNativeMethods(cls)
    } else null

    reassociateClass(cls)
    makePublic(cls)

    modifyStaticInit(cls, mana)
    rewriteUsedClasses(cls)
    if (isInheritedFromBase(cls)) {
      modifyArrays(cls, mana)
      addRPCRedirects(cls)
      if(nativeM != null)
        addOverwrittenNativeMethods(cls, nativeM)
      //addMethods(cls, addedMethods)
      transformClass(cls)
    } else {
      // this is really annoying, some classes are not inherited from objectbase
      // and if we end up trying to work with them then they will end up not being able to find
      // the fields on objectbase that it needs for various operations to work
      System.err.println("cls is not inherited from objectbase and we are trying to work with it: "+cls.getName)
      ???
    }
  }

  private def modifyInternalClass(cls: CtClass/*, mana: MethodAnalysis*/): Unit ={
    // the internal classes have special annotations on them to control how they are rwriten
    var clsa = cls
    // if the class is internal to something, we still want the annotations for the file to be "active"

    lazy val mana = getMethodAnalysis(cls)

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
              cls.replaceClassName(nrw.oldCls().getName, nrw.newName())
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
          case sp: RewriteSetSuperclass => {
            // force the super class to be something else
            if(clsa == cls)
              cls.setSuperclass(cls.getClassPool.get(sp.superclass()))
          }
          case _: RewriteAddArrayWrap => {
            addArrayWrapMethods(cls)
          }
          case _: RewriteAddSerialization => {
            addSerializeMethods(cls)
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

  private def arrayWrapMethod(cls: CtClass, m: CtMethod): CtMethod = {
    if(!m.getSignature.contains("["))
      return null// we do not need to wrap this method

    val (rt, rt_cast) = {
      if(m.getReturnType.isArray) {
        val arr = cls.getClassPool.get(rewriteArrayType(Descriptor.toJvmName(m.getReturnType), false).replace('/','.'))
        (arr, s"(${arr.getName}) ${config.internalPrefix}ArrayHelpers.makeDJArray ")
      } else {
        (m.getReturnType, "")
      }
    }
    val argsM = m.getParameterTypes.map(a => {
      if(a.isArray) {
        (cls.getClassPool.get(rewriteArrayType(Descriptor.toJvmName(a), false).replace('/','.')), true, a)
      } else (a, false, null)
    })
    val body =
      s"""
             {
               return $rt_cast (${m.getName} (${
        argsM.zipWithIndex.map(a => {
          if(a._1._2 == false) {
            "$"+(a._2+1)
          } else {
            "("+a._1._3.getName+ ")" +config.internalPrefix + "ArrayHelpers.makeNativeArray( $"+(a._2+1) +")"
          }
        }).mkString(", ")
      })) ;
             }
           """
    // hack with name so that we call the origional method even if we only differ in return type
    val ret = CtNewMethod.make(m.getModifiers, rt, m.getName + "_new_name", argsM.map(_._1), m.getExceptionTypes, body, cls)
    ret.setName(m.getName)
    ret
  }

  private def addArrayWrapMethods(cls: CtClass): Unit = {
    for(m <- cls.getDeclaredMethods) {
      //if(m.getSignature.contains("[")) {
        // we need to add another method to wrap this
      val r = arrayWrapMethod(cls, m)
      if(r != null)
        cls.addMethod(r)
    //}
    }
  }

  private def hasNativeMethods(cls: CtClass): Boolean = {
    // if this clsas contains some native methods then we can't
    // directly instaniate this class, as it will need the native methods
    // so instead we will make proxy methods for all of its public and protected methods
    for(m <- cls.getDeclaredMethods) {
      if(Modifier.isNative(m.getModifiers))
        return true
    }
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

  private def findNativeMethodsToOverwrite(cls: CtClass) = {
    val rwMembers = cls.getDeclaredMethods.filter(m=>Modifier.isNative(m.getModifiers))

    if(rwMembers.isEmpty) {
      null
    } else {
      rwMembers.foreach(m => {
        cls.removeMethod(m)
      })
      rwMembers
    }
  }

  private def addOverwrittenNativeMethods(cls: CtClass, rwMembers: Iterable[CtMethod]) = {

    val orgClassName = cls.getName.substring(config.coreprefix.length)

    for(m <- rwMembers) {
      //val args = getArguments(m.getSignature)
      val args = Descriptor.getParameterTypes(m.getSignature, cls.getClassPool)
      // TODO: deal with the fact we have stuff rewritten into internal.coreclazz namespace
      val static = if (Modifier.isStatic(m.getModifiers)) "static" else ""

      // ${if (m.getReturnType != CtClass.voidType) "return" else ""} ${makeDummyValue(m.getReturnType)} ;

      // work around javassist bug
      val (cls_types, arg_vals) = if (args.length == 0) {
        ("new java.lang.String[0]", "new java.lang.Object[0]")
      } else {
        (s"new ``java``.``lang``.``String`` [] { ${
          args.map(v => {
            if (v.getName.startsWith(config.arrayprefix)) {
              ??? // this needs to determine the origional array type
            } else if (v.isArray) {
              //??? // how are we now getting an array at this point
              "\"" + Descriptor.toJvmName(v).replace('/', '.') + "\""
            } else {
              "\"" + v.getName + "\""
            }
          }).mkString(", ")
        } }",
          s"new java.lang.Object [] { ${
            args.zipWithIndex.map(v => {
              if (v._1.isPrimitive) {
                // we need to manually box this
                s"${v._1.asInstanceOf[CtPrimitiveType].getWrapperName}.valueOf( a${v._2} )"
              } else {
                s"a${v._2}"
              }
            }).mkString(", ")
          } }")
      }

      var rt_type = m.getReturnType

      // work around for javassist not automatically undoing boxing
      val (cast_prefix, cast_suffix) = if (rt_type.isPrimitive) {
        (s"((edu.berkeley.dj.internal.coreclazz.${rt_type.asInstanceOf[CtPrimitiveType].getWrapperName})", s").${rt_type.getName}Value()")
      } else {
        (s"(${getUsableName(rt_type)})", "")
      }

      val mth_code = s"""
           ${getAccessControl(m.getModifiers)} ${static} ${getUsableName(m.getReturnType)} ``${m.getName}`` (${args.zipWithIndex.map(v => getUsableName(v._1) + " a" + v._2).mkString(", ")}) {
             edu.berkeley.dj.internal.InternalInterface.getInternalInterface().simplePrint("\t\tcall native: ${cls.getName} ${m.getName}");
               ${if (rt_type != CtClass.voidType) s"return $cast_prefix " else ""}
               edu.berkeley.dj.internal.ProxyHelper.invokeProxy(
                 ${if (Modifier.isStatic(m.getModifiers)) "null" else "this"} ,
                 "${orgClassName}",
                 ${getUsableName(cls)}.class ,
                 $cls_types,
                 "${m.getName}",
                 $arg_vals
                 ) ${if (rt_type != CtClass.voidType) cast_suffix else ""} ;
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
    }
  }


  /*private def makeOverwrittenNativeMethods(cls: CtClass): Iterable[CtMethod] = {
    // For now if
    // something is going to need a native method, we can just manually overwrite the native method
    // calls, otherwise this is going to end up becoming a really complicated system

    val rwMembers = cls.getDeclaredMethods.filter(m=>Modifier.isNative(m.getModifiers))

    rwMembers.foreach(m=> {
      cls.removeMethod(m)
    })

    val orgClassName = cls.getName.substring(config.coreprefix.length)

    //rwMembers.foreach(m=>{
    for(m <- rwMembers) yield {
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
            if(v.getName.startsWith(config.arrayprefix)) {
              ??? // this needs to determine the origional array type
            } else if(v.isArray) {
              //??? // how are we now getting an array at this point
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
      /*try {
        cls.addMethod(CtMethod.make(mth_code, cls))
      } catch {
        case e: Throwable => {
          println(e)
          throw e
        }
      }*/
      CtMethod.make(mth_code, cls)
    }
  }*/

  private lazy val baseArrayClsImpl = runningPool.get(config.arrayprefix + "Base_impl")
  private lazy val baseArrayClsInter = runningPool.get(config.arrayprefix + "Base")

  private def makeArrayClass(clsname: String): CtClass = {
    val uindx = clsname.lastIndexOf("_")
    val cnt = clsname.substring(uindx + 1).toInt
    val makeInterface = !clsname.endsWith(s"_impl_$cnt")
    val baseType = if(makeInterface) {
      clsname.substring(config.arrayprefix.size, uindx)
    } else {
      clsname.substring(config.arrayprefix.size, uindx - 5)
    }

    val (wrapType, jvmtyp, isPrimitive, primType) = baseType match {
      case "Byte" => ("byte", "B", true, CtClass.byteType)
      case "Character" => ("char", "C", true, CtClass.charType)
      case "Short" => ("short", "S", true, CtClass.shortType)
      case "Integer" => ("int", "I", true, CtClass.intType)
      case "Long" => ("long", "J", true, CtClass.longType)
      case "Float" => ("float", "F", true, CtClass.floatType)
      case "Double" => ("double", "D", true, CtClass.doubleType)
      case "Boolean" => ("boolean", "Z", true, CtClass.booleanType)
      case s => {
        (s, "A", false, null)
      }
    }

    def augName(n: String) = n.replaceAll("[^A-Za-z0-9]", "_")

    val cls = if(makeInterface) {
      runningPool.makeInterface(clsname, baseArrayClsInter)
    } else {
      val c = runningPool.makeClass(clsname, baseArrayClsImpl)
      c.setModifiers(c.getModifiers | Modifier.FINAL)
      c
    }


    val allInheritedTypes = new mutable.HashSet[String]()
    def addInterfaces(inter: CtClass): Unit = {
      val cn = inter.getName
      val uidx = cn.lastIndexOf("_")
      if(uidx < 0) return
      val btype = cn.substring(config.arrayprefix.size, uidx)
      if(!allInheritedTypes.contains(btype) && cn.startsWith(config.arrayprefix)) {
        allInheritedTypes += btype
        for (i <- inter.getInterfaces)
          addInterfaces(i)
      }
    }

    //val intercls =
    if(!makeInterface) {
      // add the interface of the type that this class is going to implement
      val i = runningPool.get(config.arrayprefix + baseType + "_" + cnt)
      cls.addInterface(i)
      if(baseType != "java.lang.Object")
        cls.addInterface(runningPool.get(config.arrayprefix + "java.lang.Object_"+cnt))
      if(!isPrimitive)
        addInterfaces(i)
      i
    } else {
      // add all the classes that this implements and inherits from
      if(!isPrimitive && baseType != "java.lang.Object") {
        val btype = runningPool.get(baseType)
        val stype = btype.getSuperclass
        if(stype.getName != "java.lang.Object") {
          cls.addInterface(runningPool.get(config.arrayprefix + stype.getName + "_"+cnt))
        }
        for(i <- btype.getInterfaces) {
          val n = i.getName
          if(n != s"${config.coreprefix}java.lang.Object")
            cls.addInterface(runningPool.get(config.arrayprefix + n + "_" + cnt))
        }
      }
    }

    if(isPrimitive)
      allInheritedTypes += wrapType
    else
      allInheritedTypes += baseType


    val inner_arr_typ = if(cnt > 1)
      config.arrayprefix + baseType + "_" + (cnt - 1)
    else
      wrapType
    if(!makeInterface) {
      cls.addField(CtField.make(s"public ${inner_arr_typ}[] ir;", cls))
    }
    val length_mth =
      s"""
         public int length() {
           if((__dj_class_mode & 0x01) != 0) {
             return __dj_class_manager.readField_I(9);
           } else {
             return ir.length;
           }
         }
       """
    val length_mth_int = "int length();"
    if(makeInterface)
      //cls.addMethod(CtMethod.make(length_mth_int, cls))
    {} else
      cls.addMethod(CtMethod.make(length_mth, cls))

    if(cnt > 1) {
      val typname = config.arrayprefix + baseType + "_" + (cnt - 1)
      if(makeInterface) {
        val get_mth_int = s"${typname} get_${augName(typname)}(int i);"
        cls.addMethod(CtMethod.make(get_mth_int, cls))
      } else {
        val get_mth =
          s"""
           public ${typname} get_${augName(typname)}(int i) {
             if((__dj_class_mode & 0x01) != 0) {
               return (${typname}) __dj_class_manager.readField_A(i + ${baseFieldCount});
             } else {
               return ir[i];
             }
           }
         """
        cls.addMethod(CtMethod.make(get_mth, cls))
      }

      if(makeInterface) {
        val set_mth_int = s"void set_${augName(typname)}(int i, ${typname} v);"
        cls.addMethod(CtMethod.make(set_mth_int, cls))
      } else {
        val set_mth =
          s"""
           public void set_${augName(typname)}(int i, ${typname} v) {
             if((__dj_class_mode & 0x02) != 0) {
               __dj_class_manager.writeField_A(i + ${baseFieldCount}, v);
             } else {
               ir[i] = v;
             }
           }
          """
        cls.addMethod(CtMethod.make(set_mth, cls))
      }

      if(!makeInterface) {
        val dj_write_mth =
          s"""
           public void __dj_writeFieldID_A(int id, Object v) {
             if(id < ${baseFieldCount}) {
               super.__dj_writeFieldID_A(id, v);
             } else {
               ir[id - ${baseFieldCount}] = (${typname})v;
             }
           }
         """
        cls.addMethod(CtMethod.make(dj_write_mth, cls))

        val dj_read_mth =
          s"""
           public Object __dj_readFieldID_A(int id) {
             if(id < ${baseFieldCount}) {
               return super.__dj_readFieldID_A(id);
             } else {
               return ir[id - ${baseFieldCount}];
             }
           }
         """
        cls.addMethod(CtMethod.make(dj_read_mth, cls))

      }

    } else {

      val refname = if(isPrimitive) wrapType else "Object"

      if(makeInterface) {
        val get_mth_int = s"${wrapType} get_${augName(wrapType)}(int i);"
        cls.addMethod(CtMethod.make(get_mth_int, cls))
      } else {
        for(itype <- allInheritedTypes) {
          val get_mth =
            s"""
            public ${itype} get_${augName(itype)}(int i) {
             if((__dj_class_mode & 0x1) != 0) {
               return (${wrapType}) __dj_class_manager.readField_${jvmtyp}(i + ${baseFieldCount});
             } else {
               return ir[i];
             }
           }
         """
          cls.addMethod(CtMethod.make(get_mth, cls))
        }

        val get_mth_obj =
          s"""
             public Object get_java_lang_Object(int i) {
               if((__dj_class_mode & 0x1) != 0) {
                 return ${if(isPrimitive) (s"java.lang.$baseType.valueOf") else "" } (__dj_class_manager.readField_${jvmtyp}(i + ${baseFieldCount}));
               } else {
                 return ${if(isPrimitive) (s"java.lang.$baseType.valueOf") else "" } (ir[i]);
               }
             }
           """

        if(!allInheritedTypes.contains("java.lang.Object"))
          cls.addMethod(CtMethod.make(get_mth_obj, cls))
      }

      if(makeInterface) {
        val set_mth_int = s"void set_${augName(wrapType)}(int i, ${wrapType} v);"
        cls.addMethod(CtMethod.make(set_mth_int, cls))
      } else {
        for(itype <- allInheritedTypes) {
          val set_mth =
            s"""
             public void set_${augName(itype)} (int i, ${itype} v) {
             if((__dj_class_mode & 0x2) != 0) {
               __dj_class_manager.writeField_${jvmtyp}(i + ${baseFieldCount}, v);
             } else {
               ir[i] = (${wrapType})v;
             }
           }
         """
          cls.addMethod(CtMethod.make(set_mth, cls))
        }

        val set_mth_obj =
          s"""
             public void set_java_lang_Object(int i, Object v) {
               if((__dj_class_mode & 0x2) != 0) {
                 __dj_class_manager.writeField_${jvmtyp}(i + ${baseFieldCount}, ( ${if(isPrimitive) s"((${baseType})v).${wrapType}Value()" else "v"} ) );
               } else {
                 ir[i] = (${wrapType}) ( ${if(isPrimitive) s"((${baseType})v).${wrapType}Value()" else "v"} ) ;
               }
             }
           """
        if(!allInheritedTypes.contains("java.lang.Object"))
          cls.addMethod(CtMethod.make(set_mth_obj, cls))
      }

      if(!makeInterface) {
        val dj_read_mth =
          s"""
           public ${refname} __dj_readFieldID_${jvmtyp} (int id) {
             if(id < ${baseFieldCount}) {
               return super.__dj_readFieldID_${jvmtyp}(id);
             } else {
               return ir[id - ${baseFieldCount}];
             }
           }
         """
        cls.addMethod(CtMethod.make(dj_read_mth, cls))

        val dj_write_mth =
          s"""
           public void __dj_writeFieldID_${jvmtyp} (int id, ${refname} val) {
             if(id < ${baseFieldCount}) {
               super.__dj_writeFieldID_${jvmtyp}(id, val);
             } else {
               ir[id - ${baseFieldCount}] = val;
             }
           }
         """
        cls.addMethod(CtMethod.make(dj_write_mth, cls))
      }
    }
    if(!makeInterface) {
      cls.addConstructor(CtNewConstructor.defaultConstructor(cls))

      // the constructors will have to go onto the classes since javassist doesn't support having static methods
      // on interfaces
      val inter_name = config.arrayprefix + baseType + "_" + cnt
      val static_constructor =
        s"""
         public static ${inter_name} newInstance_1(int i) {
           ${clsname} ret = new ${clsname}();
           ret.ir = new ${inner_arr_typ}[i];
           return ret;
         }
       """
      cls.addMethod(CtMethod.make(static_constructor, cls))


      // serialization methods

      val serialize_helper = s"""
        public void __dj_serialize_obj(edu.berkeley.dj.internal.SerializeManager man) {
          super.__dj_serialize_obj(man);
          ${inner_arr_typ}[] arr = this.ir;
          if(man.getCurrentAction() == edu.berkeley.dj.internal.SerializeManager.SerializationAction.MOVE_OBJ_MASTER ||
            man.getCurrentAction() == edu.berkeley.dj.internal.SerializeManager.SerializationAction.MOVE_OBJ_BLOCK_TIL_READY) {
            this.ir = null;
          }
          int len = arr.length;
          man.register_size(4 ${if(isPrimitive) s"+ ${primType.asInstanceOf[CtPrimitiveType].getDataSize} * len" else ""},
          ${if(!isPrimitive) "len" else "0"});
          man.put_value_I(len);
          for(int i = 0; i < len; i++) {
            ${
        if(isPrimitive) s"man.put_value_${jvmtyp}(arr[i]);" else "arr[i] = man.put_object(arr[i]);"
         }
          }


        }
        """

      val deserialize_helper = s"""
        public void __dj_deserialize_obj(edu.berkeley.dj.internal.SerializeManager man) {
          super.__dj_deserialize_obj(man);
          int len = man.get_value_I();
          this.ir = new ${inner_arr_typ}[len];
          for(int i = 0; i < len; i++) {
            ${if(isPrimitive) s"this.ir[i] = man.get_value_${jvmtyp}();" else "this.ir[i] = man.get_object(this.ir[i]);"}
          }
        }
        """

      val empty_obj_helper =
        s"""
           public void __dj_empty_obj() {
             super.__dj_empty_obj();
             this.ir = null;
           }
         """

      cls.addMethod(CtMethod.make(serialize_helper, cls))
      cls.addMethod(CtMethod.make(deserialize_helper, cls))
      cls.addMethod(CtMethod.make(empty_obj_helper, cls))

    }

    cls
  }

  lazy val ioWrapperObject = runningPool.get(s"${config.internalPrefix}IOWrapperObject")

  // construct a wrapper for this class to be run without modifications so that it can perform IO type operatos
  def makeIOClass(source_cls: CtClass): CtClass = {
    val cls = runningPool.makeClass(source_cls.getName, ioWrapperObject)

    // first check that we can use use this class
    // eg no public fields, inherits from Object, is final
    if(source_cls.getSuperclass.getName != "java.lang.Object")
      throw new DJIOException(s"DJIO class '${source_cls.getName}' must have super class of 'java.lang.Object'")

    if(!Modifier.isFinal(source_cls.getModifiers))
      throw new DJIOException(s"DJIO class '${source_cls.getName}' must be final")

    for(field <- source_cls.getDeclaredFields) {
      if(Modifier.isPublic(field.getModifiers))
        throw new DJIOException(s"DJIO class '${source_cls.getName}' can not have public field '${field.getName}'")
    }

//    cls.addField(CtField.make("public int __dj_io_owning_machine = -1;", cls))
//    cls.addField(CtField.make("public int __dj_io_object_id = -1;", cls))

    for(con <- source_cls.getConstructors) {
      // not sure why would have a non public constructor
      if(Modifier.isPublic(con.getModifiers)) {
        val ann = con.getAnnotation(classOf[DJIOTargetMachineArgPosition])
        if(ann == null) {
          // TODO: should allow this constructor but simply just
          // prevent it from being called inside the distributed program
          throw new DJIOException(s"DJIO class '${source_cls.getName}' is missing the @DJIOTargetMachineArgPosition annotation on one of its constructors")
        }
        val targetArgPos = ann.asInstanceOf[DJIOTargetMachineArgPosition].value()
        val paramsTypes = con.getParameterTypes
        if(!(targetArgPos >= 1 && targetArgPos <= paramsTypes.length && paramsTypes(targetArgPos - 1) == CtClass.intType)) {
          throw new DJIOException(s"DJIO class '${source_cls.getName}' has bad target argument for target argument")
        }
        val args = paramsTypes.zipWithIndex.map(a => s"${getUsableName(a._1)} arg${a._2}").mkString(", ")
        val argsTyp = paramsTypes.map(s => "\""+s.getName+"\"").mkString(", ")
        val argsTypStr = if(paramsTypes.length > 0) {
          s"new String [] { $argsTyp }"
        } else {
          s"new String[0]"
        }
        val con_code = s"""
                           public ${cls.getSimpleName} (${args}) {
                             super();
                             this.__dj_class_mode |= 0x80; // IS_IO_WRAPPER
                             this.__dj_io_owning_machine = arg${targetArgPos - 1} ;
                             this.__dj_io_object_id = ${config.internalPrefix}IOHelper.constructLocalIO(this.__dj_io_owning_machine, "${source_cls.getName}", $argsTypStr ,  $$args, this);
                           }
                           """
        cls.addConstructor(CtNewConstructor.make(con_code, cls))
      }
    }

    for(mth <- source_cls.getDeclaredMethods) {
      if(Modifier.isPublic(mth.getModifiers)) {
        // we need to create a proxy to this method
        val params = mth.getParameterTypes
        val args = params.zipWithIndex.map(a => s"${getUsableName(a._1)} arg${a._2}").mkString(", ")
        val argsTyp = params.map(s => "\""+s.getName+"\"").mkString(", ")
        val argsTypStr = if(params.length > 0) {
          s"new String [] { $argsTyp }"
        } else {
          "new String[0]"
        }
        val (returnPrefix, returnSuffix) = if(mth.getReturnType == CtClass.voidType) {
          ("", "") // void do nothing
        } else {
          if(mth.getReturnType.isPrimitive) {
            (s"return ((${config.coreprefix}${mth.getReturnType.asInstanceOf[CtPrimitiveType].getWrapperName})(", s")).${mth.getReturnType.getName}Value()")
          } else {
            (s"return (${getUsableName(mth.getReturnType)})(", ")")
          }
        }
        val mth_code =
          s"""
             public ${getUsableName(mth.getReturnType)} ${mth.getName} (${args}) {
               ${returnPrefix} ${config.internalPrefix}IOHelper.callMethod(this.__dj_io_owning_machine, this.__dj_io_object_id, "${mth.getName}", $argsTypStr , $$args, this) ${returnSuffix} ;
             }
           """
        cls.addMethod(CtMethod.make(mth_code, cls))
      }
    }

    // TODO: serialize/deserialize methods
    // TODO: remote reads and write methods??


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
      // try to just get the class normally from the pool
      basePool get classname
    } catch {
      case e: NotFoundException => {
        try {
          // if this class starts with our prefix edu.berkeley.dj.internal.coreclazz
          // check if we can find a replaced version of that class
          if (classname.startsWith(config.coreprefix)) {
            val c = basePool get (classname + "00DJ")
            c.setName(classname)
            c
          } else null
        } catch {
          case e: NotFoundException => {
            if (classname.contains("$")) {
              // There is some dollar sign in the class name so try to change the containing class
              try {
                val c = basePool.get(classname.replaceAll("(\\.[^\\.\\$]+)\\$", "$100DJ\\$"))
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

  //type MethodAnalysis = Map[MethodInfo, Array[Frame]]

  private def getMethodAnalysis(cls: CtClass): MethodAnalysis = {
    try {
      val m = cls.getDeclaredBehaviors.map(m => {
        val a = new Analyzer
        try {
          Map(m.getMethodInfo -> a.analyze(cls, m.getMethodInfo))
        } catch {
          case _: BadBytecode => null
        }
      }).filter(_ != null).reduce(_ ++ _)
      new MethodAnalysis(m)
    } catch {
      //case _: BadBytecode => {}
      case _: UnsupportedOperationException => new MethodAnalysis(Map()) // there are no declared methods
    }
  }

  override def createCtClass(classname: String, addToCache: CtClass => Unit): CtClass = {
    MethodInfo.doPreverify = true

    if(classname.startsWith(config.coreprefix) && classname.endsWith("00DJ")) {
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
      val ret = new CtArray(classname, runningPool)
      addToCache(ret)
      return ret
    }

    val cls_int = try {
      val c = basePool get (config.coreprefix + classname + "00DJ")
      c.setName(classname)
      c
    } catch {
      case e: NotFoundException =>
        if (classname.contains("$")) {
          try {
            val c = basePool.get(config.coreprefix + classname.replaceAll("(\\.[^\\.\\$]+)\\$", "$100DJ\\$"))
            c.setName(classname)
            c
          } catch {
            case e: NotFoundException => null
          }
        } else null
    }

    if(cls_int != null) {
      // this class is being replaced from the coreclazz even through it isn't in a privleged namespace
      // use the internal rewriting stuff
      reassociateClass(cls_int)
      addToCache(cls_int)
      modifyInternalClass(cls_int)
      return cls_int
    }

    val cls = findBaseClass(classname)
    //println("create class name:" + classname)


    if(classname.startsWith(config.arrayprefix)) {
      if(cls != null) {
        //val mana = getMethodAnalysis(cls)
        reassociateClass(cls)
        addToCache(cls)
        modifyInternalClass(cls)
        return cls
      }
      //val uindx = classname.lastIndexOf("_")
      //val sp = classname.substring(0, uindx).drop(config.arrayprefix.size)
      //val cnt = classname.substring(uindx + 1).toInt
      val ret = makeArrayClass(classname) //, sp, cnt)
      addToCache(ret)
      return ret
    }

    // for edu.berkeley.dj.internal.coreclazz.
    if (classname.startsWith(config.coreprefix)) {
      if (cls != null) {
        //val mana = getMethodAnalysis(cls)
        reassociateClass(cls)
        addToCache(cls)
        modifyInternalClass(cls)
        return cls
      }
      var orgName = classname.drop(config.coreprefix.size)
      val clso = basePool get orgName
      /*if(hasNativeMethods(clso)) {
        return makeProxyCls(clso)
      }*/
      val manao = getMethodAnalysis(clso)
      reassociateClass(clso)
      clso.setName(classname)
      addToCache(clso)
      modifyClass(clso, manao, overrideNative = true)
      return clso
    }

    if(classname.startsWith(config.proxysubclasses)) {
      // TODO:
      throw new NotImplementedError()
    }

    if (cls == null)
      return null

    if(cls.isPrimitive) {
      addToCache(cls)

      return cls
    } else if(cls.isArray) {
      // TODO: some custom handling for array types

      // don't think tha this is ever used
      ???
    } else if (!classname.startsWith("edu.berkeley.dj.internal.")) {

      if(cls.getAnnotation(classOf[DJIO]) != null) {
        // this is an io class
        return makeIOClass(cls)
      }

      val mana = getMethodAnalysis(cls)
      addToCache(cls)
      if(!checkIsAThrowable(cls))
        modifyClass(cls, mana)
      else {
        println("??")
      }
    } else if (classname.startsWith(config.internalPrefix)) {
      //val mana = getMethodAnalysis(cls)
      reassociateClass(cls)
      addToCache(cls)
      modifyInternalClass(cls)
    }
    cls
  }

}

private[rt] class MethodAnalysis (im: Map[MethodInfo, Array[Frame]]) {

  val m = new mutable.HashMap[MethodInfo, Array[Frame]]()
  m ++= im.toSeq

  def getPlace(minfo: MethodInfo, place: Int) = {
    val arr = moffsets.get(minfo).orNull
    var sum = 0
    var i = 0
    while(sum < place) {
      if(i > arr.length) {
        println("FFFFFFFFFFFFFFUCK")
        throw new RuntimeException()
      }
      sum += arr(i) + 1

      i += 1
    }
    if(i >= arr.length)
      arr.length - 1
    else
      i
  }

  def addOffset(minfo: MethodInfo, place: Int, gap: Int): Unit = {
    val arr = moffsets.get(minfo).orNull
    val p = getPlace(minfo, place)
    arr(p) += gap
  }

  def addMethod(cls: CtClass, minfo: MethodInfo): Unit = {
    val analyzer = new Analyzer
    val f = analyzer.analyze(cls, minfo)
    m += (minfo -> f)
    moffsets += (minfo -> new Array[Int](f.length))
  }

  private val moffsets = new mutable.HashMap[MethodInfo, Array[Int]]()

  im.map(m => {
    val l = if(m._2 == null) 0 else m._2.length
    moffsets += (m._1 -> new Array[Int](l))
  })

}
