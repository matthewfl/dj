package testcase

import java.math.BigInteger
import java.util.concurrent.Callable

import edu.berkeley.dj.internal.{CONSTS, DistributedRunner, InternalInterface, ObjectBase}

/**
 * Created by matthewfl
 */

case class Struct(val a: Int, val b: Int)

class makeThisRpc {

  var someVal: Int = 6

  def something = {
    for(a <- 0 until someVal) {
      someVal += 99
    }
  }

}

object SimpleMapthing {

  var someTest = new Struct(1,2)

  def main(args: Array[String]) = {

    someTest = new Struct(3,4)

    // wait until there is a second machine running
    while(InternalInterface.getInternalInterface.getAllHosts.length == 1) {
      Thread.sleep(1000)
    }

    val thing = new makeThisRpc


    Class.forName("edu.berkeley.dj.internal.arrayclazz.edu.berkeley.dj.internal.coreclazz.java.math.BigInteger_1")
    Class.forName("edu.berkeley.dj.internal.arrayclazz.edu.berkeley.dj.internal.coreclazz.java.math.BigInteger_2")
    Class.forName("edu.berkeley.dj.internal.arrayclazz.edu.berkeley.dj.internal.coreclazz.java.math.BigInteger_impl_1")
    Class.forName("edu.berkeley.dj.internal.coreclazz.java.math.BigInteger")
    val bi = BigInteger.valueOf(0)



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
            // hack to change the mode so we can see what redirects are taking place
            thing.asInstanceOf[ObjectBase].__dj_class_mode |= CONSTS.PERFORM_RPC_REDIRECTS

            InternalInterface.debug("remote debug stuff")
            InternalInterface.debug("the some stuff is "+someTest.a)
            InternalInterface.debug("before value: "+thing.someVal)
            thing.something
            InternalInterface.debug("after val:"+thing.someVal)
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
