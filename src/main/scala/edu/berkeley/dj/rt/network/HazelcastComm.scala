package edu.berkeley.dj.rt.network

import java.io.Serializable

import com.hazelcast.config.{MulticastConfig, TcpIpConfig, ListenerConfig, Config}
import com.hazelcast.core._
import edu.berkeley.dj.internal.NetworkForwardRequest
import edu.berkeley.dj.rt.network.HazelcastComm.{sendReply, sendMessage}
import edu.berkeley.dj.rt.network.HazelcastHost.{DJModConfig, startProgram}
import edu.berkeley.dj.utils.Memo

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

/**
 * Created by matthewfl
 *
 * This is the basic interface for hazelcast, which is good for running over a typical network
 *
 */
class HazelcastComm(recever: NetworkRecever,
                    private val appId: String,
                    private val host: HazelcastHost) extends NetworkCommunication(recever) {

  private def app_id_key = "DJ_app_id_"+appId

  private var nextReplyId = 1

  private val replyPromises = new mutable.HashMap[Long, Promise[Array[Byte]]]

  private lazy val app_id_map = Memo[Int,Int,Member] { case id: Int => {
    var ret: Member = null
    for(m <- host.instance.getCluster.getMembers) {
      if(m.getIntAttribute(app_id_key) == id)
        ret = m
    }
    if(ret == null)
      throw new RuntimeException(s"host $id not found for application $appId")
    ret
  }}

  override def send(to: Int, action: Int, msg: Array[Byte]) = {
    val tom = app_id_map(to)
    val exec = host.instance.getExecutorService("send-msg")
    exec.executeOnMember(new sendMessage(appId, getSelfId, action, 0, msg), tom)
  }

  override def sendWrpl(to: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = {
    val tom = app_id_map(to)
    val (nextId, ret) = this.synchronized {
      nextReplyId += 1
      val p = Promise[Array[Byte]]()
      replyPromises.put(nextReplyId, p)
      (nextReplyId, p)
    }
    val exec = host.instance.getExecutorService("send-msg")
    exec.executeOnMember(new sendMessage(appId, getSelfId, action, nextReplyId, msg), tom)
    ret.future
  }

  override def getAllHosts: Seq[Int] = {
    val ret = new ListBuffer[Int]()
    for(m <- host.instance.getCluster.getMembers) {
      val r = m.getIntAttribute(app_id_key)
      if(r != null)
        ret += r
    }
    ret
  }

  lazy val getSelfId: Int = host.instance.getCluster.getLocalMember.getIntAttribute(app_id_key)

  private[network] def processMsg(from: Int, action: Int, msg: Array[Byte], replyId: Long) = {
    try {
      if(replyId == 0) {
        // there is no reply required
        recv(from, action, msg)
      } else {
      // there is some expected reply
      val fut = recvWrpl(from, action, msg)
        fut.onComplete(f => {
          val send = f match {
            case Success(m) => new sendReply(appId, getSelfId, replyId, msg = m, error = null)
              // TODO: ? should failure check the exception if instance of forward request?
            case Failure(e) => new sendReply(appId, getSelfId, replyId, msg = null, error = e)
          }
          val exec = host.instance.getExecutorService("send-msg")
          exec.executeOnMember(send, app_id_map(from))
        })
      }
    } catch {
      case e: NetworkForwardRequest => {
        val tom = app_id_map(e.to)
        val exec = host.instance.getExecutorService("send-msg")
        exec.executeOnMember(new sendMessage(appId, from, action, replyId, msg), tom)
      }
    }
  }

  private[network] def processReply(from: Int, replyId: Long, msg: Array[Byte], error: Throwable): Unit = {
    val res = this.synchronized {
      val r = replyPromises.get(replyId)
      replyPromises -= replyId
      r
    }
    res match {
      case Some(p) => {
        if(error != null)
          p.failure(error)
        else
          p.success(msg)
      }
      case None => ??? // what do here
    }
  }

}



class HazelcastHost(private val gcode: String,
                    val commMan: NetworkManager) extends NetworkHost {

  // gcode should be a long (~20-30) random string of characters
  assert(gcode.length > 10)

  private val hconfig = new DJModConfig(this)

  hconfig.getGroupConfig.setName("DJ_cluster_"+gcode.substring(0, 5))
  hconfig.getGroupConfig.setPassword(gcode)

  // for the master node where we are presenting the logging of the application instead of the network
  //hconfig.setProperties("hazelcast.logging.type", "none")

  hconfig.getNetworkConfig.getJoin.setTcpIpConfig(new TcpIpConfig {
    // TODO: take this configuration from the command line
    // hack, just take this from the system get property
    val seed = System.getProperty("dj.cluster_seed", "10.7.0.5,10.7.0.1,10.7.0.17")
    addMember(seed)
    setEnabled(true)
  })
  hconfig.getNetworkConfig.getJoin.setMulticastConfig(new MulticastConfig {
    setEnabled(false)
  })
  hconfig.addListenerConfig(new ListenerConfig() {
    setImplementation(new MembershipListener {

      override def memberAttributeChanged(memberAttributeEvent: MemberAttributeEvent): Unit = {}

      override def memberAdded(membershipEvent: MembershipEvent): Unit = {
        println("new member to cluster")
        println(membershipEvent)
      }

      override def memberRemoved(membershipEvent: MembershipEvent): Unit = {
        println("We have lost a member of the cluster, shutting down since will have undefined behavior past this point")
        println(membershipEvent)
        System.exit(1)
      }
    })
  })

  /*private[network]*/ val instance = Hazelcast.newHazelcastInstance(hconfig)

  private def member = instance.getCluster.getLocalMember

  private[network] val running_apps = new mutable.HashMap[String,HazelcastComm]

  override def getApplicationComm(identifier: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication = {
    if(isMaster) {
      val ra: IMap[String,Integer] = instance.getMap("DJ_running_apps")
      ra.lock(identifier)
      try {
        // check to make sure that there is not another master of this application running in the cluster
        for(m <- instance.getCluster.getMembers) {
          if(m != member && m.getIntAttribute("DJ_app_id_" + identifier) == 0)
            throw new RuntimeException(s"There already exists a master of $identifier on $m")
        }
        // for now we are restricting that a given node can only be one master
        member.setStringAttribute("DJ_master_of", identifier)
        member.setIntAttribute("DJ_app_id_" + identifier, 0) // indicate that this is the master
        ra.putIfAbsent(identifier, 1)
      } finally {
        ra.unlock(identifier)
      }
    }
    // if we are not the master then the calling machine should have already set DJ_app_id_$identifier for us
    val r = new HazelcastComm(recever, identifier, this)
    running_apps.put(identifier, r)
    r
  }

  override def createNewApp(identifier: String) = {
    //val otherMembers = instance.getCluster.getMembers.filter(_ != instance.getCluster.getLocalMember)
    val exec = instance.getExecutorService("start-app")
    val ra: IMap[String,Integer] = instance.getMap("DJ_running_apps")
    ra.lock(identifier)
    try {
      var cnt = ra.get(identifier)
      if(cnt == null)
        cnt = 1

      for (m <- instance.getCluster.getMembers) {
        // make sure that this is not outself and not a master of another application
        if (m != member && m.getStringAttribute("DJ_master_of") == null) {
          // create some id for this node for this application
          //m.setIntAttribute("DJ_app_id_"+identifier, cnt)

          // run something on the remote machine to signal that it should start up this application
          exec.executeOnMember(new startProgram(identifier, cnt), m)
          cnt += 1
        }
      }
      ra.set(identifier, cnt)
    } finally {
      ra.unlock(identifier)
    }
  }

  override def runClient(man: NetworkManager) = {
    // look for all currently running applications
    // and start them
    // also register
    //instance.
  }

}

object HazelcastHost {
  private class startProgram (private val appId: String,
                              private val selfId: Int) extends Runnable with Serializable with HazelcastInstanceAware {

    @transient var instance: HazelcastInstance = null

    override def setHazelcastInstance(inst: HazelcastInstance) = instance = inst

    def getHost = instance.getConfig.asInstanceOf[DJModConfig].djhost

    override def run() = {
      instance.getCluster.getLocalMember.setIntAttribute("DJ_app_id_"+appId, selfId)
      val recvr = getHost.commMan.makeClientApplication(appId)
      val nc = getHost.getApplicationComm(appId, false, recvr)
      recvr.start(nc)
      println(s"-------------------> starting application: $appId on $selfId")
    }
  }



  private[network] class DJModConfig(val djhost: HazelcastHost) extends Config {
  }

}

object HazelcastComm {
  private class sendMessage (private val appId: String,
                             private val from: Int,
                             private val action: Int,
                             private val msgReplyId: Long,
                             private val msg: Array[Byte]) extends Runnable with Serializable with HazelcastInstanceAware {
    @transient var instance: HazelcastInstance = null

    override def setHazelcastInstance(inst: HazelcastInstance) = instance = inst

    def getHost = instance.getConfig.asInstanceOf[DJModConfig].djhost

    override def run() = {
      // TODO: optimize this so that we are not doing a hashtable lookup on the receving end
      // could have the apps have some static int and just hold them in an array or something
      //println(s"========================== running on remote $from $action")
      getHost.running_apps.get(appId) match {
        case Some(app) => app.processMsg(from, action, msg, msgReplyId)
        case None => throw new RuntimeException(s"unable to find application $appId on $instance")
      }
    }
  }

  private class sendReply(private val appId: String,
                          private val from: Int,
                          private val replyId: Long,
                          private val msg: Array[Byte],
                          private val error: Throwable) extends Runnable with Serializable with HazelcastInstanceAware {
    @transient var instance: HazelcastInstance = null

    override def setHazelcastInstance(inst: HazelcastInstance) = instance = inst

    def getHost = instance.getConfig.asInstanceOf[DJModConfig].djhost

    override def run() = {
      getHost.running_apps.get(appId) match {
        case Some(app) => app.processReply(from, replyId, msg, error)
        case None => ???
      }
    }
  }
}

// dummy test server that echos replies back to whoeversent it
object testd {
  def main(args: Array[String]) = {
    val comm = new NetworkManager("test"*3, "hazelcast") {
      override def makeClientApplication(identifier: String): NetworkRecever = {
        println(s"making new echo client for $identifier")
        new NetworkRecever {
          // for recving when there will be no reply
          override def recv(from: Int, action: Int, msg: Array[Byte]): Unit = {
            println(s"echo from: $from, action: $action, msg: ${new String(msg)}")
          }

          // for sending a reply back
          override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = {
            println(s"echo with rply, from: $from, action: $action, msg: ${new String(msg)}")
            Future {
              Thread.sleep(1000) // wait for 1 second
              msg
            }
          }

          override def start(nc: NetworkCommunication): Unit = {
            println(s"start with $identifier nop")
          }
        }
      }
    }
    comm.runClient
  }
}