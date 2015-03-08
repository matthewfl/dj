package edu.berkeley.dj.rt

import java.lang.reflect.InvocationTargetException
import java.security.ProtectionDomain
import javassist._

/**
 * Created by matthewfl
 */
private[rt] class Manager (val config : Config, mainJar : String) {

  val pool = new ClassPool(true)
  pool.appendClassPath(mainJar)
  //pool.childFirstLookup = true

  val securityManger = new SecurityManager(this)

  val rewriter = new Rewriter(this)

  val runningPool = new ClassPoolProxy(this, rewriter)

  val loader = new LoaderProxy(this, runningPool)
  //val loader = new Loader(runningPool)

  val protectionDomain = new ProtectionDomain(null, null, loader, null)
  loader.setDomain(protectionDomain)

  def startMain (mainClass : String, args : Array[String]) = {

    val cls = loader.loadClass("edu.berkeley.dj.internal.PreMain")
    val ri = new RunningInterface(config)
    // HACK: some complication with using getDeclaredMethod from scala
    val premain = cls.getDeclaredMethods.filter(_.getName == "premain")(0)
    try {
      premain.invoke(null, ri.asInstanceOf[java.lang.Object], mainClass, args)
    } catch {
      case e : InvocationTargetException => { throw e.getTargetException }
    }
  }
}
