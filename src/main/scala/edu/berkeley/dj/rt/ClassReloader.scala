package edu.berkeley.dj.rt

import java.lang.management.ManagementFactory

import com.sun.jdi.Bootstrap
import com.sun.jdi.connect.AttachingConnector

import scala.collection.JavaConversions._

/**
 * Created by matthewfl
 */
object ClassReloader {

  lazy val port = System.getProperty("dj.jdwpport")

  lazy val pid = ManagementFactory.getRuntimeMXBean.getName.split("@")(0)

  lazy val virtualMachine = {
    val vmm = Bootstrap.virtualMachineManager().allConnectors().filter(_.name() == "com.sun.jdi.ProcessAttach")(0).asInstanceOf[AttachingConnector]
    val args = vmm.defaultArguments()
    args.get("pid").setValue(pid)
    vmm.attach(args)
  }

  def reloadClass(loader: ClassLoader, name: String, bytes: Array[Byte]): Unit = {
    val possible_classes = virtualMachine.classesByName(name)

  }








}
