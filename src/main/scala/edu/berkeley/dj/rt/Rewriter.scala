package edu.berkeley.dj.rt

import javassist._

/**
 * Created by matthewfl
 */
class Rewriter (private val config : Config, private val basePool : ClassPool) {

  val runningInterface = new RunningInterface(config)
  //edu.berkeley.dj.internal.InternalInterfaceFactory.RunningUUID = config.uuid

  private val moveInterface = basePool.get("edu.berkeley.dj.internal.Movable")


  def createCtClass(classname : String) : CtClass = {
    if(classname.startsWith("edu.berkeley.dj.rt")) {
      // do not allow loading the runtime into the runtime
      //throw new ClassNotFoundException(classname)
      return null
    }
    /*if(classname == "edu.berkeley.dj.internal.InternalInterfaceFactory") {
      // HACK: make the InternalInterfaceFactory be shared between the internal code
      // and this code, which means that we can use its field to pass values into the
      // running program
      edu.berkeley.dj.internal.InternalInterfaceFactory.lock.synchronized {
        // wait for what ever might be useing this object
        while(edu.berkeley.dj.internal.InternalInterfaceFactory.ii != null) {}
        //edu.berkeley.dj.internal.InternalInterfaceFactory.ii = runningInterface
        edu.berkeley.dj.internal.InternalInterfaceFactory.RunningUUID = config.uuid
      }
      return null
    }*/
    val cls = basePool get classname
    if(!classname.startsWith("edu.berkeley.dj.internal")) {
      cls.addInterface(moveInterface)
    }
    cls
  }

}
