package examples.simpleMR

import edu.berkeley.dj.internal.{JITCommands, InternalInterface, ThreadHelpers}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by matthewfl
 */
sealed trait Pipeline[T] {

  def map[U](func: (T, (String, U) => Unit) => Unit): Pipeline[U] = ???

  def reduce[U](func: (String, Seq[T], (String, U) => Unit) => Unit): Pipeline[U] = ???

  def get(id: String): Seq[T] = ???

  def _recv(k: String, v: T): Unit= ???
}


class InputPipe[T] (val data: Iterator[T]) extends Pipeline[T] {

  private var scheduled: Int = 0
  private var complete: Int = 0


  override def map[U](func: (T, (String, U) => Unit) => Unit): Pipeline[U] = {
    new MappedPipe[U](this, (pipe: MappedPipe[U]) => {
      for (d <- data) {
        scheduled += 1
        ThreadHelpers.runTaskCluster(new Runnable {
          override def run(): Unit = {
            func(d, (k: String, v: U) => pipe._recv(k,v))
            InputPipe.this.synchronized{
              complete += 1
              if(complete == scheduled)
                InputPipe.this.notifyAll()
            }
          }
        })
        // block until everything is done
      }
      this.synchronized {
        this.wait()
      }
    })
  }

}

class MappedPipe[T] (val previous: Pipeline[_], private val prev_run: (MappedPipe[T]) => Unit) extends Pipeline[T] {

  private val data = new mutable.HashMap[String, ListBuffer[T]]()

  override def _recv(k: String, v: T) : Unit = {
    // TODO: may race here
    data.getOrElseUpdate(k, new ListBuffer[T] ) += v
  }

  private var scheduled: Int = 0
  private var complete: Int = 0
  private var previousRun = false

  override def reduce[U](func: (String, Seq[T], (String, U) => Unit) => Unit): Pipeline[U] = {
    new ReducePipe[U](this, (pipe: ReducePipe[U]) => {
      if(!previousRun) {
        prev_run(this)
        previousRun = true
      }
      val hosts = InternalInterface.getInternalInterface.getAllHosts
      for(d <- data) {
        scheduled += 1
        val t = hosts(d._1.hashCode % hosts.length) // target machine
        // relocate the objects
        for(i <- d._2) {
          JITCommands.moveObject(i, t)
        }
        JITCommands.moveObject(d._2, t)
        ThreadHelpers.runTaskCluster(new Runnable {
          override def run(): Unit = {
            func(d._1, d._2, (k: String, v: U) => pipe._recv(k, v))
            MappedPipe.this.synchronized {
              complete += 1
              if(complete == scheduled)
                MappedPipe.this.notifyAll()
            }
          }
        })
      }
      // wait for all steps to finish
      this.synchronized {
        this.wait()
      }
    })

  }

}

class ReducePipe[T] (val previous: Pipeline[_], private val prev_run: (ReducePipe[T]) => Unit) extends Pipeline[T] {

  private val data = new mutable.HashMap[String, ListBuffer[T]]()
  private var previousRun = false

  override def _recv(k: String, v: T): Unit = {
    data.getOrElseUpdate(k, new ListBuffer[T]) += v
  }

  override def get(id: String): Seq[T] = {
    if(!previousRun) {
      prev_run(this)
      previousRun = true
    }
    data.getOrElse(id, ListBuffer.empty[T])
  }

}



object Pipeline {

  def start[T](d: Iterator[T]): Pipeline[T] = new InputPipe[T](d)
}
