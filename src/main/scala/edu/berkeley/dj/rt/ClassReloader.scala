package edu.berkeley.dj.rt

import java.lang.management.ManagementFactory
import java.util

import com.sun.jdi.event.{MethodEntryEvent, Event, EventIterator, EventSet}
import com.sun.jdi.request.EventRequest
import com.sun.jdi.{ReferenceType, StringReference, Bootstrap}
import com.sun.jdi.connect.AttachingConnector

import scala.collection.JavaConversions._

/**
 * Created by matthewfl
 */
object ClassReloader {

  lazy val enabled = {
    val en = System.getProperty("dj.classreload", "")
    if(en == "disabled" || en == "false") {
      false
    } else {
      try {
        port
        virtualMachine
        true
      } catch {
        case e: RuntimeException => false
      }
    }
  }

  lazy val (host, port) = {
    val p = System.getProperty("dj.jdwpport")
    if(p != null) {
      ("127.0.0.1", p)
    } else {
      val args = ManagementFactory.getRuntimeMXBean.getInputArguments.filter(s => s.contains("jdwp") && s.contains("address="))
      if(args.length == 0)
        throw new RuntimeException("unable to locate port to connect debugger to for reloading classes")
      val rg = "address=([0-9.:a-fA-F]*):([0-9]+)".r
      val mt = rg.findFirstMatchIn(args(0)).get
      (mt.group(1), mt.group(2))
    }
  }

  lazy val pid = ManagementFactory.getRuntimeMXBean.getName.split("@")(0)

  lazy val virtualMachine = {
    val vmm = Bootstrap.virtualMachineManager().allConnectors()
      .filter(_.name() == "com.sun.jdi.SocketAttach")(0).asInstanceOf[AttachingConnector]
    val args = vmm.defaultArguments()
    args.get("port").setValue(port)
    args.get("hostname").setValue(host)
    vmm.attach(args)
  }

  def reloadClass(loader: LoaderProxy, name: String, bytes: Array[Byte]): Unit = {
    println(s"performing reload: $name")
    val possible_classes = virtualMachine.classesByName(name)
    val clss = possible_classes.filter(c => {
      val cl = c.classLoader()
      val f = cl.referenceType().fieldByName("loaderUUID")
      if(f != null) {
        val fv = cl.getValue(f).asInstanceOf[StringReference]
        fv.value == loader.loaderUUID
      } else false
    })
    if(clss.length != 1)
      throw new RuntimeException(s"Unable to find class $name to replace")
    val rmap = new util.HashMap[ReferenceType, Array[Byte]]()
    rmap.put(clss(0), bytes)
    newClassFile = rmap
    virtualMachine.redefineClasses(rmap)
    println(s"reloaded class: $name")
  }

  // based off javassist's hotswapper

  private class Trigger { def doSwap() = {} }

  private val trigger = new Trigger

  private var newClassFile: util.Map[ReferenceType, Array[Byte]] = null

  private lazy val request = {
    val mereq = virtualMachine.eventRequestManager().createMethodEntryRequest()
    mereq.addClassFilter(classOf[Trigger].getName)
    mereq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD)
    mereq
  }

  private def doReload(): Unit = {
    startDaemon
    assert(newClassFile != null)
    request.enable
    trigger.doSwap()
    request.disable
    if(newClassFile != null) {
      newClassFile = null
      throw new RuntimeException("Failed to reload class")
    }
  }

  private def startDaemon {
    new Thread() {
      private def errorMsg(e: Throwable) {
        System.err.print("Exception in thread \"HotSwap\" ")
        e.printStackTrace(System.err)
      }

      override def run {
        var events: EventSet = null
        try {
          events = waitEvent
          val iter: EventIterator = events.eventIterator
          var done = false
          while (!done && iter.hasNext) {
            val event: Event = iter.nextEvent
            if (event.isInstanceOf[MethodEntryEvent]) {
              hotswap
              done = true
            }
          }
        }
        catch {
          case e: Throwable => {
            errorMsg(e)
          }
        }
        try {
          if (events != null) events.resume
        }
        catch {
          case e: Throwable => {
            errorMsg(e)
          }
        }
      }
    }.start
  }

  def waitEvent = {
    virtualMachine.eventQueue().remove()
  }

  def hotswap: Unit = {
    virtualMachine.redefineClasses(newClassFile)
    newClassFile = null
  }

}
