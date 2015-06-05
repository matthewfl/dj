package edu.berkeley.dj.rt

/**
 * Created by matthewfl
 */
class RunningInterface (private val config : Config, private val manager: Manager) {

  private var callIn : Object = null
  private var callInCls : java.lang.Class[_] = null

  override def toString = "RunningInterface (" + config.uuid + ")"

  def getUUID = config.uuid

  def getUnsafe() = Unsafe.theUnsafe

  def classRenamed(name: String) = {
    val s = manager.rewriter.jclassmap.get(name.replace(".","/")).asInstanceOf[String]
    if(s != null)
      s.replace("/",".")
    else
      null
  }

  def setCallIn(obj : Object) = {
    if(obj.getClass.getName != "edu.berkeley.dj.internal.InternalInterfaceWrap") {
      throw new RuntimeException("must use internal interface for call in")
    }
    if(callIn != null)
      throw new RuntimeException("can only set the call in interface once")
    callIn = obj
    callInCls = obj.getClass
  }

  private def callIn(action : Int, args: Object*) = {

  }

  def printStdout(i: Int) = {
    println("to stdout "+i)
  }



}
