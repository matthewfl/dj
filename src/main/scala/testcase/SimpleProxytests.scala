package testcase

import java.io.File

/**
 * Created by matthewfl
 */
object SimpleProxytests {

  def main(args: Array[String]) = {
    new File("/home/matthew").listFiles().foreach(f => {
      System.out.println("fname: "+f.getName)
    })
  }

}