package edu.berkeley.dj.rt.convert

import javassist.CtClass
import javassist.bytecode.{ConstPool, CodeIterator}
import javassist.convert.Transformer
import javassist.bytecode.Opcode._

/**
 * Created by matthewfl
 */
class FieldAccess(next : Transformer) extends Transformer(next) {

  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    val c : Int = it.byteAt(pos)
    if(c == GETFIELD || c == GETSTATIC || c == PUTFIELD || c == PUTSTATIC) {
      // there needs to be some difference between static fields and object

      // use it.insertGap to make space for extra instructions

      // it appears that the limitations origionally imposed by javassist in terms of rewriting
      // field access are only because that is what is hard coded in
    }
    pos
  }

}
