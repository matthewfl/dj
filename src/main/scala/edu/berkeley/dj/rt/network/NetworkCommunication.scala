package edu.berkeley.dj.rt.network

import scala.concurrent.Future

/**
 * Created by matthewfl
 */
abstract class NetworkCommunication(val recever: NetworkRecever) {

  abstract def send(to: Int, action: Int, msg: Array[Byte]): Unit

  abstract def sendWrpl(to: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]]

  protected def recv(from: Int, action: Int, msg: Array[Byte]): Unit = recever.recv(from, action, msg)

  protected def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = recever.recvWrpl(from, action, msg)


}

trait NetworkRecever {
  // for recving when there will be no reply
  def recv(from: Int, action: Int, msg: Array[Byte]): Unit

  // for sending a reply back
  def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]]

}

trait NetworkHost {
  def getApplicationComm(identifier: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication
}

object NetworkCommunication {

  private var host: NetworkHost = null

  def setupHost(code: String, mode: String) = mode match {
    case "dummy" => {
      host = new DummyHost()
    }
    case "hazelcast" => {
      throw new NotImplementedError()
    }
    case "gasnet" => {
      throw new NotImplementedError()
    }
  }

  def getApplication(identifer: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication = {
    host.getApplicationComm(identifer, isMaster, recever)
  }



}