package testcase

import edu.berkeley.dj.internal.{ObjectBase, JITCommands, InternalInterface}

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

    // look up the other machine id
    val target: Int = InternalInterface.getInternalInterface.getAllHosts
      .filter(i => i != InternalInterface.getInternalInterface.getSelfId)(0)

    println("before create")
    val o = new SimpleSerializationTest

    println("after create")

    JITCommands.moveObject(o, target)

    println("after move")

    // have to wait until the object has been moved
    Thread.sleep(10 * 1000)


    // the object should have been moved by now
    val ob = o.asInstanceOf[Object].asInstanceOf[ObjectBase]

    println("owner: "+ob.__dj_class_manager.getOwner)
    println("gg val: "+o.gg)



    //val sm = SerializeManager.computeSize(o, 1)

    //println(sm)

    //SerializeManager.serialize(o, 2)

    //println(ss.position())

  }

}
