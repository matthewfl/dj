package edu.berkeley.dj.rt

//import javassist.tools.reflect.{Loader,Metaobject}
//import javassist.{ClassPool,Translator}
import javassist._


/**
 * Created by matthewfl
 */

class LoaderTranslator extends Translator {
  override def start(pool : ClassPool) = {}

  override def onLoad(pool : ClassPool, classname : String) = {
    if(classname == "edu.berkeley.dj.internal.InternalInterfaceFactory") {
      println("ggg");
    }
  }
}

/*class LoaderRunningMeta extends Metaobject {
  println("constructed")
}*/

class LoaderProxy(pool : ClassPoolProxy) extends Loader(pool) {

  addTranslator(pool, new LoaderTranslator)


  // TODO: make this check that the class that is getting loaded is some basic class
  // eg java.lang, otherwise, when a class is not found, then it can start looking into
  // the upper level class path code, which contains this

  //override protected def delegateToParent(classname : String) = throw new ClassNotFoundException(classname)
  override  protected def delegateToParent(classname : String) = {
    println("loading from parent class: "+classname)
    super.delegateToParent(classname)
  }

}
