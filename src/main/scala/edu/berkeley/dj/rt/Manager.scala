package edu.berkeley.dj.rt

import java.lang.reflect.InvocationTargetException
import java.security.ProtectionDomain
import javassist._
import javassist.bytecode.MethodInfo

/**
 * Created by matthewfl
 */

private[rt] trait Manager {
  def config: Config

  //def runningPool : javassist.ClassPool

  def protectionDomain: ProtectionDomain

  // TODO:? remove this
  def classRename(name: String): String
}

/**
 * Manager for the master machine
 *
 */
private[rt] class MasterManager (val config: Config, classpaths: String) extends Manager {

  val pool = new ClassPool(true)

  // TODO: comment to allow items that are not built into this jar
  //classpaths.split(":").foreach(pool.appendClassPath(_))

  pool.appendClassPath(new ClassClassPath(this.getClass))
  //pool.childFirstLookup = true

  //val securityManger = new SecurityManager(this)

  val rewriter = new Rewriter(this)

  override def classRename(name: String): String = {
    rewriter.jclassmap.get(name).asInstanceOf[String]
  }

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
    val ri = new RunningInterface(config, this)
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

/**
 * Manager for a client machine
 *
 * Will essentially proxy requests for classes to the main machine
 */
private [rt] class ClientManager (val config: Config) extends Manager {

  override def classRename(name: String): String = {
    throw new NotImplementedError()
  }

  val loader = new RemoteLoaderProxy(this, ClassPool.getDefault)

  val protectionDomain = new ProtectionDomain(null, null, loader, null)
  loader.setDomain(protectionDomain)

  def startClient = {
    val cls = loader.loadClass("edu.berkeley.dj.internal.ClientMain")
    val ri = new RunningInterface(config, this)
  }
}