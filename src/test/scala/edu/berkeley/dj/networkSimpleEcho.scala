package edu.berkeley.dj

import org.scalatest.FunSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Created by matthewfl
 */
class networkSimpleEcho extends FunSuite {

  ignore ("simple start application") {
    import edu.berkeley.dj.rt.network._
    val gg = new HazelcastHost("test"*3, null)
    val appcomm = gg.getApplicationComm("test123", true, new NetworkRecever {
      // for recving when there will be no reply
      override def recv(from: Int, action: Int, msg: Array[Byte]): Unit = ??? // we are not going to expect for there to be any msg returned

      // for sending a reply back
      override def recvWrpl(from: Int, action: Int, msg: Array[Byte]): Future[Array[Byte]] = ??? // we are also not going to get back some request for data

      override def start(nc: NetworkCommunication): Unit = {}
    })
    gg.createNewApp("test123")

    for(h <- appcomm.getAllHosts) {
      if(h != appcomm.getSelfId)
        appcomm.send(h, 1, "some test msg".getBytes)
    }

    for(h <- appcomm.getAllHosts) {
      if(h != appcomm.getSelfId) {
        val res = appcomm.sendWrpl(h, 2, "test msg".getBytes)
        assert(new String(Await.result(res, 15 seconds)) == "test msg")
      }
    }

    Thread.sleep(5000)
  }

}
