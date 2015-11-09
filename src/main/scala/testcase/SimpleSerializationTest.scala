package testcase

import edu.berkeley.dj.internal.{JITCommands, InternalInterface}

/**
 * Created by matthewfl
 */
class SimpleSerializationTest {

  val test = 1

  var qwer = 123

  var gg = "something here"

}

object SimpleSerializationTest {

  def main(args: Array[String]): Unit = {

    // wait until we have another machine
    while(InternalInterface.getInternalInterface.getAllHosts.length == 1) {
      Thread.sleep(100)
    }

    println("before create")
    val o = new SimpleSerializationTest

    println("after create")

    JITCommands.moveObject(o, InternalInterface.getInternalInterface.getAllHosts()(1))

    println("after move")
    //val sm = SerializeManager.computeSize(o, 1)

    //println(sm)

    //SerializeManager.serialize(o, 2)

    //println(ss.position())

  }

}
