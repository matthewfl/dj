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
      val mname = c match {
        case MONITORENTER => "monitorEnter"
        case MONITOREXIT => "monitorExit"
      }
      var newId = cp.findMember(mname, "(Ljava/lang/Object;)V", "edu.berkeley.dj.internal.ObjectHelpers")
      if(newId == -1) {
        val nref = cp.addNameAndTypeInfo(mname, "(Ljava/lang/Object;)V")
        val clsref = cp.addClassInfo("edu.berkeley.dj.internal.ObjectHelpers")
        newId = cp.addMethodrefInfo(clsref, nref)
      }
      // first overwrite this action to make sure we don't see it again
      it.writeByte(NOP, pos)
      // this may insert more then 2 spaces, in which case we would not end up overwriting the existing op
      val g = it.insertGapAt(pos, 2, false)
      it.writeByte(INVOKESTATIC, pos)
      it.write16bit(newId, pos + 1)
    }
    pos
  }

}
