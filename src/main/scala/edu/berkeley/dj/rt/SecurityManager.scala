package edu.berkeley.dj.rt

import java.security.Permission

/**
 * Created by matthewfl
 */
class SecurityManager extends java.lang.SecurityManager {

  override def checkLink(lib : String) = {
    // need to change this to check that this
    // is not using some nested class loader from the sanbox..

    val cl = currentClassLoader()
    if(cl != null && cl.isInstanceOf[LoaderProxy])
      throw new SecurityException()
  }

  override def checkPackageDefinition(pkg: String) = {
    println("trying to define package: "+pkg)
  }

  override def checkPermission(perm: Permission) = {

  }

}
