package edu.berkeley.dj.rt

import java.util.jar.JarFile

import scala.collection.mutable

/**
 * Created by matthewfl
 */
object RealMain {

  var arguments = mutable.Map[String,String](
    "fjar" -> "",
    "cp"-> "",
    "maincls" -> "",
    "debug_clazz_bytecode" -> null
  )

  def help = {
    println("arguments: -fjar [fat jar] -maincls [[main class]] [class arguments...]")
  }

  def main(args : Array[String]) : Unit = {
    if(args.size < 2) {
      help
      return
    }

    var argsp = List[String]()
    var i = 0
    var done_parsing_args = false
    while(i < args.size) {
      if(args(i) == "--") {
        done_parsing_args = true
        i += 1
      } else if(!done_parsing_args && args(i).startsWith("-") && arguments.contains(args(i).drop(1))) {
        arguments.update(args(i).drop(1), args(i+1))
        i += 2
      } else {
        argsp ++= List(args(i))
        i += 1
      }
    }

    val fjar = arguments("fjar")
    var clsp = arguments("cp")
    if(fjar.isEmpty && clsp.isEmpty) {
      help
      return
    }
    if(!clsp.contains(fjar)) {
      clsp = if (clsp.isEmpty)
        fjar
      else
        clsp + ":"+ fjar
    }

    var maincls = arguments("maincls")
    if(maincls.isEmpty) {
      if(fjar.isEmpty) {
        help
        return
      }
      var jfile = new JarFile(fjar)
      maincls = jfile.getManifest.getMainAttributes.getValue("Main-Class")
      if(maincls == null) {
        help
        return
      }
    }

    val config = new Config(debug_clazz_bytecode=arguments("debug_clazz_bytecode"))

    val man = new Manager(config, clsp)
    println("Starting program: "+maincls)
    man.startMain(maincls, argsp.toArray)

  }
}
