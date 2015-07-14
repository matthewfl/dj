package testcase

import java.util.concurrent.Callable

import edu.berkeley.dj.internal.{ObjectBase, DistributedRunner, InternalInterface}

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
        /*val r = new Runnable {

          var v = 5

          def gg = v

          override def run(): Unit = {
            InternalInterface.debug("\n\n\n\n\n--------------------> running on some remote host\n\n\n\n" + v)
            this.synchronized {
              System.out.println("Another printing on the remote host")
            }
            v = 6
          }
        }
        DistributedRunner.runOnRemote(h, r)
        Thread.sleep(1500)
        */
        val synclock = new ObjectBase // TODO: automatically cast `new Object` to objectbase

        val c = new Callable[Int] {
          override def call = {
            synclock.synchronized { synclock.wait() }
            InternalInterface.debug("remote debug stuff")
            999
          }
        }

        val f = DistributedRunner.runOnRemote(h, c)

        Thread.sleep(1000)

        synclock.synchronized {
          synclock.notify()
        }

        InternalInterface.debug("got back the value: "+f.get())

        // TODO: issue with scala using reflection to get the value of r.v, will require something that
        // is remapping the reflection
        //val rr = r.gg
        //InternalInterface.debug("\n\n\n------New value for v"+r.v)
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
