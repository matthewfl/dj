package edu.berkeley.dj.rt

import java.lang.reflect.Modifier

import edu.berkeley.dj.internal.{CONSTS, DJIO}

import scala.collection.mutable

/**
  * Created by matthewfl
  */
class IOManager (val manager: Manager) {

  private val objs = new mutable.HashMap[Int,Any]()

  // what DJ object this corresponds to
  private val robjs = new mutable.HashMap[Any,Array[Byte]]()

  private var nextInt = 1

  private def unsafe = Unsafe.theUnsafe

  lazy val loader = {
    val r = manager.ioLoader
    manager.io.loadInterface(r)
    r
  }

  def djLoader = manager.loader

  def config = manager.config

  def loadInterface(l: LoaderProxy) = {
    // provide the internal interface a m link to this class
    val wrapper = l.loadClass(manager.config.ioInternalPrefix + "IOInternalInterface")
    val c = wrapper.getDeclaredMethods.filter(_.getName == "setInternalInterface")(0)
    c.setAccessible(true)
    c.invoke(null, this)
  }

  def constructClass(classname: String, argsCls: Array[String], args: Array[Object], self: Array[Byte]): Int = {
    val cls = loader.loadClass(classname)

    val argsClss = argsCls.map(findClass)

    val argsC = args.map(convertFromDJ)

    val constructor = cls.getDeclaredConstructor(argsClss:_*)
    val obj = constructor.newInstance(argsC:_*)

    val id = this.synchronized {
      val i = nextInt
      nextInt += 1
      objs.put(i, obj)
      robjs.put(obj, self)
      i
    }

    id
  }

  /*
  Call method from DJ into runtime
   */
  def callMethod(objectId: Int, methodName: String, argsCls: Array[String], args: Array[Object]): Any = {
    val obj = this.synchronized {
      objs.get(objectId).orNull
    }

    assert(obj != null, "object was not found")

    val cls = obj.getClass

    val argsClss = argsCls.map(findClass)

    val argsC = args.map(convertFromDJ)

    val mth = cls.getDeclaredMethod(methodName, argsClss:_*)

    val ret = mth.invoke(obj, argsC:_*)

    convertToDJ(ret)
  }

  def callDJMethod(wrapped: Object, method: String, argsType: Array[String], args: Array[Object]): Object = {
    val clsw = wrapped.getClass
    val argsT = argsType.map(findClassDJ)
    val mth = clsw.getDeclaredMethod(method, argsT:_*)
    val argsV = args.zip(argsT).map(v => {
      if(!v._2.isPrimitive)
        convertToDJ(v._1)
      else
        v._1
    })
    val res = mth.invoke(wrapped, argsV:_*)
    convertFromDJ(res)
  }

  private def findClassDJ(classname: String): Class[_] = classname match {
    case "byte" => classOf[Byte]
    case "short" => classOf[Short]
    case "char" => classOf[Char]
    case "int" => classOf[Int]
    case "long" => classOf[Long]
    case "float" => classOf[Float]
    case "double" => classOf[Double]
    case v => manager.loader.loadClass(v)
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

  private lazy val objectBase = djLoader.loadClass(config.internalPrefix+"ObjectBase")

  private def convertFromDJ(obj: Object): Object = {
    if(obj == null)
      return null

    val cls = obj.getClass
    val cname = cls.getName

    // this is from the system loader
    // this will cover things like String
    // TODO: should handle instances of Class<?> to make sure that they are referenced to the new loader
    if(cls.getClassLoader == null)
      return obj

    // check that this is a subclass of the ObjectBase class
    // this will throw cast exception otherwise
    cls.asSubclass(objectBase)


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
          ???
        }
      }
      // TODO: handle other cases
      ???
    }

    val unprefix_name = if(cname.startsWith(config.coreprefix))
      cname.drop(config.coreprefix.length) else cname

    val protected_name = protectedName(unprefix_name)

    val new_name = if(protected_name)
      unprefix_name
    else
      unprefix_name + config.ioProxySuffix

