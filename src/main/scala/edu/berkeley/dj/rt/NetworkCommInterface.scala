package edu.berkeley.dj.rt

import java.nio.ByteBuffer
import java.util.UUID

import edu.berkeley.dj.internal.NetworkForwardRequest
import edu.berkeley.dj.rt.network.{RedirectRequestToAlternateMachine, NetworkCommunication, NetworkRecever}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by matthewfl
 *
 * The interface for receving messages from other nodes on the network
 */
class NetworkCommInterface(private val man: Manager) extends NetworkRecever {

  private implicit def arr2Future(a: Array[Byte]) = Future.successful(a)

  override def recv(from: Int, action: Int, msg: Array[Byte]) = try {
    action match {
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
        //Future
        man.threadPool.submit {
          man.runningInterface.callIn(3, from, msg)
        }
      }
      case 104 => {
        // update this machine with a new location for an object
        val bb = ByteBuffer.wrap(msg)
        man.runningInterface.callIn(4, new UUID(bb.getLong(), bb.getLong()), bb.getInt)
      }
      case 105 => {
        // notifiy the master object that some other host is waiting on it
        man.runningInterface.callIn(7, from, ByteBuffer.wrap(msg))
      }
      case 106 => {
        // unlock a monitor with some count of number of notifications
        man.runningInterface.callIn(9, ByteBuffer.wrap(msg))
      }
      case 107 => {
        // recv a notification on an object
        man.runningInterface.callIn(10, ByteBuffer.wrap(msg))
      }
      case 108 => {
        // recv a write on the static fields
        man.runningInterface.callIn(12, ByteBuffer.wrap(msg))
      }
      case 109 => {
        // cache the bytecode for something
        val bb = ByteBuffer.wrap(msg)
        val namel = bb.getInt()
        val name = new String(msg, 4, namel)
        val clsa = new Array[Byte](msg.length - 4 - namel)
        Array.copy(msg, 4 + namel, clsa, 0, clsa.length)
        val cache = man.loader.asInstanceOf[RemoteLoaderProxy].classByteCache
        man.loader.synchronized {
          cache += (name -> clsa)
        }
      }
      case 110 => {
        // reload a class
        man.loader.reloadClass(new String(msg))
      }
      case 111 => {
        // request to move an object
        man.runningInterface.callIn(14, ByteBuffer.wrap(msg))
      }
      case 112 => {
        // recv a serialized object either for caching or moving
        man.runningInterface.callIn(15, ByteBuffer.wrap(msg))
      }
    }
  } catch {
    case e: NetworkForwardRequest => {
      throw new RedirectRequestToAlternateMachine(e.to)
    }
    case e: Throwable => {
      System.err.println(s"There was an error with command: $action\n$e")
      e.printStackTrace(System.err)
    }
  }

    override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = try {
      action match {
        case 1 => {
          // load a class from the parent class loader
          if (man.isMaster) {
            val cname = new String(msg)
            Future.successful(man.asInstanceOf[MasterManager].loader.getClassBytes(cname))
          } else {
            Future.failed(new RuntimeException("This is not the master machine"))
          }
        }
        case 2 => {
          // check if we need to rename this class
          val nname = man.classRename(new String(msg))
          if (nname == null)
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
          //??? // there was a bug here, idk if anything is using this
          val name = new String(msg, 4, len)
          val arr = new Array[Byte](msg.length - len - 4)
          Array.copy(msg, 4 + len, arr, 0, arr.length)
          man.runningInterface.setDistributed(name, arr)
          Array[Byte]()
        }
        case 6 => {
          // get a byte array from the map
          val name = new String(msg)
          man.runningInterface.getDistributed(name)
        }
        case 7 => {
          // lock an object
          val res = man.runningInterface.callIn(8, ByteBuffer.wrap(msg), false).asInstanceOf[Boolean]
          if (!res) {
            // we did not lock
            Future {
              // this will block until it gets the lock
              man.runningInterface.callIn(8, ByteBuffer.wrap(msg), true)
              Array[Byte]()
            }
          } else {
            // we got the lock send reply
            Array[Byte]()
          }
        }
        case 8 => {
          // make the mode on a class distributed
          man.runningInterface.typeDistributed(new String(msg))
          Array[Byte]()
        }
        case 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 => {
          // reading something that is local on this machine
          man.runningInterface.callIn(5, action, ByteBuffer.wrap(msg)).asInstanceOf[ByteBuffer].array()
        }
        case 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 => {
          man.runningInterface.callIn(6, action, ByteBuffer.wrap(msg))
          Array[Byte]()
        }
        case 30 => {
          // get the values of all static fields on a class
          val cname = new String(msg)
          man.runningInterface.callIn(11, cname).asInstanceOf[Array[Byte]]
        }
        case 31 => {
          // perform an rpc call to this machine
          man.runningInterface.callIn(13, ByteBuffer.wrap(msg)).asInstanceOf[ByteBuffer].array()
        }
      }
    } catch {
      case e: NetworkForwardRequest => {
        throw new RedirectRequestToAlternateMachine(e.to)
      }
      case e: Throwable => {
        System.err.println(s"There was an error with command $action\n$e")
        e.printStackTrace(System.err)
        Future.failed(e)
      }
    }

  override def start(nc: NetworkCommunication) = {
      man.networkInterface = nc
      man.start
    }

  }
