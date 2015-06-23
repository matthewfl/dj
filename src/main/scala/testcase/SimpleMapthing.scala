package testcase

import edu.berkeley.dj.internal.{DistributedRunner, InternalInterface}

/**
 * Created by matthewfl
 */

case class Struct(val a: Int, val b: Int)

object SimpleMapthing {

  def main(args: Array[String]) = {

    // wait until there is a second machine running
    while(InternalInterface.getInternalInterface.getAllHosts.length == 1) {
      Thread.sleep(1000)
    }

    for(h <- InternalInterface.getInternalInterface.getAllHosts) {
      println(s"see host $h")
      if(InternalInterface.getInternalInterface.getSelfId != h) {
        // going to try and run a command on an external machine
        val r = new Runnable {

          var v = 5

          override def run(): Unit = {
            InternalInterface.debug("\n\n\n\n\n--------------------> running on some remote host\n\n\n\n" + v)
            System.out.println("Another printing on the remote host")
            v = 6
          }
        }
        DistributedRunner.runOnRemote(h, r)
        Thread.sleep(1500)
        InternalInterface.debug("\n\n\n------New value for v"+r.v)
      }
    }
    Thread.sleep(120000)


    /*

    Thread.sleep(120000);

    val map = new util.TreeMap[Int, Struct]()

    for(i <- 0 until 10000) {
      map.put(i, new Struct(i * 2, i*3))
    }

    println("the simple map thing has finished")

    Thread.sleep(120000);

*/

  }

}
