package edu.berkeley.dj.rt

import javassist.CtClass

import scala.collection.mutable


/**
 * Created by matthewfl
 */
class ClassPoolProxy (private val manager : Manager, private val rewriter : Rewriter) extends javassist.ClassPool(false) {

  childFirstLookup = false

  private val cache = new mutable.HashMap[String, CtClass]

  override protected def getCached(classname : String) : CtClass = {
    cache get classname orNull
  }

  override protected def cacheCtClass(classname : String, c : CtClass, dynamic : Boolean) = {
    cache += (classname -> c)
  }

  override protected def removeCached(classname : String) = {
    cache remove classname orNull
  }

  override protected def createCtClass(classname : String, useCache : Boolean) : CtClass = {
    if(!canRewrite(classname)) {
      return manager.pool.get(classname)
    }
    val res = rewriter.createCtClass(classname)
    if(res != null)
      res
    else {
      println("we failed")
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

  def canRewrite(classname : String) = {
    classname != "java.lang.Object"
  }

}
