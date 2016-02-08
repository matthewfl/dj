package edu.berkeley.dj.rt

import java.nio._
import java.util.concurrent.TimeoutException
import java.util.UUID

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Created by matthewfl
 */
class RunningInterface (private val config: Config, private val manager: Manager) {

  private var callIn : Object = null
  private var callInCls : java.lang.Class[_] = null
  private var callInMth : java.lang.reflect.Method = null

  private def block[T](f: Future[T]): T = Await.result(f, 60 seconds)

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
    manager.threadPool.submit {
      callIn(1, obj)
    }
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
      Await.result(manager.networkInterface.sendWrpl(0, 3, name.getBytes()), 60 seconds)(0) == 1
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
        manager.networkInterface.send(n, 101, Array[Byte](code.asInstanceOf[Byte]))
    }
    System.exit(code)
  }

  def registerClient = {
    // notifiy the master that there is a new running client
    manager.networkInterface.send(0, 102, Array[Byte]())
  }

  def getSelfId: Int = manager.networkInterface.getSelfId

  def getAllHosts: Array[Int] = manager.networkInterface.getAllHosts.toArray

  def runOnRemote(id: Int, arr: Array[Byte]) = {
    manager.networkInterface.send(id, 103, arr)
  }

  def readField(req: ByteBuffer, op: Int, to: Int): ByteBuffer = {
    block(manager.networkInterface.sendWrpl(to, op, req))
  }

  def writeField(req: ByteBuffer, op: Int, to: Int): Unit = {
    block(manager.networkInterface.sendWrpl(to, op, req))
  }

  def waitOnObject(obj: Array[Byte], to: Int, notify_cnt: Int) = {
    val s = ByteBuffer.allocate(obj.length + 4)
    s.put(obj)
    s.putInt(notify_cnt)
    manager.networkInterface.send(to, 105, s)
  }

  def acquireObjectMonitor(obj: ByteBuffer, to: Int): Boolean = {
    try {
      block(manager.networkInterface.sendWrpl(to, 7, obj))
      true
    } catch {
      case e: TimeoutException => ???
    }
  }

  def releaseObjectMonitor(obj: ByteBuffer, to: Int, notify_cnt: Int): Unit = {
    val s = ByteBuffer.allocate(obj.limit() + 4)
    obj.flip()
    s.put(obj)
    s.putInt(notify_cnt)
    manager.networkInterface.send(to, 106, s)
  }

  def typeDistributed(name: String): Unit = {
    if(name.startsWith(config.internalPrefix) && !name.startsWith(config.coreprefix))
      return
    if(manager.isMaster) {
      val m = manager.asInstanceOf[MasterManager]
      val mod = m.classMode.getMode(name)
      if(!mod.distributedCopies) {
        mod.distributedCopies = true
        m.reloadClass(name)
      }
    } else {
      block(manager.networkInterface.sendWrpl(0, 8, name.getBytes()))
      //??? // TODO: send message to manager to force the class to be reloaded
    }
  }

  def sendNotifyOnObject(obj: Array[Byte], machine: Int, count: Int): Unit = {
    val buf = ByteBuffer.allocate(obj.length + 4)
    buf.put(obj)
    buf.putInt(count)
    manager.networkInterface.send(machine, 107, buf)
  }

  def staticFieldUpdate(update: Array[Byte]): Unit = {
    manager.networkInterface.sendAll(108, update)
  }

  def loadStaticFields(clsname: String) = {
    block(manager.networkInterface.sendWrpl(0, 30, clsname.getBytes))
  }

  def checkClassIsLoaded(clsname: String) = {
    manager.loader.classLoaded(clsname)
  }

  def checkShouldRedirectMethod(clsname: String, id: String) = {
    // Have to communicate with the master machine to get this information.....ggg
    //manager.clas
    id == "something()V"
    //false
  }

  def redirectMethod(req: ByteBuffer, machine: Int) = {
    // TODO: need to make sure that the timer doesn't expire on these rpc calls
    block(manager.networkInterface.sendWrpl(machine, 31, req))
  }

  def sendMoveObject(req: ByteBuffer, machine: Int) = {
    // send msg to owning machine to move object
    manager.networkInterface.send(machine, 111, req)
  }

  def sendSerializedObject(req: ByteBuffer, machine: Int): Unit = {
    manager.networkInterface.send(machine, 112, req)
  }

  def createIOObject(clsname: String, argsTyp: Array[String], args: Array[Object], self: Array[Byte]): Int = {
    manager.io.constructClass(clsname, argsTyp, args, self)
  }

  def callIOMethod(objectId: Int, methodName: String, argsTyp: Array[String], args: Array[Object]): Any = {
    manager.io.callMethod(objectId, methodName, argsTyp, args)
  }

  def proxyCreateIOObject(target: Int, req: ByteBuffer) = {
    ???
  }

  def proxyCallIOMethod(target: Int, req: ByteBuffer) = {
    ???
  }

  def updateObjectLocation(id: UUID, target: Int, machine: Int): Unit = {
    val buf = ByteBuffer.allocate(20)
    buf.putLong(id.getMostSignificantBits)
    buf.putLong(id.getLeastSignificantBits)
    buf.putInt(target)
    manager.networkInterface.send(machine, 104, buf)
  }


  def updateObjectLocationAll(id: UUID, target: Int): Unit = {
    val buf = ByteBuffer.allocate(20)
    buf.putLong(id.getMostSignificantBits)
    buf.putLong(id.getLeastSignificantBits)
    buf.putInt(target)
    manager.networkInterface.sendAll(104, buf)
  }


  def changeReferenceCount(id: UUID, delta: Int, machine: Int): Unit = {
    val buf = ByteBuffer.allocate(20)
    buf.putLong(id.getMostSignificantBits)
    buf.putLong(id.getLeastSignificantBits)
    buf.putInt(delta)
    manager.networkInterface.send(machine, 113, buf)
  }

  def moveObjectFieldRef(target: Int, buf: ByteBuffer): Unit = {
    manager.networkInterface.send(target, 114, buf)
  }

  def sendMakeCache(machine: Int, buf: ByteBuffer): Unit = {
    manager.networkInterface.send(machine, 115, buf)
  }


  def relocateObject(id: UUID): Unit = {
    // if we somehow have lost an object
    // send a command to determine what this object's state is on all machines
    // _Hopefully_ only used for debugging
    val buf = ByteBuffer.allocate(16)
    buf.putLong(id.getMostSignificantBits)
    buf.putLong(id.getLeastSignificantBits)
    manager.networkInterface.sendAll(116, buf)
  }

  def startNetwork = {
    manager.asInstanceOf[MasterManager].startNetwork()
  }




}
