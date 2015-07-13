package edu.berkeley.dj.rt

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

import edu.berkeley.dj.rt.network.NetworkManager

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by matthewfl
 */
object RealMain {

  var arguments = mutable.Map[String,String](
    "fjar" -> "",
    "cp"-> "",
    "maincls" -> "",
    "debug_clazz_bytecode" -> null,
    "cluster_code" -> "dummy", // the code code that the nodes use to identify to eachother
    "cluster_conn" -> "dummy", // will start up two processes in the same jvm
    "mode" -> "master"
  )

  def help = {
    println(
      """
        |To start a processing node: -cluster_code 'cluster-code-key-board-cat' -mode client
        |To start master: -cluster_code -fjar [fat jar] -maincls [main class]
      """.stripMargin)
    println("arguments: -fjar [fat jar] -maincls [[main class]] [class arguments...]")
  }

  // this hack allows us to access the java tools without having to have some wrapper script
  // for setting the class path
  def addJavaToolsToPath: Boolean = {
    try {
      // TODO: maybe use another class from this package so that we don't cause there to be not found errors later?
      Class.forName("com.sun.jdi.VirtualMachine")
      return true
    } catch {
      case e: ClassNotFoundException => {}
    }
    // Find the java tools.jar file
    val f = new File(System.getProperty("java.home").dropRight(3)+"lib/tools.jar")
    if(!f.exists()) {
      System.err.println("tools.jar not found, please make sure that the jdk (not just the jre) is installed")
      return false
    }
    try {
      val cl = ClassLoader.getSystemClassLoader.asInstanceOf[URLClassLoader]
      val mth = classOf[URLClassLoader].getDeclaredMethods().filter(_.getName == "addURL")(0)
      mth.setAccessible(true)
      mth.invoke(cl, f.toURI.toURL)
      return true
    } catch {
      case e: Throwable => {
        System.err.println("Unable to automatically add java tools.jar to classpath\nplease manually add tools.jar to the classpath\n"+e)
        //throw e
        return false
      }
    }
  }

  def main(args: Array[String]) : Unit = {
    if (args.size < 2) {
      help
      return
    }

    var argsp = List[String]()
    var i = 0
    var done_parsing_args = false

    while (i < args.size) {
      if (args(i) == "--") {
        done_parsing_args = true
        i += 1
      } else if (!done_parsing_args && args(i).startsWith("-") && arguments.contains(args(i).drop(1))) {
        arguments.update(args(i).drop(1), args(i + 1))
        i += 2
      } else {
        argsp ++= List(args(i))
        i += 1
      }
    }

    System.setSecurityManager(new SecurityManager)

    val canAccessJDI = addJavaToolsToPath

    arguments("mode") match {
      case "master" => {
        val fjar = arguments("fjar")
        var clsp = arguments("cp")
        if (fjar.isEmpty && clsp.isEmpty) {
          help
          return
        }
        if (!clsp.contains(fjar)) {
          clsp = if (clsp.isEmpty)
            fjar
          else
            clsp + ":" + fjar
        }

        var maincls = arguments("maincls")
        if (maincls.isEmpty) {
          if (fjar.isEmpty) {
            help
            return
          }
          var jfile = new JarFile(fjar)
          maincls = jfile.getManifest.getMainAttributes.getValue("Main-Class")
          if (maincls == null) {
            help
            return
          }
        }

        if(arguments("cluster_code") != "dummy" && arguments("cluster_conn") == "dummy") {
          arguments("cluster_conn") = "hazelcast"
        }

        val dummy_f = if(arguments("cluster_conn") == "dummy") {
          // start up a dummy connection so we can at least have to different systems talking with eachother
          Future {
            val comm = new NetworkManager(arguments("cluster_code"), "dummy")
            comm.runClient
          }
        } else null

        val config = new Config(
          debug_clazz_bytecode = arguments("debug_clazz_bytecode"),
          cluster_code = arguments("cluster_code"),
          cluster_conn_mode = arguments("cluster_conn")
        )


        val man = new MasterManager(config, clsp)
        println("Starting program: " + maincls)
        man.startMain(maincls, argsp.toArray)


      }
      case "client" => {
        if(arguments("cluster_conn") == "dummy")
          arguments("cluster_conn") = "hazelcast" // it does not make since to make to make a single node with the dummy client
        startClient(arguments("cluster_conn"), arguments("cluster_code"))
      }
      case _ => {
        help
      }
    }
  }

  def startClient(cmode: String, code: String) = {
    val comm = new NetworkManager(code, cmode)
    comm.runClient
  }
}
