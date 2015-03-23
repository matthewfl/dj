package edu.berkeley.dj.rt

/**
 * Created by matthewfl
 */
class SecurityManager (private val manager : Manager) extends java.lang.SecurityManager {

  override def checkLink(lib : String) = {
    // need to change this to check that this
    // is not using some nested class loader from the sanbox..

    val cl = currentClassLoader()
    if(cl != null && cl.isInstanceOf[LoaderProxy])
      throw new SecurityException()
  }
}
