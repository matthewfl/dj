package edu.berkeley.dj.rt.network

import scala.collection.mutable
import scala.concurrent.Future

/**
 * Created by matthewfl
 */
class DummyComm(recever: NetworkRecever, private val p: DummyHost, private val selfid: Int) extends NetworkCommunication {

  override def send(to: Int, action: Int, msg: Array[Byte]) = {
    p.hosts.get(to) match {
      case Some(h) => h.recv(selfid, action, msg)
      case None => throw new RuntimeException("host not found: "+to)
    }
  }

  override def sendWrpl(to: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = {
    p.hosts.get(to) match {
      case Some(h) => h.recvWrpl(selfid, action, msg)
      case None => throw new RuntimeException("host not found: "+to)
    }
  }

}


class DummyHost extends NetworkHost {

  var nextHostId = 0

  val hosts = new mutable.HashMap[Int,DummyComm]()

  def getApplicationComm(identifier: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication = {
    val id = if(isMaster)
      0
    else {
      nextHostId += 1
      nextHostId
    }
    val r = new DummyComm(recever, this, id)
    hosts.put(id, r)
  }

}