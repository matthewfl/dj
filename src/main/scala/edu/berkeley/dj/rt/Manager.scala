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

sealed private[rt] trait Manager {

  def config: Config

  //def runningPool : javassist.ClassPool

  def protectionDomain: ProtectionDomain

  // TODO:? remove this
  def classRename(name: String): String

  def start: Unit = ???

  private[rt] var networkInterface: NetworkCommunication = null

  private[rt] var runningInterface: RunningInterface = null

  def isMaster = false

  def loader: LoaderProxy

  def ioLoader: LoaderProxy

  private[rt] val threadPool = new ThreadPool

  private[rt] val io = new IOManager(this)

}

/**
 * Manager for the master machine
 *
 */
private[rt] class MasterManager (val config: Config, classpaths: String) extends Manager {

  override def isMaster = true

  val classMode = new ClassModeManager

  val pool = new ClassPool(true)

  // TODO: comment to allow items that are not built into this jar
  //classpaths.split(":").foreach(pool.appendClassPath(_))

  pool.appendClassPath(new ClassClassPath(this.getClass))
  //pool.childFirstLookup = true

  //val securityManger = new SecurityManager(this)

  val rewriter = new Rewriter(this)

  val ioRewriter = new IORewriter(this)

  override def classRename(name: String): String = {
    rewriter.jclassmap.get(name).asInstanceOf[String]
  }

  val runningPool = new ClassPoolProxy(this, rewriter)

  val loader = new LoaderProxy(this, runningPool)
  //val loader = new Loader(runningPool)

  val ioRunningPool = new ClassPoolProxy(this, ioRewriter)

  val ioLoader = new LoaderProxy(this, ioRunningPool, debug_prefix = "-io/")
  //io.loadInterface(ioLoader)

  val protectionDomain = new ProtectionDomain(null, null, loader, null)
  loader.setDomain(protectionDomain)

  def reloadClass(name: String): Unit = {
    if(ClassReloader.enabled) {
      loader.reloadClass(name)
      // TODO: should cache the bytes for the class here
      networkInterface.sendAll(110, name.getBytes())
    }
  }

  private var nmanager: NetworkManager = null

  def startMain (mainClass : String, args : Array[String]) = {
    if(config.debug_clazz_bytecode != null) {
      //CtClass.debugDump = config.debug_clazz_bytecode
      MethodInfo.doPreverify = true
    }
    nmanager = new NetworkManager(config.cluster_code, config.cluster_conn_mode)
    val cls = loader.loadClass("edu.berkeley.dj.internal.PreMain")
    runningInterface = new RunningInterface(config, this)
    networkInterface = nmanager.getApplication(config.uuid, true, new NetworkCommInterface(this))

    // HACK: some complication with using getDeclaredMethod from scala
    val premain = cls.getDeclaredMethods.filter(_.getName == "premain")(0)
    try {
      premain.invoke(null, runningInterface.asInstanceOf[java.lang.Object], config.distributed_jit, mainClass, args)
    } catch {
      case e: InvocationTargetException => {
        val en = e.getTargetException.getClass.getName
        if(en == "java.lang.IncompatibleClassChangeError") {
          println("gaaaa")
        }
        e.getTargetException.printStackTrace()
        throw e
      }
    }
  }

  private[rt] def startNetwork () = {
    // this will create instances on other machines
    nmanager.createNewApp(config.uuid)
  }
}

/**
 * Manager for a client machine
 *
 * Will essentially proxy requests for classes to the main machine
 */
private[rt] class ClientManager (val config: Config) extends Manager {

  override def classRename(name: String): String = innerClassRename(name)

  private lazy val innerClassRename: String ==> String = Memo { case name: String =>
    // send a request to the master node asking about this class name
    // wait at most 60 seconds
    // if we get a non zero value for the first byte, then we must end up renaming this class
    // memorize it so that we can have a faster op in the future
    val res = Await.result(networkInterface.sendWrpl(0, 2, name.getBytes), 60 seconds)
    if(res.length == 0)
      null
    else
      new String(res)
  }

  val loader = new RemoteLoaderProxy(this, ClassPool.getDefault)

  lazy val ioLoader: LoaderProxy = {
    new RemoteLoaderProxy(this, ClassPool.getDefault, ioLoader=true)
  }
  //io.loadInterface(ioLoader)

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
        e.printStackTrace()
        throw e
      }
    }
  }

  override def start = startClient
}