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


  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    val c : Int = it.byteAt(pos)

    if(clazz.getName.startsWith(config.internalPrefix))
      return pos


    if(c == ARRAYLENGTH) {
      // this doesn't know the type of the array

    }


    pos
  }

}
