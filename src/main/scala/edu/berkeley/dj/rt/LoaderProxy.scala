package edu.berkeley.dj.rt

//import javassist.tools.reflect.{Loader,Metaobject}
//import javassist.{ClassPool,Translator}

import java.lang.reflect.InvocationTargetException
import javassist._


/**
 * Created by matthewfl
 */

class LoaderTranslator extends Translator {
  override def start(pool : ClassPool) = {}

  override def onLoad(pool : ClassPool, classname : String) = {
    println("loading class: "+classname)
  }
}

/*class LoaderRunningMeta extends Metaobject {
  println("constructed")
}*/

class LoaderProxy(private val manager : Manager, private val pool : ClassPoolProxy)
  extends Loader(null, pool) {

  addTranslator(pool, new LoaderTranslator)


  // TODO: make this check that the class that is getting loaded is some basic class
  // eg java.lang, otherwise, when a class is not found, then it can start looking into
  // the upper level class path code, which contains this

  //override protected def delegateToParent(classname : String) = throw new ClassNotFoundException(classname)
  override  protected def delegateToParent(classname : String) = {
    println("loading from parent class: "+classname)
    super.delegateToParent(classname)
  }


  /*override protected def findClass(classname : String) : Class[_] = {
    null
    /*val cls = pool get classname
    val btyecode : Array[Byte] = cls.toBytecode.to
    val pkgname = classname.substring(0, classname.lastIndexOf("."))
    if(getPackage(pkgname) == null)
      definePackage(pkgname, null, null, null, null, null, null, null)
    defineClass(classname, btyecode, 0, bytecode.length, manager.protectionDomain)
*/
    /*try {
      super.findClass(classname)
    } catch {
      case e : Throwable => {
        println("gggg")
        throw e
      }
    }*/

  }*/

  /*override protected def loadClass(classname : String, resolve : Boolean) = {
    try {
      super.loadClass(classname, resolve)
    } catch {
      case e : Throwable => {
        println("qqqq")
        throw e
      }
    }
  }*/

}
