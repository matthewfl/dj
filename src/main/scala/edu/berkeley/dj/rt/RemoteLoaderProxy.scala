package edu.berkeley.dj.rt

import javassist.ClassPool

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
    // this causes the system to freeze for some reason
    /*this.synchronized {
      val res = classByteCache.get(classname) // we aren't going to end up needing the bytes twice so we can delete it now
      res match {
        case Some(b) => {
          classByteCache.remove(classname)
          return b
        }
      }
    }*/
    println(s"remote loading $classname")
    Await.result(manager.networkInterface.sendWrpl(0, 1, classname.getBytes), 600 seconds)
  }

  val classByteCache = new collection.mutable.HashMap[String, Array[Byte]]()

}
