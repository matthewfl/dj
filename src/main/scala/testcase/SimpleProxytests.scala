package testcase

import java.io.{File, PrintWriter}

/**
 * Created by matthewfl
 */
object SimpleProxytests {

  def main(args: Array[String]) = {
    new File("/home/matthew").listFiles().foreach(f => {
      System.out.println("fname: "+f.getName)
    })

    val op = new PrintWriter(new File("/tmp/test123"))
    op.write("this is some test content")
    op.close()
  }

}