package edu.berkeley.dj.rt

/**
 * Created by matthewfl
 */
object Main {
  def main(args : Array[String]) : Unit = {
    if(args.size < 2) {
      println("arguments: [fat jar] [main class] [class arguments...]")
      return
    }

    val config = new Config
    val man = new Manager(config, args(0))
    println("Starting program: "+args(1))
    man.startMain(args(1), new Array[String](0))

  }
}
