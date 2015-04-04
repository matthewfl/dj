package testcase

import edu.berkeley.dj.internal.InternalInterface
/**
 * Created by matthewfl
 */

class Main {

  def something = {
    println(qwer456asdf)
    notifyAll()
  }

  var qwer456asdf = 123

  val asdfanthter = " something somewhere "
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

    println("external read"+mm.qwer456asdf)

    mm.qwer456asdf = 456

    mm.something


  }
}
