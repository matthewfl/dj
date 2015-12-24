package edu.berkeley.dj.rt

//import javassist.tools.reflect.{Loader,Metaobject}
//import javassist.{ClassPool,Translator}

import java.io.{File, FileOutputStream}
import java.net.URL
import java.util.UUID
import javassist._

import scala.collection.mutable


/**
 * Created by matthewfl
 */

/*class LoaderTranslator extends Translator {
  override def start(pool : ClassPool) = {}

  override def onLoad(pool : ClassPool, classname : String) = {
    println("on loading class: "+classname)
  }
}*/

/*class LoaderRunningMeta extends Metaobject {
  println("constructed")
}*/

class LoaderProxy(private val manager: Manager, private val pool: ClassPool, val debug_prefix: String="/")
  extends Loader(null, pool) {

  // called by the class reloader to check that we have the correct class
  val loaderUUID = "LD"+UUID.randomUUID().toString.replace("-","")

  //addTranslator(pool, new LoaderTranslator)


  // TODO: make this check that the class that is getting loaded is some basic class
  // eg java.lang, otherwise, when a class is not found, then it can start looking into
  // the upper level class path code, which contains this

  //override protected def delegateToParent(classname : String) = throw new ClassNotFoundException(classname)
  override  protected def delegateToParent(classname : String) = {
    //println("loading from parent class: "+classname)
    //assert(classname.startsWith("java.lang."))
    super.delegateToParent(classname)
  }

  private val classBytecodes = new mutable.HashMap[String,Array[Byte]]()

  // what about when we want to force some class to be reloaded with new bytecode...
  def getClassBytes(classname: String): Array[Byte] = {
    // TODO: more fine grain sync here
    // we should be able to load more then one class at a time
    // but we would like to avoid loading two instances of the same class at the same time since that
    // can lead to strange behavior
    classBytecodes.synchronized {
      classBytecodes.getOrElseUpdate(classname, {
        val cls = pool get classname
        if (cls != null) {
          cls.detach()
          val clazz = cls.toBytecode()
          if (manager.config.debug_clazz_bytecode != null) {
            val fl = new File(s"${manager.config.debug_clazz_bytecode}${debug_prefix}${classname.replace(".", "/")}.class")
            fl.getParentFile.mkdirs()
            val f = new FileOutputStream(fl)
            f.write(clazz)
            f.close()
          }
          clazz
        } else null
      })
    }
  }

  private val definedClasses = new mutable.HashSet[String]()

  override protected def findClass(classname: String) : Class[_] = {
    // the java.* classes can not be rewritten by us, also they contain a special meaning between the
    // jvm and the running program, so modification becomes an issue
    val lbd = loadClassByDelegation(classname)
    if(lbd != null)
      return lbd

    //println("loading class: "+classname)
    val clazz : Array[Byte] = getClassBytes(classname)
    try {
      val lindx = classname.lastIndexOf(".")
      if (lindx != -1) {
        val pkgname = classname.substring(0, lindx)
        if (getPackage(pkgname) == null)
          definePackage(pkgname, null, null, null, null, null, null, null)
      }
      val dcls = defineClass(classname, clazz, 0, clazz.length, manager.protectionDomain)
      resolveClass(dcls)
      definedClasses.synchronized { definedClasses += classname }
      dcls
    } catch {
      case e : IncompatibleClassChangeError => {
        System.err.println("=======================failed to redefine the class "+classname)
        null
      }
      case e: Throwable => {
        System.err.println("========================o come on\n"+e.toString)
        throw e
        null
      }
    }
  }

  def reloadClass(name: String): Unit = {
    if(!ClassReloader.enabled)
      return
    val defined = definedClasses.synchronized { definedClasses.contains(name) }
    if(defined) {
      // if the class is not loaded, then we must not currently need it
      // so there is no need to attempt to redefine it
      classBytecodes.synchronized {
        classBytecodes.remove(name)
        pool.asInstanceOf[ClassPoolProxy].setClass(name, null)
      }
      val nbytes = getClassBytes(name)
      ClassReloader.reloadClass(this, name, nbytes)
    }
  }

  def classLoaded(name: String) = definedClasses.synchronized { definedClasses.contains(name) }

  // make this load all of the classes
  // and not pass to the parent the class in the java. namespace
  // some classes can only be loaded by the parent loader
  //doDelegation = false

  /*override def getResource(url: String) : URL = {

  }*/

  /*override def getResources(url: String) : Enumeration[URL] = {
    java.util.Collections.emptyEnumeration
  }*/

  // TODO: need to be able to access resources that are in the jar files for a given program
  override def findResource(name: String) : URL = {
    println("res: "+name)
    null
  }

}
