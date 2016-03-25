package edu.berkeley.dj.rt

import scala.collection.mutable

/**
 * Created by matthewfl
 * 
 * Manage the "mode" that a class is operating in, eg: if the there are shared copies
 * of the class then we need to intercept the variable access
 * Potentially this may be exposed to the JIT somehow
 */
class ClassMode(val className: String) {
 
  def rewriteFieldAccess = distributedCopies || !ClassReloader.enabled
  
  var distributedCopies = false

  var loaded = false

  def addMethodRedirect(signature: String) = !ClassReloader.enabled || redirectedMethods.contains(signature) || className.contains("makeThisRpc")

  def performRedirect(signature: String) = redirectedMethods.contains(signature)

  def addMethodRedirectCheck(signature: String) = !ClassReloader.enabled

  def redirectBasedOffField(signature: String): Int = if(ClassReloader.enabled) redirectedMethods.getOrElse(signature, -3) else -3

  def setRedirect(signature: String, value: Int = -3) = redirectedMethods.put(signature, value)

  val redirectedMethods = new mutable.HashMap[String,Int]()

}


class ClassModeManager {
  
  private val classes = new mutable.HashMap[String,ClassMode]()
  
  def getMode(name: String) = {
    classes.getOrElseUpdate(name, { new ClassMode(name) })
  }
  
}