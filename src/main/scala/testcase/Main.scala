package testcase

import edu.berkeley.dj.internal.InternalInterface
/**
 * Created by matthewfl
 */

class Main {

  def something = {
    println(qwer)
    notifyAll()
  }

  var qwer = 123
}

object Main {
  def main(args : Array[String]) = {

    println("testcase class loader is: "+classOf[Main].getClassLoader.toString)

    println("this is the test case main class")

    val i = InternalInterface.getInternalInterface();
    println(i.getUUID)

    println("main fields:\n\n")
    classOf[testcase.Main].getFields.foreach(println(_))

    val mm = new Main

    println("external read"+mm.qwer)

    mm.qwer = 456

    mm.something


  }
}
