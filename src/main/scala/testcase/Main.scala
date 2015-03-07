package testcase

import edu.berkeley.dj.internal.InternalInterface
/**
 * Created by matthewfl
 */
object Main {
  def main(args : Array[String]) = {
    println("this is the test case main class")

    val i = InternalInterface.getInternalInterface();
    println(i.getUUID)

  }
}
