package edu.berkeley.dj.rt.convert

import javassist.CtClass
import javassist.bytecode.{CodeIterator, ConstPool}
import javassist.bytecode.Opcode._
import javassist.convert.Transformer

/**
 * Created by matthewfl
 */
class SpecialConverter (next : Transformer) extends Transformer(next) {

  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    val c: Int = it.byteAt(pos)

    // java.lang.String has a constructor ([CZ) that is package private, but we can't use it
    // so pop the boolean value and then call the ([C) constructor
    // ??? maybe make a modified version of the string class....but then we are going to end up converting back and forth between
    if(c == INVOKESPECIAL) {
      val ind = it.u16bitAt(pos + 1)
      val (memberName, typeSig, className) = cp.getMemberName(ind).split("::") match {
        case Array(a,b,c) => (a,b,c)
      }
      if(className == "java.lang.String" && memberName == "<init>" && typeSig == "([CZ)V") {
        // we need to replace this method
        it.insertGap(pos, 1)
        it.writeByte(POP, pos)
        it.writeByte(INVOKESPECIAL, pos + 1)
        var newId = cp.findMember("<init>", "([C)V", "java.lang.String")
        if(newId == -1) {
          newId = cp.addMethodrefInfo(cp.getMemberClass(ind), "<init>", "([C)V")
        }
        it.write16bit(newId, pos + 2)
      }
    }
    pos
  }


}
