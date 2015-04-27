package edu.berkeley.dj.rt

import javassist._
import javassist.bytecode.{MethodInfo, SignatureAttribute}

import edu.berkeley.dj.rt.convert.CodeConverter
import edu.berkeley.dj.rt.convert._


/**
 * Created by matthewfl
 */
private[rt] class Rewriter (private val manager : Manager) {
  //private val config : Config, private val basePool : ClassPool) {

  //val runningInterface = new RunningInterface(config)
  //edu.berkeley.dj.internal.InternalInterfaceFactory.RunningUUID = config.uuid

  def config = manager.config

  def basePool = manager.pool

  def runningPool = manager.runningPool

  // used to get access to the internal members that need to be rewritten
  //private val selfPool = new ClassPool(true)
  //selfPool.appendClassPath(new ClassClassPath(this.getClass))

  private lazy val moveInterface = runningPool.get("edu.berkeley.dj.internal.Movable")

  //private val rewriteNamespace = "edu.berkeley.dj.internal2"//."+config.uuid

  /*def canRewrite (classname : String) = {
    !classname.equals("java.lang.Object")
    // TODO: a lot more base class and packages
  }*/

  /*private lazy val objectBaseRaw = {
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
  }*/

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

  private def getUsuableName(typ: CtClass): String = {
    if (typ.isArray) {
      getUsuableName(typ.getComponentType) + "[]"
    } else if (typ.isPrimitive) {
      typ.getName
    } else {
      typ.getName.split("\\.").map("``" + _ + "``").mkString(".")
    }
  }

  //private def getUsuableFieldName()

  private def reassociateClass(cls: CtClass) = {
    /*if(cls.getClassPool == basePool) {
      cls.detach()
      // TODO: setClasspool
    }*/
    cls.setClassPool2(runningPool)
  }

  private def rewriteUsedClasses(cls: CtClass) = {
    val map = new JClassMap(manager)
    cls.replaceClassName(map)

    // the javassist library appears to not reflect the super class properly when replacing names
    val sname = cls.getSuperclass.getName
    val nsname = map.get(sname).asInstanceOf[String]
    if (nsname != null && nsname != sname) {
      // set the new super class
      cls.setSuperclass(cls.getClassPool.get(nsname))
    }
    //runningPool.get(cls.getSuperclass.getName)
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
    codeConverter.addTransform(new FunctionCalls(codeConverter.prevTransforms, rewriteMethodCalls.map(n => {
      val mths = cls.getMethods.filter(_.getName == n._2)
      if (!mths.isEmpty)
        Map(n._1 -> mths(0))
      else
        Map[String, CtMethod]()
    }).reduce(_ ++ _)))

    codeConverter.addTransform(new Arrays(codeConverter.prevTransforms, config))
    //codeConverter.addTransform(new FieldAccess(codeConverter.prevTransforms, config))
    //codeConverter.addTransform(new Monitors(codeConverter.prevTransforms))

    val isInterface = Modifier.isInterface(cls.getModifiers)
    if (!isInterface) {
      cls.instrument(codeConverter)
      for (field <- cls.getDeclaredFields) {
        val name = field.getName
        println("field name: " + name);
        if (!name.startsWith(config.fieldPrefix) && !field.getType.isArray) {
          val typ = field.getType
          val typ_name = getUsuableName(typ)
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
      for (field <- cls.getDeclaredFields) {
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
      }


    }
    // TODO: need to handle interfaces that can have methods on them
  }


  /*private def makeProxyCls(cls: CtClass) = {
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
  }*/

  def modifyClass(cls: CtClass): Unit = {
    println("rewriting class: " + cls.getName)
    val mods = cls.getModifiers
    println("modifiers: " + Modifier.toString(mods))
    reassociateClass(cls)
    rewriteUsedClasses(cls)
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
  }

  def createCtClass(classname: String): CtClass = {
    if (classname.startsWith("edu.berkeley.dj.rt")) {
      // do not allow loading the runtime into the runtime
      throw new ClassNotFoundException(classname)
    }

    /*if(classname == "edu.berkeley.dj.internal.ObjectBase") {
      return objectBaseRaw
    }*/

    var cls: CtClass = try {
      basePool get classname
    } catch {
      case e : NotFoundException => null
    }
    println("create class name:" + classname)

    if (classname.startsWith(config.coreprefix)) {
      if (cls != null)
        return cls
      var orgName = classname.drop(config.coreprefix.size)
      val clso = basePool get orgName
      reassociateClass(clso)
      clso.setName(classname)
      modifyClass(clso)
      return clso
    }

    if (cls == null)
      return null


    if(cls.isArray) {
      // TODO: some custom handling for array types

    } else if (!classname.startsWith("edu.berkeley.dj.internal.")) {
      modifyClass(cls)
    }
    cls
  }

}
