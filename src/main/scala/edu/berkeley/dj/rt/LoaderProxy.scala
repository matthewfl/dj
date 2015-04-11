package edu.berkeley.dj.rt

//import javassist.tools.reflect.{Loader,Metaobject}
//import javassist.{ClassPool,Translator}

import java.io.{File, FileOutputStream}
import java.lang.reflect.InvocationTargetException
import java.net.URL
import javassist._


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

class LoaderProxy(private val manager : Manager, private val pool : ClassPoolProxy)
  extends Loader(null, pool) {

  //addTranslator(pool, new LoaderTranslator)


  // TODO: make this check that the class that is getting loaded is some basic class
  // eg java.lang, otherwise, when a class is not found, then it can start looking into
  // the upper level class path code, which contains this

  //override protected def delegateToParent(classname : String) = throw new ClassNotFoundException(classname)
  override  protected def delegateToParent(classname : String) = {
    //println("loading from parent class: "+classname)
    super.delegateToParent(classname)
  }


  override protected def findClass(classname : String) : Class[_] = {
    var clazz : Array[Byte] = null
    val cls = pool get classname
    if(cls != null) {
      cls.detach()
      clazz = cls.toBytecode()
      /*if(manager.config.debug_clazz_bytecode != null) {
        val fl = new File(s"${manager.config.debug_clazz_bytecode}/${classname.replace(".","/")}.class")
        fl.getParentFile.mkdirs()
        val f = new FileOutputStream(fl)
        f.write(clazz)
        f.close()
      }*/
    }
    try {
      val lindx = classname.lastIndexOf(".")
      if (lindx != -1) {
        val pkgname = classname.substring(0, lindx)
        if (getPackage(pkgname) == null)
          definePackage(pkgname, null, null, null, null, null, null, null)
      }
      val dcls = defineClass(classname, clazz, 0, clazz.length, manager.protectionDomain)
      resolveClass(dcls)
      dcls
    } catch {
      case e : IncompatibleClassChangeError => {
        println("=======================failed to redefine the class "+classname)
        null
      }
      case e: Throwable => {
        println("========================o come on\n"+e.toString)
        null
      }
    }
  }


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
