package examples.simpleMR

/**
 * Created by matthewfl
 *
 * This is an example user of the MR framework.
 */
object ExampleMR {

  def main(args: Array[String]) = {

    println("mr starting")
    val res = Pipeline.start((0 until 50).iterator).map[Int]((v, emit) => {
      val k = s"${v % 3}"
      emit(k, v)
    }).reduce[Int]((k, vs, emit) => {
      emit(k, vs.length)
    }).get("0")(0)


    println(res)
  }

}
