package edu.berkeley.dj.rt

import javassist.{Modifier, CtClass}

import edu.berkeley.dj.internal.DJIOException

/**
  * Created by matthewfl
  */
private[rt] class IORewriter (private val manager: MasterManager) extends RewriterInterface {


  def basePool = manager.pool

  def runningPool = manager.ioRunningPool

  def config = manager.config

//  val proxySuffix = "_dj_io_proxy"

  private def reassociateClass(cls: CtClass) = {
    if(cls.getClassPool != runningPool) {
      // prime the cache of the class before we move it to a new pool
      cls.getClassFile
      cls.setClassPool2(runningPool)
      // cache this class before we have fully rewritten it in hopes that we don't loop
      runningPool.setClass(cls.getName, cls)
    }
  }

  private lazy val Object = runningPool.get("java.lang.Object")

  private def createProxyCls(classname: String): CtClass = {
    val orgClass = runningPool.get(classname.dropRight(config.ioProxySuffix.length))
    val cls = runningPool.makeClass(classname, orgClass)
    var wcls = cls
    while(wcls != Object) {
      for(field <- wcls.getDeclaredFields) {
        if(Modifier.isPublic(field.getModifiers))
          throw new DJIOException("can not have a public field on a proxied class")
      }
      for(mth <- wcls.getDeclaredMethods) {
        val mods = mth.getModifiers
        if(Modifier.isPublic(mods)) {
          if(Modifier.isFinal(mods))
            throw new DJIOException("Can not have final on one of the public methods of a proxied class")



        }
      }

      wcls = wcls.getSuperclass
    }

    cls
  }

  override def createCtClass(classname: String, addToCache: CtClass => Unit): CtClass = {
    if(classname.endsWith(config.ioProxySuffix)) {
      // TODO: make this create a class that can proxy into the distribuited application
      return createProxyCls(classname)
    }
    val cls = basePool.get(classname)
    reassociateClass(cls)
    cls
    //null
  }

}
