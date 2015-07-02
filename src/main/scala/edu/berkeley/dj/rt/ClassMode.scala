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
 
  def rewriteFieldAccess = distributedCopies
  
  var distributedCopies = false
  
  
}


class ClassModeManager {
  
  private val classes = new mutable.HashMap[String,ClassMode]()
  
  def getMode(name: String) = {
    classes.getOrElseUpdate(name, { new ClassMode(name) })
  }
  
}