//    val new_name = if(cname.startsWith(config.coreprefix)) {
//      // this must be some java like class that we had to rewrite
//      val name = cname.drop(config.coreprefix.length)
//      val lbd =
//      if(lbd)
//        name
//      else
//        name + config.ioProxySuffix
//    } else {
//      // we are going to construct a proxy of this class
//      // this will allow for values to be lazy converted
//      // and calling methods back on the JIT
//      cname + config.ioProxySuffix // "_dj_io_proxy"
//    }

    val new_cls = loader.loadClass(new_name)

    val new_instance = Unsafe.theUnsafe.allocateInstance(new_cls)

    if(protected_name) {
      // then we have to copy over all the fields
      ???
    } else {
      new_cls.getDeclaredField("__dj_io_wraps").set(new_instance, obj)
    }
/*
    if (cname.startsWith(config.coreprefix)) {
      val acls = new mutable.MutableList[Class[_]]
      var ccls = cls
      while (ccls != objectBase) {
        acls += ccls
//        ccls = ccls.getSuperclass
        ???
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

    }*/

    return new_instance

  }

  private def convertToDJ(obj: Object): Object = {
    // if it is some primitive type that we allowed, then just pass it through
    // if it is an instance of DJIO then we can wrap the class and pass it back
    // if the class is currently a io_proxy
    if(obj == null) {
      return null
    }
    val cls = obj.getClass()
    val cls_name = cls.getName
    if(cls_name.endsWith(config.ioProxySuffix)) {
      return cls.getDeclaredField("__dj_io_wraps").get(obj)
    }
    val is_io = cls.getAnnotation(classOf[DJIO]) != null
    if(is_io) {
      val r = this.synchronized {
        robjs.get(obj)
      }
      r match {
        case None => {
          val intId = this.synchronized {
            val i = nextInt
            nextInt += 1
            i
          }
          // need to construct a new object and set it equal to this
          val dj_cls = manager.loader.loadClass(cls_name)
          val dj_inst = unsafe.allocateInstance(dj_cls)
          dj_cls.getField("__dj_io_object_id").setInt(dj_inst, intId)
          dj_cls.getField("__dj_io_owning_machine").setInt(dj_inst, manager.networkInterface.getSelfId)
          this.synchronized {
            objs.put(intId, obj)
            //robjs.put(obj, intId)
          }
          //return dj_cls
          ???
        }
        case Some(rr) => {
          ???
          // this needs to look up this object and
          return rr
        }
      }
    }
    val cname = cls.getName
    val protected_name = protectedName(cname)
    if(protected_name) {
      // then we need to see if we need to rewrite this class
      // or if we can just return it
      val nname = manager.classRename(cname.replace(".","/"))
      // we don't have to deal with renaming this class
      if(nname == null || nname.replace("/", ".") == cname)
        return obj
      val ncls = manager.loader.loadClass(nname.replace("/", "."))
      val ninst = unsafe.allocateInstance(ncls)
      // need to copy over any potential fields
      var cl = cls
      var ncl = ncls
      while(cl != classOf[Object]) {
        for(fid <- cl.getDeclaredFields) {
          if(!Modifier.isStatic(fid.getModifiers)) {
            fid.setAccessible(true)
            val nfid = ncl.getDeclaredField(fid.getName)
            nfid.setAccessible(true)
            if(fid.getType.isPrimitive)
              nfid.set(ninst, fid.get(obj))
            else
              nfid.set(ninst, convertToDJ(fid.get(obj)))
          }
        }
        cl = cl.getSuperclass.asInstanceOf[Class[_ <: Object]]
        ncl = ncl.getSuperclass
      }
      return ninst
//      ???
    }

    if(cls.isArray) {
      ???
    }
    ???
    null

  }

  private def protectedName(name: String) = {
    name.startsWith("java.") ||
      name.startsWith("javax.") ||
      name.startsWith("sun.") ||
      name.startsWith("com.sun.") ||
      name.startsWith("org.w3c.") ||
      name.startsWith("org.xml.")
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