package edu.berkeley.dj.rt

import java.io.{ByteArrayInputStream, InputStream}
import javassist._

import edu.berkeley.dj.rt.convert.{FunctionCalls, CodeConverter}


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

  private def transformClass(cls : CtClass, movable : Boolean = true) = {
    val manager = runningPool.makeClass("edu.berkeley.dj.internal.managers."+cls.getName, classMangerBase)
    if(movable)
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

    val isInterface = Modifier.isInterface(cls.getModifiers)
    if(!isInterface)
      cls.instrument(codeConverter)
    // TODO: need to handle interfaces that can have methods on them

  }

  def createCtClass(classname : String) : CtClass = {
    if(classname.startsWith("edu.berkeley.dj.rt")) {
      // do not allow loading the runtime into the runtime
      throw new ClassNotFoundException(classname)
    }

    if(classname == "edu.berkeley.dj.internal.ObjectBase") {
      return objectBaseRaw
    }

    var cls = basePool get classname
    if(cls == null)
      return null

    // is this necessary???, doesn't seem like it....
    //cls = manager.runningPool.makeClass(new ByteArrayInputStream(cls.toBytecode()))

    //cls.detach
    if(!classname.startsWith("edu.berkeley.dj.internal")) {
      //cls.addInterface(moveInterface)
      println("rewriting class: "+classname)
      val mods = cls.getModifiers
      println("modifiers: "+Modifier.toString(mods))
      val sc = cls.getSuperclass
      if(sc.getName == "java.lang.Object" && !Modifier.isInterface(mods)) {
        // this comes directly off the object class
        cls.setSuperclass(objectBase)
      }
      if(cls.getName.contains("testcase"))
        transformClass(cls)
      // there is an instrument method on CtClass that takes a CodeConverter
    }
    println("done rewriting: "+classname)
    //cls.toClass(manager.loader, manager.protectionDomain)
    //cls.detach
    cls
  }

}
