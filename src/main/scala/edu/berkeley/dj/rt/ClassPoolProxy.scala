package edu.berkeley.dj.rt

import javassist.CtClass
import javassist.bytecode.Descriptor

import scala.collection.mutable


/**
 * Created by matthewfl
 */
class ClassPoolProxy (private val manager: MasterManager, private val rewriter : Rewriter) extends javassist.ClassPool(false) {

  childFirstLookup = false

  private val cache = new mutable.HashMap[String, CtClass]

  override protected def getCached(classname: String) : CtClass = {
    cache get classname orNull
  }

  override protected def cacheCtClass(classname: String, c: CtClass, dynamic: Boolean) = {
    cache += (classname -> c)
  }

  def setClass(classname: String, c: CtClass) = {
    if(c != null) {
      cache += (classname -> c)
    } else {
      cache.remove(classname)
    }
  }

  override protected def removeCached(classname: String) = {
    cache remove classname orNull
  }

  override protected def createCtClass(classname: String, useCache: Boolean) : CtClass = {
    /*if(classname.startsWith("[") || (classname.contains("/") && classname.contains(";"))) {
      // WTF: there is some bug were we can get a request for a class like: [Ljava/lang/Object;
      return Descriptor.toCtClass(classname, this)
    }*/

    if(!ClassPoolProxy.canRewrite(classname)) {
      return manager.pool.get(classname)
    }
    val res = try {
      rewriter.createCtClass(classname)
    } catch {
      case e: Throwable => {
        println("rewriter failed to create class: " + e)
        null
      }
    }
    if(res != null)
      res
    else {
      System.err.println("We failed: "+classname)
      null
    }

      //super.createCtClass(classname, useCache)
    /*val ret = super.createCtClass(classname, useCache)
    if(ret == null)
      rewriter.createCtClass(classname)
    else
      ret
      */
  }

  /*override def get(classname : String) : CtClass = {
    cache get classname match {
      case Some(c) => return c
      case None => {}
    }
    val cc = createCtClass(classname, false)
    if(cc != null) {
      cache += (classname -> cc)
      return cc
    }
    throw new ClassNotFoundException(classname)
  }*/


}

object ClassPoolProxy {

  def canRewrite(name: String) = {
    !(name.startsWith("java.")
      || name.startsWith("javax.")
      || name.startsWith("sun.")
      || name.startsWith("com.sun.")
      || name.startsWith("org.w3c.")
      || name.startsWith("org.xml."))
  }

}
