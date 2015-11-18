package edu.berkeley.dj.rt

import edu.berkeley.dj.internal.CONSTS

import scala.collection.mutable

/**
  * Created by matthewfl
  */
class IOManager (val manager: Manager) {

  private val objs = new mutable.HashMap[Int,Any]()

  private var nextInt = 1

  def loader = manager.asInstanceOf[MasterManager].ioLoader

  def djLoader = manager.loader

  def config = manager.config

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

  lazy val objectBase = djLoader.loadClass(config.internalPrefix+"ObjectBase")

  private def convertFromDJ(obj: Any): Any = {
    val cls = obj.getClass
    val cname = cls.getName

    // this is from the system loader
    // this will cover things like String
    // TODO: should handle instances of Class<?> to make sure that they are referenced to the new loader
    if(cls.getClassLoader == null)
      return obj

    if(!cls.isInstance(objectBase))
      ???

    // this must be from our internal DJ rewritten class

    val class_mode: Int = cls.getField("__dj_class_mode").getInt(obj)

    if (cname.startsWith(config.arrayprefix)) {
      val arr_field = cls.getDeclaredField("ir")
      val arr_type = arr_field.getType
      if ((class_mode & CONSTS.REMOTE_READS) != 0) {
        // we have to perform the remote reads to load the array...
        // would be nice if we could just move the master
        // and then have the array here
        ???
      } else {
        // I guess that we can directly access the array then
        if (arr_type.isPrimitive) {
          // so this allows for two direction changes....
          return arr_field.get("ir")
        } else {
          // need to make a copy of the array and convert all the objects
        }
      }
      // TODO: handle other cases
      ???
    }

    val new_name = if(cname.startsWith(config.coreprefix)) {
      // this must be some java like class that we had to rewrite
      cname.drop(config.coreprefix.length)
    } else {
      // we are going to construct a proxy of this class
      // this will allow for values to be lazy converted
      // and calling methods back on the JIT
      cname + config.ioProxySuffix // "_dj_io_proxy"
    }

    val new_cls = loader.loadClass(new_name)

    val new_instance = Unsafe.theUnsafe.allocateInstance(new_cls)

    if (cname.startsWith(config.coreprefix)) {
      val acls = new mutable.MutableList[Class[_]]
      var ccls = cls
      while (ccls != objectBase) {
        acls += ccls
        ccls = ccls.getSuperclass
      }
      val afields = acls.flatMap(_.getDeclaredFields)
      for(field <- afields)
        field.setAccessible(true)

      // this is somehow a rewritten class
      val new_name = cname.drop(config.coreprefix.length)
      val new_cls = loader.loadClass(new_name)
      for(field <- afields) {
        ???
      }
      ???

    }



    ???
  }

  private def converntToDJ(obj: Any) = {
    // if it is some primitive type that we allowed, then just pass it through
    // if it is an instance of DJIO then we can wrap the class and pass it back
    // if the class is currently a io_proxy
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