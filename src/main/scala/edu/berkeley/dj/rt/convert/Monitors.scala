package edu.berkeley.dj.rt.convert

import javassist.CtClass
import javassist.bytecode.{ConstPool, CodeIterator}
import javassist.convert.Transformer
import javassist.bytecode.Opcode._

/**
 * Created by matthewfl
 */
class Monitors(next: Transformer) extends Transformer(next) {

  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    val c : Int = it.byteAt(pos)
    if(c == MONITORENTER || c == MONITOREXIT) {
      // invoke the __dj_monitor enter or exit method
    }
    pos
  }

}
