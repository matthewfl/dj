package edu.berkeley.dj.rt

import java.nio.ByteBuffer

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

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

  def callIn(action : Int, args: Any*): Any = {
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
  // going to just make the master node hold all the "distribuited" locks
  val tempLockSet = new mutable.HashSet[String]()

  def lock(name: String): Boolean = {
    if(manager.isMaster) {
      tempLockSet.synchronized {
        if(tempLockSet.contains(name))
          return false
        tempLockSet += name
        return true
      }
    } else {
      Await.result(manager.networkInterface.sendWrpl(0, 3, name.getBytes()), 60 seconds) == 1
    }
  }

  def unlock(name: String) = {
    if(manager.isMaster) {
      tempLockSet.synchronized {
        tempLockSet -= name
      }
    } else {
      Await.result(manager.networkInterface.sendWrpl(0, 4, name.getBytes()), 60 seconds)
    }
  }

  val tempDistributiedMap = new mutable.HashMap[String,Array[Byte]]()

  def setDistributed(name: String, o: Array[Byte]): Unit = tempDistributiedMap.synchronized {
    if(manager.isMaster) {
      val i: (String, Array[Byte]) = (name, o)
      tempDistributiedMap += i
    } else {
      val nba = name.getBytes()
      val bb = ByteBuffer.allocate(nba.length + o.length + 4)
      bb.putInt(nba.length)
      bb.put(nba)
      bb.put(o)
      Await.result(manager.networkInterface.sendWrpl(0, 5, bb), 60 seconds)
    }
  }

  def getDistributed(name: String): Array[Byte] = tempDistributiedMap.synchronized {
    if(manager.isMaster) {
      tempDistributiedMap.getOrElse(name, Array[Byte]())
    } else {
      Await.result(manager.networkInterface.sendWrpl(0, 6, name.getBytes()), 60 seconds)
    }
  }

  def exit(code: Int): Unit = {
    /*if(manager.isMaster)
      System.exit(code)
      */
    for(n <- manager.networkInterface.getAllHosts) {
      if(n != manager.networkInterface.getSelfId)
        manager.networkInterface.send(n, 1, Array[Byte](code.asInstanceOf[Byte]))
    }
  }

  def registerClient = {
    // notifiy the master that there is a new running client
    manager.networkInterface.send(0, 102, Array[Byte]())
  }

  def getSelfId: Int = manager.networkInterface.getSelfId

  def getAllHosts: Array[Int] = manager.networkInterface.getAllHosts.toArray

}