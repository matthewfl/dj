package edu.berkeley.dj.rt.network

import edu.berkeley.dj.rt.{NetworkCommInterface, ClientManager, Config}

import scala.concurrent.Future

/**
 * Created by matthewfl
 */
abstract class NetworkCommunication(private val recever: NetworkRecever) {

  def send(to: Int, action: Int, msg: Array[Byte]): Unit

  def sendWrpl(to: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]]

  protected def recv(from: Int, action: Int, msg: Array[Byte]): Unit = recever.recv(from, action, msg)

  protected def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = try {
    recever.recvWrpl(from, action, msg)
  } catch {
    case e: Throwable => Future.failed(e)
  }

  // get all hosts
  def getAllHosts: Seq[Int]

  // get the id of this host, 0 should represent that this is the master
  def getSelfId: Int

}

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