package edu.berkeley.dj.rt

import java.util.concurrent.{ForkJoinTask, Callable, ForkJoinPool}

/**
 * Created by matthewfl
 */
class ThreadPool extends ForkJoinPool(Runtime.getRuntime.availableProcessors() * 3/2) {


  private[this] def pollSubmission2() = super.pollSubmission()

  val numProcessors = Runtime.getRuntime.availableProcessors()

  //val forkjoinpool = new ForkJoinPool(numProcessors * 3 / 2)

  // make sure that there is some thread running otherwise start up new threads
  private val watch_thread = new Thread(new Runnable {
    override def run(): Unit = {
      while (true) {
        Thread.sleep(1000) // 1 sec
        if (ThreadPool.this.getPoolSize - ThreadPool.this.getRunningThreadCount > numProcessors) {
          // at least 2/3 of the threads are blocked on something
          // start the next runner in a new thread to make sure that we don't cap out
          val next = ThreadPool.this.pollSubmission2
          if(next != null) {
            val nt = new Thread(new Runnable {
              override def run(): Unit = {
                try { next.invoke }
                catch { case e: Exception => {
                  println("Running thread had exception:")
                  e.printStackTrace()
                }}
              }
            })
            nt.start()
          }
        }
      }
    }
  })
  watch_thread.setDaemon(true)
  watch_thread.start()

//  def submit(lambda: => Unit) = {
//    submit(new Runnable {
//      override def run(): Unit = lambda()
//    })
//  }

  def submit[T](lambda: => T): ForkJoinTask[T] = {
    submit[T](new Callable[T] {
      override def call(): T = lambda
    })
  }


}
