package edu.berkeley.dj.rt

import scala.collection.mutable

/**
  * Created by matthewfl
  */
class IOManager (val manager: Manager) {

  private val objs = new mutable.HashMap[Int,Any]()

  private var nextInt = 1

  def loader = manager.asInstanceOf[MasterManager].ioLoader

  def loadInterface(l: LoaderProxy) = {
    // provide the internal interface a m link to this class
    val wrapper = l.loadClass(manager.config.ioInternalPrefix + "IOInternalInterface")
    val c = wrapper.getDeclaredMethods.filter(_.getName == "setInternalInterface")(0)
    c.setAccessible(true)
    c.invoke(null, this)
  }

  def constructClass(classname: String, argsCls: Array[String], args: Array[Object]): Int = {
    val cls = loader.loadClass(classname)

    val argsClss = argsCls.map(findClass)

    val constructor = cls.getDeclaredConstructor(argsClss:_*)
    val obj = constructor.newInstance(args:_*)

    val id = this.synchronized {
      val i = nextInt
      nextInt += 1
      objs.put(i, obj)
      i
    }

    id
  }

  def callMethod(objectId: Int, methodName: String, argsCls: Array[String], args: Array[Object]): Any = {
    val obj = this.synchronized {
      objs.get(objectId).orNull
    }

    assert(obj != null, "object was not found")

    val cls = obj.getClass

    val argsClss = argsCls.map(findClass)

    val mth = cls.getDeclaredMethod(methodName, argsClss:_*)

    val ret = mth.invoke(obj, args:_*)

    ret
  }

  private def findClass(classname: String): Class[_] = classname match {
    case "byte" => classOf[Byte]
    case "short" => classOf[Short]
    case "char" => classOf[Char]
    case "int" => classOf[Int]
    case "long" => classOf[Long]
    case "float" => classOf[Float]
    case "double" => classOf[Double]
    case v => loader.loadClass(v)
  }
}
//
//class IOLoader(val manager: Manager) extends LoaderProxy(manager, ClassPool.getDefault) {
//
//  override def getClassBytes(classname: String): Array[Byte] = {
//    manager.loader.getIOClassBytes(classname)
//  }
//
//}