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
    val cls = loader.loadClass("edu.berkeley.dj.internal.PreMain")
    val ri = new RunningInterface(config)
    cls.getDeclaredMethods.foreach(m => {
      // HACK: some complication with using getDeclaredMethod from scala
      if(m.getName == "premain") {
        m.invoke(null, ri.asInstanceOf[java.lang.Object], mainClass, args)
      }
    })
    //cls.getDeclaredMethod("premain", classOf[java.lang.Object], classOf[String], classOf[Array[String]).invoke(null, ri, mainClass, args)
    //loader.run(mainClass, args)
  }
}
