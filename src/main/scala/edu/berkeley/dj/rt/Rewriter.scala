package edu.berkeley.dj.rt

import javassist._

/**
 * Created by matthewfl
 */
class Rewriter (private val config : Config, private val basePool : ClassPool) {

  def createCtClass(classname : String) = {
    basePool get classname
  }

}
