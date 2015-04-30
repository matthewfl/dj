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
  // type signature will stay the same

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

      // java really needs to get a tuple type
      val (memberName, typeSig, className) = cp.getMemberName(ind).split("::") match {
        case Array(a,b,c) => (a,b,c)
      }
      // TODO: match other stuff, class actually one
      // as other ppl mihgt have defined methods such as wait with their own
      // type signature
      rewriteMethods get (memberName,typeSig,className) match {
        case None => {}
        case Some((newName, newClass)) => {
          // need to replace this method call with the new name
          // TODO: this needs to reference the new class & check type interfaces
          var newId = cp.findMember(newName, typeSig, newClass)
          if(newId == -1) {
            // need to add the new function call to the const pool
            // TODO: use the int index so that it does not create a new copy of the type sig
            val nref = cp.addNameAndTypeInfo(newName, typeSig)
            val clsref = cp.addClassInfo(newClass)
            if(c == INVOKEINTERFACE) {
              newId = cp.addInterfaceMethodrefInfo(clsref, nref)
            } else {
              /*if(Modifier.isPrivate(newName)) {
                it.write16bit(INVOKESPECIAL, pos)
              }*/
              newId = cp.addMethodrefInfo(clsref, nref)
            }
          }
          it.write16bit(newId, pos + 1)
          //println(newName)
        }
      }
    }
    pos
  }

}
