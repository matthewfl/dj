package edu.berkeley.dj.rt.convert

import javassist.CtClass
import javassist.bytecode.{MethodInfo, ConstPool, CodeIterator}
import javassist.convert.Transformer

import javassist.bytecode.Opcode._

import edu.berkeley.dj.rt.Config

/**
 * Created by matthewfl
 */
class Arrays (next: Transformer, val config: Config) extends Transformer(next) {

  /*
  Arrays are going to be tricky, as they are basically their own objects,
  so we could rewrite all of the arrays to use some wrapping object
  that would possibly be aware of what nodes contain what part of an array

  The system could dynamically make new classes for special typed arrays
  eg: if a class is loaded under the: edu.berkeley.dj.internal.arrayclazz.actual-type-of-array

  the system could then follow the same inhertence pattern that the object normally would
  there could possibly be problems with


  For the time being, lets just have a static class, and then call methods for each
  time that we are preforming some operation on the array

  It appears that arrays of primitives are treated different from typical arrays
  An array of objects can be casted to Object[], if it is primitive then this will fail
   */


  override def transform(clazz: CtClass, _pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    var pos = _pos
    val c : Int = it.byteAt(pos)

    if(clazz.getName.startsWith(config.internalPrefix))
      return pos

    /*if(c == NEWARRAY) {
      val atype : Int = it.byteAt(pos + 1)
      // this is two bytes, and I want to invoke static
      val staticMethod = atype match {
        // believe that this is an int
        case 10 => "edu/berkeley/dj/internal/DJArrayInt/create(I)[I"
        case _ => throw new RuntimeException()
      }
      val methodRef = cp.addUtf8Info(staticMethod)
      it.move(pos)
      it.insertGap(2)
      it.writeByte(INVOKESTATIC, pos)
      it.write16bit(methodRef, pos + 1)
    } else if(c == ARRAYLENGTH) {
      // this doesn't know the type of the array
      // this is one instruction, and we are changing it from 1 byte instruction to invoke virtual
      //val method = "edu/berkeley/dj/internal/DJArray/length()I"

      //it.insertGap(2)

    }*/


    pos
  }

}
