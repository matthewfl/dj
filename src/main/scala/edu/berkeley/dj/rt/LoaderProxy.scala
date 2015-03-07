package edu.berkeley.dj.rt

import javassist._

/**
 * Created by matthewfl
 */
class LoaderProxy(pool : ClassPoolProxy) extends Loader(pool) {

  // TODO: make this check that the class that is getting loaded is some basic class
  // eg java.lang, otherwise, when a class is not found, then it can start looking into
  // the upper level class path code, which contains this

  //override protected def delegateToParent(classname : String) = throw new ClassNotFoundException(classname)

}
