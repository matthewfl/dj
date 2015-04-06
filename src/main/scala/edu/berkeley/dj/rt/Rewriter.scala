package edu.berkeley.dj.rt

import javassist._

import edu.berkeley.dj.rt.convert.{Monitors, FieldAccess, FunctionCalls, CodeConverter}


/**
 * Created by matthewfl
 */
private[rt] class Rewriter (private val manager : Manager) { //private val config : Config, private val basePool : ClassPool) {

  //val runningInterface = new RunningInterface(config)
  //edu.berkeley.dj.internal.InternalInterfaceFactory.RunningUUID = config.uuid

  def config = manager.config

  def basePool = manager.pool

  def runningPool = manager.runningPool

  private lazy val moveInterface = runningPool.get("edu.berkeley.dj.internal.Movable")

  private val rewriteNamespace = "edu.berkeley.dj.internal2"//."+config.uuid

  def canRewrite (classname : String) = {
    !classname.equals("java.lang.Object")
    // TODO: a lot more base class and packages
  }

  private lazy val objectBaseRaw = {
    //val base = basePool.get("edu.berkeley.dj.internal.ObjectBase")
    //val ob = runningPool.makeClass(rewriteNamespace+".ObjectBase")
    //val fsettings = CtField.make("public int "+config.fieldPrefix+"settings = 0;", ob)
    //ob.addField(fsettings)
    //val fmanager = CtField.make("public edu.berkeley.dj.internal.Manager "+config.fieldPrefix+"manager = null;", ob)
    //ob.addField(fmanager)
    //ob
    val ob = basePool.get("edu.berkeley.dj.internal.ObjectBase")
    ob

    //base
  }

  private lazy val objectBase = runningPool.get("edu.berkeley.dj.internal.ObjectBase")

  private lazy val classMangerBase = runningPool.get("edu.berkeley.dj.internal.ClassManager")

  //private val ManagerClasses = new mutable.HashMap[String, CtClass]()

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
    "notify" -> "__dj_nofity",
    "notifyAll" -> "__dj_notifyAll",
    "wait" -> "__dj_wait"
  )

  // if these methods are anywhere
  val rewriteMethodNames = Map(
    "finalize" -> "__dj_client_finalize"
  )

  val replacedClasses = Map(
  )

  private def isClassMovable(cls: CtClass) = {
    !NonMovableClasses.contains(cls.getName)
  }

  private def getUsuableName(typ: CtClass) : String = {
    if(typ.isArray) {
      getUsuableName(typ.getComponentType) + "[]"
    } else if(typ.isPrimitive) {
      typ.getName
    } else {
      typ.getName.split("\\.").map("``"+_+"``").mkString(".")
    }
  }

  private def transformClass(cls : CtClass) = {
    //val manager = runningPool.makeClass("edu.berkeley.dj.internal.managers."+cls.getName, classMangerBase)
    cls.addInterface(moveInterface)
    val codeConverter = new CodeConverter
    /*rewriteMethodCalls.foreach(v => {
      val mth = cls.getMethods.filter(_.getName == v._2)
      if(!mth.isEmpty)
        codeConverter.redirectMethodCall(v._1, mth(0))
    })*/
    codeConverter.addTransform(new FunctionCalls(codeConverter.prevTransforms, rewriteMethodCalls.map(n => {
      val mths = cls.getMethods.filter(_.getName == n._2)
      if(!mths.isEmpty)
        Map(n._1 -> mths(0))
      else
        Map[String, CtMethod]()
    }).reduce(_ ++ _)))

    codeConverter.addTransform(new FieldAccess(codeConverter.prevTransforms, config))
    codeConverter.addTransform(new Monitors(codeConverter.prevTransforms))

    val isInterface = Modifier.isInterface(cls.getModifiers)
    if(!isInterface) {
      cls.instrument(codeConverter)
      for(field <- cls.getDeclaredFields) {
        val name = field.getName
        println("field name: " + name);
        if (!name.startsWith(config.fieldPrefix) && !field.getType.isArray) {
          val typ = field.getType
          val typ_name = getUsuableName(typ)
          val modifiers = field.getModifiers

          val accessMod =
            if (Modifier.isPublic(modifiers))
              "public"
            else if (Modifier.isProtected(modifiers))
              "protected"
            else
              "private"

          // TODO: deal with static variables
          if (!Modifier.isStatic(modifiers)) {
            val write_method =
              s"""
              ${accessMod} void ``${config.fieldPrefix}write_field_${name}`` (${typ_name} val) {
                System.out.println("field name ${name} was written on ${cls.getName}");
                this.``${name}`` = val;
             }
              """
            val read_method =
              s"""
           ${accessMod} ${typ_name} ``${config.fieldPrefix}read_field_${name}`` () {
             System.out.println("reading field ${name}");
             return this.${name};
           }
              """
            try {
              println("\t\tadding method for: " + name + " to " + cls.getName)
              cls.addMethod(CtMethod.make(write_method, cls))
              cls.addMethod(CtMethod.make(read_method, cls))
            } catch {
              case e => {
                println("gg")
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
      for(field <- cls.getDeclaredFields) {
        if(field.getType.isPrimitive) {
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
          if(isClassMovable(field.getType)) {

          } else {
            // TODO: seralize a proxy object so we can still use this later
          }
        }
      }


    }
    // TODO: need to handle interfaces that can have methods on them
  }


  private def makeProxyCls(cls: CtClass) = {
    // this is used for non movable classes
    // where we need to have some proxy that calls methods
    // back on the origional class
    val pxycls = runningPool.makeClass(config.proxyClassPrefix+cls.getName, cls)
    CtField.make("public int __dj_class_mode = 0;", pxycls);
    CtField.make("public edu.berkeley.dj.internal.ClassManager __dj_class_manager = null", pxycls);
    for(mth <- cls.getMethods) {
      if(Modifier.isPublic(mth.getModifiers)) {
        // this is a public method, so we might have to proxy it
        val rtn = mth.getReturnType.getName
        val sig = mth.getSignature
        val pxy_method =
          s"""
             public ${rtn} ${mth.getName} (
          """
      }
    }
    pxycls
  }

  def createCtClass(classname : String) : CtClass = {
    if(classname.startsWith("edu.berkeley.dj.rt")) {
      // do not allow loading the runtime into the runtime
      throw new ClassNotFoundException(classname)
    }

    if(classname == "edu.berkeley.dj.internal.ObjectBase") {
      return objectBaseRaw
    }

    if(classname.startsWith(config.proxyClassPrefix)) {
      var orgName = classname.drop(config.proxyClassPrefix.size)
      return makeProxyCls(basePool get orgName)
    }

    var cls = basePool get classname
    if(cls == null)
      return null

    // is this necessary???, doesn't seem like it....
    //cls = manager.runningPool.makeClass(new ByteArrayInputStream(cls.toBytecode()))

    //cls.detach
    if(!classname.startsWith("edu.berkeley.dj.internal.")) {
      //cls.addInterface(moveInterface)
      println("rewriting class: "+classname)
      val mods = cls.getModifiers
      println("modifiers: "+Modifier.toString(mods))
      val sc = cls.getSuperclass
      if(sc.getName == "java.lang.Object" && !Modifier.isInterface(mods)) {
        // this comes directly off the object class
        cls.setSuperclass(objectBase)
      }
      //if(cls.getName.contains("testcase"))
      transformClass(cls)
      // there is an instrument method on CtClass that takes a CodeConverter
    }
    /*if(!cls.getFields.filter(_.getName.contains("asdf")).isEmpty) {
      println("found it")
    }*/
    /*for(field <- cls.getFields) {
      println(s"\tcls: ${cls.getName} field: ${field.getName}")
    }*/

    //println("done rewriting: "+classname)
    //cls.toClass(manager.loader, manager.protectionDomain)
    //cls.detach
    cls
  }

}
