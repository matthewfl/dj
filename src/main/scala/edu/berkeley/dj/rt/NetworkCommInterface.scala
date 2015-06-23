package edu.berkeley.dj.rt

import java.nio.ByteBuffer
import java.util.UUID

import edu.berkeley.dj.rt.network.{NetworkCommunication, NetworkRecever}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by matthewfl
 *
 * The interface for receving messages from other nodes on the network
 */
class NetworkCommInterface(private val man: Manager) extends NetworkRecever {

  private implicit def arr2Future(a: Array[Byte]) = Future.successful(a)

  override def recv(from: Int, action: Int, msg: Array[Byte]) = action match {
    case 101 => {
      // exit the program
      System.exit(msg(0))
    }
    case 102 => {
      // register that a new client is present and ready
      man.runningInterface.callIn(2, from)
    }
    case 103 => {
      // run a command on this machine as send by a remote machine
      Future {
        man.runningInterface.callIn(3, from, msg)
      }
    }
    case 104 => {
      // update this machine with a new location for an object
      val bb = ByteBuffer.wrap(msg)
      man.runningInterface.callIn(4, new UUID(bb.getLong(), bb.getLong()), bb.getInt)
    }
    case 105 => {
      man.runningInterface.callIn(7, from, ByteBuffer.wrap(msg))
    }
  }

  override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = action match {
    case 1 => {
      // load a class from the parent class loader
      if(man.isMaster) {
        val cname = new String(msg)
        Future.successful(man.asInstanceOf[MasterManager].loader.getClassBytes(cname))
      } else {
        Future.failed(new RuntimeException("This is not the master machine"))
      }
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
      man.runningInterface.lock(lname) match {
        case true => Array[Byte](1)
        case false => Array[Byte](0)
      }
    }
    case 4 => {
      // unlock a given string
      val lname = new String(msg)
      man.runningInterface.unlock(lname)
      Array[Byte]()
    }
    case 5 => {
      // set a byte array for the distributed map
      val buff = ByteBuffer.wrap(msg)
      val len = buff.getInt()
      val name = new String(msg, 0, len)
      val arr = new Array[Byte](msg.length - len - 4)
      Array.copy(msg, 4, arr, 0, arr.length)
      man.runningInterface.setDistributed(name, arr)
      Array[Byte]()
    }
    case 6 => {
      // get a byte array from the map
      val name = new String(msg)
      man.runningInterface.getDistributed(name)
    }
    case 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 => {
      // reading something that is local on this machine
      man.runningInterface.callIn(5, action, ByteBuffer.wrap(msg)).asInstanceOf[ByteBuffer].array()
    }
    case 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 => {
      man.runningInterface.callIn(6, action, ByteBuffer.wrap(msg))
      Array[Byte]()
    }

  }

  override def start(nc: NetworkCommunication) = {
    man.networkInterface = nc
    man.start
  }

}
