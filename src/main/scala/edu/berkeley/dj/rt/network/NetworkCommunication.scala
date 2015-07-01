package edu.berkeley.dj.rt.network

import java.nio.ByteBuffer

import edu.berkeley.dj.rt.{NetworkCommInterface, ClientManager, Config}

import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by matthewfl
 */
abstract class NetworkCommunication(private val recever: NetworkRecever) {

  def send(to: Int, action: Int, msg: Array[Byte]): Unit

  def sendWrpl(to: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]]

  // TODO: change the internal apis to use ByteBuffers so that it can avoid copying often
  // can override the seralization method of the comm system to only write a subset of the bytes from a given
  // bytebuffer which would avoid copying items another time
  def send(to: Int, action: Int, msg: ByteBuffer): Unit = {
    val arrmsg: Array[Byte] = if(msg.limit() == msg.position()) {
      msg.array()
    } else {
      val g = new Array[Byte](msg.position())
      Array.copy(msg.array, 0, g, 0, msg.position())
      g
    }
    send(to, action, arrmsg)
  }

  def sendWrpl(to: Int, action: Int, msg: ByteBuffer): Future[ByteBuffer] = {
    val arrmsg: Array[Byte] = if(msg.limit() == msg.position()) {
      msg.array()
    } else {
      val g = new Array[Byte](msg.position())
      Array.copy(msg.array, 0, g, 0, msg.position())
      g
    }
    val ret = Promise[ByteBuffer]()
    val ss = sendWrpl(to, action, arrmsg)
    ss.onSuccess {
      case a => ret.success(ByteBuffer.wrap(a))
    }
    ss.onFailure {
      case e => ret.failure(e)
    }
    ret.future
  }

  protected def recv(from: Int, action: Int, msg: Array[Byte]): Unit = recever.recv(from, action, msg)

  protected def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = try {
    recever.recvWrpl(from, action, msg)
  } catch {
    case r: RedirectRequestToAlternateMachine => throw r
    case e: Throwable => Future.failed(e)
  }

  // get all hosts
  def getAllHosts: Seq[Int]

  // get the id of this host, 0 should represent that this is the master
  def getSelfId: Int

}

// this allows the current machine to forward the request to the one which the actual data
// this should avoid having a round trip back to the original requesting machine
// however there should be some message instructing the machine to route new messages of this type some way else
class RedirectRequestToAlternateMachine(val altMachine: Int) extends Throwable {}

trait NetworkRecever {
  // for recving when there will be no reply
  def recv(from: Int, action: Int, msg: Array[Byte]): Unit

  // for sending a reply back
  def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]]

  def start(nc: NetworkCommunication) // should this be async?
}

trait NetworkHostRecever {

  def newProgram(identifier: String): Unit

}

trait NetworkHost {
  def getApplicationComm(identifier: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication

  // send a message to all the clients
  def createNewApp(identifier: String)

  // keep this client alive as long as it is told not to die
  def runClient(man: NetworkManager)
}

class NetworkManager(val code: String, val mode: String) {

  private val host: NetworkHost = mode match {
    case "dummy" => {
      new DummyHost(this)
    }
    case "hazelcast" => {
      new HazelcastHost(code, this)
    }
    case "gasnet" => {
      throw new NotImplementedError()
    }
  }

  def getApplication(identifer: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication = {
    host.getApplicationComm(identifer, isMaster, recever)
  }

  // run an even loop that waits for new client applications to be started
  def runClient = {
    host.runClient(this)
  }

  // send a message to all the other hosts that they should start an application with some identifier
  def createNewApp(identifier: String) = host.createNewApp(identifier)

  def makeClientApplication(identifier: String): NetworkRecever = {
    println(s"we are making the client application for $identifier")
    // on the client, we get a callback with the identifier and then have to construct a runtime on this client host
    val config = new Config(
      debug_clazz_bytecode = null,
      cluster_code = code,
      uuid = identifier
    )
    val man = new ClientManager(config)
    val comm = new NetworkCommInterface(man)
    comm
  }


}