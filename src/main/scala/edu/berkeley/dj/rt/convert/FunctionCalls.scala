package edu.berkeley.dj.rt.convert


import javassist.{Modifier, CtMethod, CtClass}
import javassist.bytecode.{MethodInfo, ConstPool, CodeIterator}
import javassist.convert.Transformer
import javassist.bytecode.Opcode._

/**
 * Created by matthewfl
 */
class FunctionCalls (next : Transformer, val rewriteMethods: Map[(String,String,String),(String,String)]) extends Transformer(next) {

  // rewriteMethods:
  // (method_name, type_signature, class_name), (new_method_name, new_class_name)
  // type signature will stay the same or add a argument for the this pointer

  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    val c : Int = it.byteAt(pos)
    if(c == INVOKEINTERFACE || c == INVOKESPECIAL || c == INVOKESTATIC || c == INVOKEVIRTUAL) {
      val ind = it.u16bitAt(pos + 1)

      // java really needs to get a tuple type
      val (memberName, typeSig, className) = cp.getMemberName(ind).split("::") match {
        case Array(a,b,c) => (a,b,c)
      }
      rewriteMethods get (memberName,typeSig,className) match {
        case None => {}
        case Some((newName, newClass)) => {
          // need to replace this method call with the new name
          val ntypeSig = if(c != INVOKESTATIC) {
            // We are converting a non static call to a static call
            // so the first argument is going to end up being the this
            typeSig.replace("(", "(Ljava/lang/Object;")
          } else typeSig
          var newId = cp.findMember(newName, ntypeSig, newClass)
          if(newId == -1) {
            // need to add the new function call to the const pool
            val nref = cp.addNameAndTypeInfo(newName, ntypeSig)
            val clsref = cp.addClassInfo(newClass)
            newId = cp.addMethodrefInfo(clsref, nref)
          }
          it.writeByte(INVOKESTATIC, pos)
          it.write16bit(newId, pos + 1)
        }
      }
    }
    pos
  }

}
