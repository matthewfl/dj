package edu.berkeley.dj.rt

import javassist.CtClass

/**
  * Created by matthewfl
  */
private[rt] class IORewriter (private val manager: MasterManager) extends RewriterInterface {


  def basePool = manager.pool

  def runningPool = manager.ioRunningPool

  private def reassociateClass(cls: CtClass) = {
    if(cls.getClassPool != runningPool) {
      // prime the cache of the class before we move it to a new pool
      cls.getClassFile
      cls.setClassPool2(runningPool)
      // cache this class before we have fully rewritten it in hopes that we don't loop
      runningPool.setClass(cls.getName, cls)
    }
  }

  override def createCtClass(classname: String, addToCache: CtClass => Unit): CtClass = {
    if(classname.endsWith("_dj_io_proxy")) {
      // TODO: make this create a class that can proxy into the distribuited application
      ???
    }
    val cls = basePool.get(classname)
    reassociateClass(cls)
    cls
    //null
  }

}
