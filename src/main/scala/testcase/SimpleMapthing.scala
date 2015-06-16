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
        DistributedRunner.runOnRemote(h, new Runnable {
          override def run(): Unit = {
            println("--------------------> running on some remote host")
          }
        })
      }
    }
    Thread.sleep(60000)


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
