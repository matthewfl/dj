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

  val redirectedMethods = new mutable.HashSet[String]()

}


class ClassModeManager {
  
  private val classes = new mutable.HashMap[String,ClassMode]()
  
  def getMode(name: String) = {
    classes.getOrElseUpdate(name, { new ClassMode(name) })
  }
  
}