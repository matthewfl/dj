package edu.berkeley.dj.rt

import javassist.bytecode.Descriptor
import javassist._

import edu.berkeley.dj.internal.DJIOException

/**
  * Created by matthewfl
  */
private[rt] class IORewriter (private val manager: MasterManager) extends RewriterInterface {


  def basePool = manager.pool

  def runningPool = manager.ioRunningPool

  def config = manager.config

//  val proxySuffix = "_dj_io_proxy"

  private def reassociateClass(cls: CtClass) = {
    if(cls.getClassPool != runningPool) {
      // prime the cache of the class before we move it to a new pool
      cls.getClassFile
      cls.setClassPool2(runningPool)
      // cache this class before we have fully rewritten it in hopes that we don't loop
      runningPool.setClass(cls.getName, cls)
    }
  }

  private lazy val Object = runningPool.get("java.lang.Object")

  private def createProxyCls(classname: String): CtClass = {
    val orgClass = runningPool.get(classname.dropRight(config.ioProxySuffix.length))
    val cls = runningPool.makeClass(classname, orgClass)
    if(orgClass.getSuperclass != Object) {
      throw new DJIOException("super class must be Object")
    }
    for(field <- orgClass.getDeclaredFields) {
      if(Modifier.isPublic(field.getModifiers))
        throw new DJIOException("can not have a public field on a proxied class")
    }
    for(mth <- orgClass.getDeclaredMethods) {
      val mods = mth.getModifiers
      if(Modifier.isPublic(mods)) {
        if(Modifier.isFinal(mods))
          throw new DJIOException("Can not have final on one of the public methods of a proxied class")
      }
    }

    cls.addField(CtField.make("public Object __dj_io_wraps;", cls))

    // the class should be good at this point, so we can create a proxy wrapper for it now
    for(mth <- orgClass.getDeclaredMethods) {
      val mods = mth.getModifiers
      val access = if(Modifier.isPublic(mods))
        "public"
      else if(Modifier.isPrivate(mods))
        "private"
      else if(Modifier.isProtected(mods))
        "protected"
      else
        ""
      val rtype = mth.getReturnType

      val args = Descriptor.getParameterTypes(mth.getSignature, cls.getClassPool)

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

      val (cast_prefix, cast_suffix) = if (rtype.isPrimitive) {
        (s"((edu.berkeley.dj.internal.coreclazz.${rtype.asInstanceOf[CtPrimitiveType].getWrapperName})", s").${rtype.getName}Value()")
      } else {
        (s"(${rtype.getName})", "")
      }


      val code =
        s"""
           ${access} ${rtype.getName} ${mth.getName} (${args.zipWithIndex.map(v => v._1.getName + " a"+v._2).mkString(", ")}) {
             ${if(rtype != CtClass.voidType) s"return $cast_prefix" else ""}
             edu.berkeley.dj.ioInternal.IOCallDJHelper.call(__dj_io_wraps, "${mth.getName}",
               $cls_types,
               $$args
             ) ${if(rtype != CtClass.voidType) cast_suffix else ""};
           }
         """

      cls.addMethod(CtNewMethod.make(code, cls))
    }

    cls
  }

  override def createCtClass(classname: String, addToCache: CtClass => Unit): CtClass = {
    if(classname.endsWith(config.ioProxySuffix)) {
      // TODO: make this create a class that can proxy into the distribuited application
      return createProxyCls(classname)
    }
    val cls = basePool.get(classname)
    reassociateClass(cls)
    cls
    //null
  }

}
