package testcase

import java.util

import scala.collection.mutable

/**
 * Created by matthewfl
 */

case class Struct(val a: Int, val b: Int)

object SimpleMapthing {

  def main(args: Array[String]) = {

    Thread.sleep(120000);

    val map = new util.TreeMap[Int, Struct]()

    for(i <- 0 until 10000) {
      map.put(i, new Struct(i * 2, i*3))
    }

    println("the simple map thing has finished")

    Thread.sleep(120000);

  }

}
