package edu.berkeley.dj.rt

import java.lang.reflect.InvocationTargetException
import java.security.ProtectionDomain
import javassist._
import javassist.bytecode.MethodInfo

import edu.berkeley.dj.rt.network.{NetworkManager, NetworkCommunication}
import edu.berkeley.dj.utils.Memo
import edu.berkeley.dj.utils.Memo.==>

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by matthewfl
 */

private[rt] trait Manager {
  def config: Config

  //def runningPool : javassist.ClassPool

  def protectionDomain: ProtectionDomain

  // TODO:? remove this
  def classRename(name: String): String

  def start: Unit = ???

  private[rt] var networkInterface: NetworkCommunication = null

  private[rt] var runningInterface: RunningInterface = null

  def isMaster = false


}

/**
 * Manager for the master machine
 *
 */
private[rt] class MasterManager (val config: Config, classpaths: String) extends Manager {

  override def isMaster = true

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
    val nmanager = new NetworkManager(config.cluster_code, config.cluster_conn_mode)
    val cls = loader.loadClass("edu.berkeley.dj.internal.PreMain")
    runningInterface = new RunningInterface(config, this)
    networkInterface = nmanager.getApplication(config.uuid, true, new NetworkCommInterface(this))
    // HACK: some complication with using getDeclaredMethod from scala
    val premain = cls.getDeclaredMethods.filter(_.getName == "premain")(0)
    try {
      premain.invoke(null, runningInterface.asInstanceOf[java.lang.Object], mainClass, args)
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

  private lazy val innerClassRename: String ==> Boolean = Memo { case name: String =>
    // send a request to the master node asking about this class name
    // wait at most 60 seconds
    // if we get a non zero value for the first byte, then we must end up renaming this class
    // memorize it so that we can have a faster op in the future
    Await.result(networkInterface.sendWrpl(0, 2, name.getBytes), 60 seconds)(0) != 0
  }

  val loader = new RemoteLoaderProxy(this, ClassPool.getDefault)

  val protectionDomain = new ProtectionDomain(null, null, loader, null)
  loader.setDomain(protectionDomain)

  def startClient = {
    val cls = loader.loadClass("edu.berkeley.dj.internal.ClientMain")
    runningInterface = new RunningInterface(config, this)
    val start = cls.getDeclaredMethods.filter(_.getName == "prestart")(0)
    try {
      start.invoke(null, runningInterface.asInstanceOf[java.lang.Object])
    } catch {
      case e: InvocationTargetException => {
        println("some internal error on client " + e.getTargetException)
        throw e
      }
    }
  }

  override def start = startClient
}