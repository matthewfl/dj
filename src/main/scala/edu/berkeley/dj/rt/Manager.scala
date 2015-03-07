package edu.berkeley.dj.rt

import javassist._

/**
 * Created by matthewfl
 */
class Manager (config : Config, mainJar : String) {
  private val pool = new ClassPool(true)
  pool.appendClassPath(mainJar)




  def startMain (mainClass : String, args : Array[String]) = {
    val rewriter = new Rewriter(config, pool)
    val runningPool = new ClassPoolProxy(rewriter)
    val loader = new LoaderProxy(runningPool)
    loader.run(mainClass, args)
  }
}
