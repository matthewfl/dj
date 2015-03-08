package testcase

import edu.berkeley.dj.internal.InternalInterface
/**
 * Created by matthewfl
 */

class Main {}

object Main {
  def main(args : Array[String]) = {

    println("testcase class loader is: "+classOf[Main].getClassLoader.toString)

    println("this is the test case main class")

    val i = InternalInterface.getInternalInterface();
    println(i.getUUID)

    val f = classOf[testcase.Main].getFields

    println(f.length)

  }
}
