package testcase

/**
 * Created by matthewfl
 */
class MapReduceTest {

  def run: Unit = {
    (0 until 50).par.map(v => {
      System.out.println(s"getting the map operation for $v")
      (v % 30, v)
    }).groupBy(_._1).map(vs => {
      System.out.println(s"getting the reduced result with ${vs._1} and length ${vs._2.length}")
    })
  }

}


object MapReduceTest {

  def main(args: Array[String]): Unit = {
    val mr = new MapReduceTest()

    mr.run

  }
}