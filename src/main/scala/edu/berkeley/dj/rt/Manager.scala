package edu.berkeley.dj.rt

import java.lang.reflect.InvocationTargetException
import java.security.ProtectionDomain
import javassist._
import javassist.bytecode.MethodInfo

/**
 * Created by matthewfl
 */
private[rt] class Manager (val config: Config, classpaths: String) {

  val pool = new ClassPool(true)

  classpaths.split(":").foreach(pool.appendClassPath(_))
  //pool.appendClassPath(new ClassClassPath(this.getClass))
  pool.childFirstLookup = true

  val securityManger = new SecurityManager(this)

  val rewriter = new Rewriter(this)

  val runningPool = new ClassPoolProxy(this, rewriter)

  val loader = new LoaderProxy(this, runningPool)
  //val loader = new Loader(runningPool)

  val protectionDomain = new ProtectionDomain(null, null, loader, null)
  loader.setDomain(protectionDomain)

  def startMain (mainClass : String, args : Array[String]) = {
    if(config.debug_clazz_bytecode != null) {
      //CtClass.debugDump = config.debug_clazz_bytecode
      MethodInfo.doPreverify = true
    }
    val cls = loader.loadClass("edu.berkeley.dj.internal.PreMain")
    val ri = new RunningInterface(config)
    // HACK: some complication with using getDeclaredMethod from scala
    val premain = cls.getDeclaredMethods.filter(_.getName == "premain")(0)
    try {
      premain.invoke(null, ri.asInstanceOf[java.lang.Object], mainClass, args)
    } catch {
      case e: InvocationTargetException => {
        val en = e.getTargetException.getClass.getName
        if(en == "java.lang.IncompatibleClassChangeError") {
          println("gaaaa")
        }
        throw e
      }
    }
  }
}
