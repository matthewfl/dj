package edu.berkeley.dj.rt

import javassist.CtClass

import scala.collection.mutable


/**
 * Created by matthewfl
 */
class ClassPoolProxy (private val rewriter : Rewriter) extends javassist.ClassPool {

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

  override protected def createCtClass(classname : String, useCache : Boolean) = {
    val ret = super.createCtClass(classname, useCache)
    if(ret == null)
      rewriter.createCtClass(classname)
    else
      ret
  }

}
