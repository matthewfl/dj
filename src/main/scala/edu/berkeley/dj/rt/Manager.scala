package edu.berkeley.dj.rt

import javassist._

/**
 * Created by matthewfl
 */
class Manager (config : Config, mainJar : String) {
  private val pool = new ClassPool
  pool.appendClassPath(mainJar)

  def startMain (mainClass : String, args : Array[String]) = {
    val loader = new Loader(pool)
    loader.run(mainClass, args)
  }
}
