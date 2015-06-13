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
  var receved_bytes = false

  def nm_setup = {
    val nm = new NetworkManager("testtesttest", "dummy")
    val nm2 = new NetworkManager("testtesttest", "dummy") {
      override def makeClientApplication(id: String) = {
        receved_new_app = true
        new NetworkRecever {
          // for recving when there will be no reply
          override def recv(from: Int, action: Int, msg: Array[Byte]): Unit = {
            assert(action == 2)
            assert(from == 0)
            assert(msg.length == 3)
            assert(msg(0) == 1)
            assert(msg(1) == 2)
            assert(msg(2) == 3)
            receved_bytes = true
          }

          // for sending a reply back
          override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = {
            assert(from == 0)
            assert(action == 1)
            assert(new String(msg).equals("test123"))
            Future.successful(Array[Byte](6,5,4))
          }

          override def start: Unit = {
            app_started = true
          }
        }
      }
    }

    Future { nm2.runClient }

    (nm, nm2)
  }

  def wait_till_true(func: => Boolean): Boolean = {
    for(i <- 0 until 1000; if func == false) {
      Thread.sleep(3)
    }
    func
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

    assert(wait_till_true(receved_new_app))

    assert(wait_till_true(app_started))

    for(h <- network.getAllHosts) {
      if(h != network.getSelfId)
        network.send(h, 2, Array[Byte](1,2,3))
    }

    assert(wait_till_true(receved_bytes))

    var r : Future[Array[Byte]] = null
    for(h <- network.getAllHosts) {
      if(h != network.getSelfId)
        r = network.sendWrpl(h, 1, "test123".getBytes())
    }

    val arr = Await.result(r, 1500 millisecond)
    assert(arr.length == 3)
    assert(arr(0) == 6)
    assert(arr(1) == 5)
    assert(arr(2) == 4)

    nm.createNewApp("exit")

  }

}
