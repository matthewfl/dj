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
    case 101 => {
      // exit the program
      System.exit(msg(0))
    }
    case 102 => {
      // register that a new client is present and ready
      man.runningInterface.callIn(2, from)
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
    case 3 => {
      // lock a given string
      val lname = new String(msg)
      man.runningInterface.lock(lname)
    }
    case 4 => {
      // unlock a given string
      val lname = new String(msg)
      man.runningInterface.unlock(lname)
      Array[Byte]()
    }
    case 5 => {
      // set a byte array for the distributed map

    }
    case 6 => {
      // get a byte array from the map
      val name = new String(msg)
      man.runningInterface.getDistributed(name)
    }
  }

  implicit def booleanToArr(b: Boolean) = b match {
    case true => Array[Byte](1)
    case false => Array[Byte](0)
  }

  override def start(nc: NetworkCommunication) = {
    man.networkInterface = nc
    man.start
  }

}
