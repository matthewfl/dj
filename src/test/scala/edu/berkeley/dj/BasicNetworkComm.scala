package edu.berkeley.dj

import edu.berkeley.dj.rt.network.{NetworkRecever, NetworkManager, NetworkCommunication}
import org.scalatest.FunSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by matthewfl
 */
class BasicNetworkComm extends FunSuite {

  var receved_new_app = false
  var app_started = false

  def nm_setup = {
    val nm = new NetworkManager("test", "dummy")
    val nm2 = new NetworkManager("test", "dummy") {
      override def makeClientApplication(id: String) = {
        receved_new_app = true
        new NetworkRecever {
          // for recving when there will be no reply
          override def recv(from: Int, action: Int, msg: Array[Byte]): Unit = ???

          // for sending a reply back
          override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = ???

          override def start: Unit = {
            app_started = true
          }
        }
      }
    }

    Future { nm2.runClient }

    (nm, nm2)
  }


  test("simple set up client") {
    val (nm, nm2) = nm_setup
    val network = nm.getApplication("test-app", true, new NetworkRecever {// for recving when there will be no reply
    override def recv(from: Int, action: Int, msg: Array[Byte]): Unit = ???

      // for sending a reply back
      override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = ???

      override def start: Unit = ???
    })

    assert(receved_new_app == false)

    nm.createNewApp("test-app")

    val r = Future {
      for(i <- 0 until 1000; if receved_new_app == false) {
        Thread.sleep(1)
      }
      receved_new_app
    }

    assert(Await.result(r, 1500 millisecond) == true)
  }

}
