package edu.berkeley.dj.rt.network

import com.hazelcast.config.{MulticastConfig, TcpIpConfig, ListenerConfig, Config}
import com.hazelcast.core.{MemberAttributeEvent, MembershipEvent, MembershipListener, Hazelcast}

import scala.collection.JavaConversions._

/**
 * Created by matthewfl
 */
class HazelcastComm {


}

class HazelcastHost(private val gcode: String) extends NetworkHost {

  assert(gcode.length > 10)

  private val hconfig = new Config()
  hconfig.getGroupConfig.setName("DJ_cluster_"+gcode.substring(0, 5))
  hconfig.getGroupConfig.setPassword(gcode)

  // for the master node where we are presenting the logging of the application instead of the network
  //hconfig.setProperties("hazelcast.logging.type", "none")

  hconfig.getNetworkConfig.getJoin.setTcpIpConfig(new TcpIpConfig {
    // TODO: take this configuration from the command line
    addMember("10.7.0.5,10.7.0.1,10.7.0.17")
    setEnabled(true)
  })
  hconfig.getNetworkConfig.getJoin.setMulticastConfig(new MulticastConfig {
    setEnabled(false)
  })
  hconfig.addListenerConfig(new ListenerConfig() {
    setImplementation(new MembershipListener {override def memberAttributeChanged(memberAttributeEvent: MemberAttributeEvent): Unit = ???

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

  private val instance = Hazelcast.newHazelcastInstance(hconfig)

  override def getApplicationComm(identifier: String, isMaster: Boolean, recever: NetworkRecever): NetworkCommunication = ???

  override def createNewApp(identifier: String) = {
    val otherMembers = instance.getCluster.getMembers.filter(_ != instance.getCluster.getLocalMember)
    val exec = instance.getExecutorService("start-app")
    for(m <- instance.getCluster.getMembers) {
      if(m != instance.getCluster.getLocalMember) {

      }
    }
  }

  override def runClient(man: NetworkManager) = {
    //instance.
  }

}
