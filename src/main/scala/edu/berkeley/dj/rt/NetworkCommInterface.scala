package edu.berkeley.dj.rt

import edu.berkeley.dj.rt.network.{NetworkCommunication, NetworkRecever}

import scala.concurrent.Future

/**
 * Created by matthewfl
 *
 * The interface for receving messages from other nodes on the network
 */
class NetworkCommInterface(private val man: Manager) extends NetworkRecever {

  override def recv(from: Int, action: Int, msg: Array[Byte]) = action match {
    case 1 => {
      // exit the program
      System.exit(msg(0))
    }

  }

  override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = action match {
    case 1 => {
      // load a class from the parent class loader
      val cname = new String(msg)
      Future.successful(man.asInstanceOf[MasterManager].loader.getClassBytes(cname))
    }
    case 2 => {
      // check if we need to rename this class
      val nname = man.classRename(new String(msg))
      if(nname == null)
        Future.successful(Array[Byte]())
      else
        Future.successful(nname.getBytes)
    }
  }

  override def start(nc: NetworkCommunication) = {
    man.networkInterface = nc
    man.start
  }

}
