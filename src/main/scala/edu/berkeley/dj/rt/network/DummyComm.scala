package edu.berkeley.dj.rt.network

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

/**
 * Created by matthewfl
 *
 * Dummy system for when running unit tests or more then one dj server in a single jvm that are communicating
 *
 * Attempt to use futures and other async stuff to simulate the async nature of the network
 */
class DummyComm(recever: NetworkRecever,
                private val appId: String,
                private val p: DummyHost,
                private val selfid: Int) extends NetworkCommunication(recever) {

  val workPool = new java.util.concurrent.ForkJoinPool(20)

  val sendCnt = new AtomicInteger()

  override def send(to: Int, action: Int, msg: Array[Byte]): Unit = {
    sendCnt.addAndGet(1)
    DummyHost.comms.get((appId, to)) match {
      case Some(h) => {
        workPool.execute(new Runnable {
          override def run() = {
            try { h.recv(selfid, action, msg) }
            catch { case e: RedirectRequestToAlternateMachine => {
              send(e.altMachine, action, msg)
            }}
            sendCnt.addAndGet(-1)
          }
        })
      }
      case None => throw new RuntimeException("host not found: "+to)
    }
  }

  val sendWCnt = new AtomicInteger()

  override def sendWrpl(to: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = {
    sendWCnt.addAndGet(1)
    DummyHost.comms.get((appId, to)) match {
      case Some(h) => {
        val ret = Promise[Array[Byte]]()
        // doing it this way disconnects the calls between these two operations
        // so they can happen concurrently
        //Future {
        workPool.execute(new Runnable {
          override def run() = {
            try {
              val f = h.recvWrpl(selfid, action, msg)
              f.onSuccess({
                case e => ret.success(e)
              })
              f.onFailure({
                // not sure if want to support passing back the redirect through the future?
                /*case e: NetworkForwardRequest => {
                  ???
                  //sendWrpl(e.to, action, msg).onComplete(ret.complete)
                }*/
                case e: Throwable => {
                  e.printStackTrace()
                  ret.failure(e)
                }
              })
              //f.onComplete(ret.complete)

            } catch {
              case e: RedirectRequestToAlternateMachine => {
                sendWrpl(e.altMachine, action, msg).onComplete(ret.complete)
              }
            }
            sendWCnt.addAndGet(-1)
          }
        })
        ret.future
      }
        /*Future {
        // this recvWrpl returns a future so it can do an async reply
        // normally we would attach callbacks to send the result back to the sending machine
        // but here we are just using await since this is designed to be used with unit tests etc
        Await.result(h.recvWrpl(selfid, action, msg), 3 minute)
      }*/
      case None => throw new RuntimeException("host not found: "+to)
    }
  }

  override def getAllHosts: Seq[Int] = {
    val ret = new ListBuffer[Int]()
    for(c <- DummyHost.comms) {
      if(c._1._1 == appId)
        ret += c._1._2
    }
    ret
  }

  override def getSelfId = selfid

}


class DummyHost(val man: NetworkManager) extends NetworkHost {

  override def getApplicationComm(identifier: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication = {
    val id = if(isMaster)
      0
    else {
      DummyHost.nextHostId += 1
      DummyHost.nextHostId
    }
    val r = new DummyComm(recever, identifier, this, id)
    DummyHost.comms.put((identifier,id), r)
    r
  }

  override def createNewApp(identifier: String) = {
    /*val rc = man.makeClientApplication(identifier)
    getApplicationComm(identifier, false, rc)
    */
    for(h <- DummyHost.hosts) {
      if(h != this)
        h.waitLock.trySuccess(identifier)
    }
    // should this wait for the new servers to be setup
  }

  var waitLock = Promise[String]()

  override def runClient(man: NetworkManager) = {
    var exit = false
    while(!exit) {
      val act = Await.result(waitLock.future, 120 seconds)
      if(act == "exit") exit = true
      else {
        val rc = man.makeClientApplication(act)
        val comm = getApplicationComm(act, false, rc)
        waitLock = Promise[String]()
        Future { rc.start(comm) }
      }
    }
  }

  DummyHost.hosts += this

}

object DummyHost {
  // the set of all applications and id that are created for it
  var nextHostId: Int = 0
  val comms = new mutable.HashMap[(String, Int), DummyComm]()

  val hosts = new mutable.HashSet[DummyHost]()
}