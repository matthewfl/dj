package edu.berkeley.dj.rt

import scala.collection.mutable

/**
 * Created by matthewfl
 */
class RunningInterface (private val config : Config, private val manager: Manager) {

  private var callIn : Object = null
  private var callInCls : java.lang.Class[_] = null
  private var callInMth : java.lang.reflect.Method = null

  override def toString = "RunningInterface (" + config.uuid + ")"

  def getUUID = config.uuid

  def getUnsafe() = Unsafe.theUnsafe

  def classRenamed(name: String) = {
    val s = manager.classRename(name.replace(".","/")).asInstanceOf[String]
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
    callInMth = callInCls.getDeclaredMethods.filter(_.getName == "callIn")(0)
    callInMth.setAccessible(true)
  }

  private def callIn(action : Int, args: Object*): Any = {
    callInMth.invoke(callIn, Integer.valueOf(action), args.toArray.asInstanceOf[Array[Object]])
  }

  def printStdout(i: Int) = {
    println("to stdout "+i)
  }

  def threadId = Thread.currentThread().getId

  def startThread(obj: Object) = {
    val thread = new Thread() {
      override def run() = {
        callIn(1, obj)
      }
    }
    thread.start()
  }

  // some sort of locking when distribuited
  val tempLockSet = new mutable.HashSet[String]()

  def lock(name: String): Boolean = {
    tempLockSet.synchronized {
      if(tempLockSet.contains(name))
        return false
      tempLockSet += name
      return true
    }
  }

  def unlock(name: String) = {
    tempLockSet.synchronized {
      tempLockSet -= name
    }
  }

  val tempDistributiedMap = new mutable.HashMap[String,Object]()

  def setDistributed(name: String, o: Object): Unit = {
    val i: (String,Object) = (name, o)
    tempDistributiedMap += i
  }

  def getDistributed(name: String): Object = {
    tempDistributiedMap.getOrElse(name, null)
  }

  def exit(code: Int): Unit = {
    System.exit(code)
  }



}
