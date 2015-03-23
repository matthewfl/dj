package edu.berkeley.dj.rt.convert

import javassist.CtClass
import javassist.bytecode.{MethodInfo, ConstPool, CodeIterator}
import javassist.convert.Transformer
import javassist.bytecode.Opcode._

/**
 * Created by matthewfl
 */
class FunctionCalls (next : Transformer, val rewriteMethods : Map[String, String]) extends Transformer(next) {

/*  var replacements = Map[Int, Int]()

  override def initialize(cp: ConstPool, clazz: CtClass, minfo: MethodInfo) = {
    replacements = rewriteMethods.map(m => {
      val from = clazz.getMethods.zipWithIndex.filter(_._1.getName == m._1)
      if(!from.isEmpty) {
        val to = clazz.getMethods.zipWithIndex.filter(_._1.getName == m._2)
        if(!to.isEmpty)
          Map(from(0)._2 -> to(0)._2)
        else
          Map[Int,Int]()
      } else
        Map[Int,Int]()
    }).reduce(_ ++ _)
  }
*/


  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    val c : Int = it.byteAt(pos)
    if(c == INVOKEINTERFACE || c == INVOKESPECIAL || c == INVOKESTATIC || c == INVOKEVIRTUAL) {
      val ind = it.u16bitAt(pos + 1)
      // need to determine what this is getting called on
      cp.getUtf8Info(cp.getMemberNameAndType(ind))


    }
    pos
  }

}
