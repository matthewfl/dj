package edu.berkeley.dj.rt

import java.io.{ByteArrayInputStream, InputStream}
import javassist._

/**
 * Created by matthewfl
 */
private[rt] class Rewriter (private val manager : Manager) { //private val config : Config, private val basePool : ClassPool) {

  //val runningInterface = new RunningInterface(config)
  //edu.berkeley.dj.internal.InternalInterfaceFactory.RunningUUID = config.uuid

  def config = manager.config

  def basePool = manager.pool

  def runningPool = manager.runningPool

  private val moveInterface = basePool.get("edu.berkeley.dj.internal.Movable")

  private val rewriteNamespace = "edu.berkeley.dj.internal2"//."+config.uuid

  private lazy val objectBase = {
    //val base = basePool.get("edu.berkeley.dj.internal.ObjectBase")
    val ob = runningPool.makeClass(rewriteNamespace+".ObjectBase")
    val fsettings = CtField.make("public int "+config.fieldPrefix+"settings = 0;", ob)
    ob.addField(fsettings)
    val fmanager = CtField.make("public edu.berkeley.dj.internal.Manager "+config.fieldPrefix+"manager = null;", ob)
    ob.addField(fmanager)
    ob
    //base
  }


  def createCtClass(classname : String) : CtClass = {
    if(classname.startsWith("edu.berkeley.dj.rt")) {
      // do not allow loading the runtime into the runtime
      throw new ClassNotFoundException(classname)
    }

    if(classname == rewriteNamespace + ".ObjectBase") {
      return objectBase
    }


    var cls = basePool get classname
    if(cls == null)
      return null

    cls = manager.runningPool.makeClass(new ByteArrayInputStream(cls.toBytecode()))

    //cls.detach
    if(!classname.startsWith("edu.berkeley.dj.internal")) {
      cls.addInterface(moveInterface)
      println("rewriting class: "+classname)
      val sc = cls.getSuperclass
      if(sc.getName == "java.lang.Object") {
        // this comes directly off the object class
        //cls.setSuperclass(objectBase)
      }
    }
    println("done rewriting: "+classname)
    //cls.toClass(manager.loader, manager.protectionDomain)
    cls.detach
    cls
  }

}
