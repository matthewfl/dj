package edu.berkeley.dj.rt.convert

import javassist.{NotFoundException, CtClass}
import javassist.bytecode.{MethodInfo, ConstPool, CodeIterator}
import javassist.convert.{TransformReadField, Transformer}
import javassist.bytecode.Opcode._

import edu.berkeley.dj.rt.Config

/**
 * Created by matthewfl
 */
class FieldAccess(next : Transformer, val config: Config) extends Transformer(next) {

  private var minfo : MethodInfo = null
  private var enabled = true

  override def initialize(cp: ConstPool, clazz: CtClass, minfo: MethodInfo) = {
    this.minfo = minfo
    // TODO: determine if this method was declared inside .internal
    enabled = !minfo.getName.startsWith(config.fieldPrefix)
  }

  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    val c : Int = it.byteAt(pos)
    // do not rewrite the field access of the internal methods
    if(!enabled)
      return pos
    /* THINKING:
    the system should intercept all writes as that will likely already be somewhat slow
    as it has to write into ram, so adding a (hopefully) 10 ish instruction overhead
    shouldn't be that bad

    In the case of reads, there might not be a good reason to intercept primitive types as
    they could simply be copied to instances of a class
    For non primitive types, (eg objects) we don't want to have to build an entire object structure
    on each machine, so it might make since to interecept these values
    doing something like a null check and then if it is null check if it should
    intercept the read and attempt to load an external value
    the advantage of this would be that the jvm should already be looking for null check
    type operations and attempting to optimize these away
     */

    //if(c == GETFIELD || c == GETSTATIC || c == PUTFIELD || c == PUTSTATIC) {
    if(c == PUTFIELD || c == GETFIELD) {
      val index = it.u16bitAt(pos + 1)

      val fname = cp.getFieldrefName(index)
      val ftype = cp.getFieldrefType(index)

      // if there are dj fields, then don't modify their writes I guess
      if (fname.startsWith(config.fieldPrefix))
        return pos

      if (c == PUTFIELD && !ftype.startsWith("[")) {
        // there should exist a method like ${config.fieldPrefix}write_field_${name}(${ftype})

        // putfield should already have a form of objectref, value on the stack
        // and use two bytes for the location identifier
        // invoke virtual should take the objectref and then the arguments, which should only be one
        // will possibly have to pop the returned values


        // TODO: make this look this up in the cp rather then just add it every time
        val methodType = s"(${ftype})V"
        val methodCls = cp.getFieldrefClass(index)
        val methodName = s"${config.fieldPrefix}write_field_${fname}"
        val methodRef = cp.addMethodrefInfo(methodCls, methodName, methodType)
        it.writeByte(INVOKEVIRTUAL, pos)
        it.write16bit(methodRef, pos + 1)
      } else if (c == GETFIELD) {
        // if this field is not a primitive type, then
        // replace the read access
        /*if(!isPrimitiveType(ftype)) {
          val methodType = s"()${ftype}"
          val methodCls = cp.getFieldrefClass(index)
          val methodName = s"${config.fieldPrefix}read_field_${fname}"
          val methodRef = cp.addMethodrefInfo(methodCls, methodName, methodType)
          it.writeByte(INVOKEVIRTUAL, pos)
          it.write16bit(methodRef, pos + 1)
        }*/
      }
      // TODO: deal with static fields



    }
    pos
  }


  def isPrimitiveType(ftype: String) = {
    ftype.length == 1 && "ZCBSIJFDV".contains(ftype)
  }

}
