package edu.berkeley.dj.rt

import javassist.{Loader, ClassPool}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by matthewfl
 */
class RemoteLoaderProxy (private val manager: Manager, private val pool: ClassPool) extends LoaderProxy(manager, pool) {

  // this should cache classes as they are loaded, so there should be some multicast message that tells
  // the 

  override def getClassBytes(classname: String): Array[Byte] = {
    // this calls back to the master and loads the content of some class
    //throw new NotImplementedError()
    Await.result(manager.networkInterface.sendWrpl(0, 1, classname.getBytes), 180 seconds)
  }

}
