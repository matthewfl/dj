//package edu.berkeley.dj.rt
//
//import java.io.{File, FileOutputStream}
//import javassist.{Loader, ClassPool}
//
///**
//  * Created by matthewfl
//  */
//class IOLoaderProxy (private val manager: Manager, private val pool: ClassPool, val debug_prefix: String="/")
//  extends Loader(null, pool) {
//
//  override protected def findClass(classname: String): Class[_] = {
//    val lbd = loadClassByDelegation(classname)
//    if(lbd != null)
//      return lbd
//
//    val clazz: Array[Byte] = getClassBytes(classname)
//    try {
//      val lindx = classname.lastIndexOf(".")
//      if (lindx != -1) {
//        val pkgname = classname.substring(0, lindx)
//        if (getPackage(pkgname) == null)
//          definePackage(pkgname, null, null, null, null, null, null, null)
//      }
//      val dcls = defineClass(classname, clazz, 0, clazz.length, manager.protectionDomain)
//      resolveClass(dcls)
//      //definedClasses.synchronized { definedClasses += classname }
//      dcls
//    } catch {
//      case e: IncompatibleClassChangeError => {
//        System.err.println("=======================failed to redefine the class " + classname)
//        null
//      }
//      case e: Throwable => {
//        System.err.println("========================o come on\n" + e.toString)
//        throw e
//        null
//      }
//    }
//  }
//
//  private val classBytecodes = new scala.collection.mutable.HashMap[String,Array[Byte]]()
//
//  def getClassBytes(classname: String): Array[Byte] = {
//    classBytecodes.synchronized {
//      classBytecodes.getOrElseUpdate(classname, {
//        val cls = pool get classname
//        if (cls != null) {
//          cls.detach()
//          val clazz = cls.toBytecode()
//          if (manager.config.debug_clazz_bytecode != null) {
//            val fl = new File(s"${manager.config.debug_clazz_bytecode}${debug_prefix}${classname.replace(".", "/")}.class")
//            fl.getParentFile.mkdirs()
//            val f = new FileOutputStream(fl)
//            f.write(clazz)
//            f.close()
//          }
//          clazz
//        } else null
//      })
//    }
//    //manager.asInstanceOf[MasterManager].ioRewriter.createCtClass(classname).toBytecode
//  }
//}
