package edu.berkeley.dj.rt.convert

import javassist.CtClass
import javassist.bytecode.Opcode._
import javassist.bytecode.{CodeIterator, ConstPool, Descriptor, MethodInfo}
import javassist.convert.Transformer

import edu.berkeley.dj.rt.{Rewriter, Config}

/**
 * Created by matthewfl
 */
class FieldAccess(next : Transformer, val config: Config, val rewriter: Rewriter) extends Transformer(next) {

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

    /* Don't handle getstatic here since we replicate the static fields to all machines
     * at all times
     */

    //if(c == GETFIELD || c == GETSTATIC || c == PUTFIELD || c == PUTSTATIC) {
    if(c == PUTFIELD || c == GETFIELD || c == PUTSTATIC) {
      val index = it.u16bitAt(pos + 1)

      val fname = cp.getFieldrefName(index)
      val ftype = cp.getFieldrefType(index)
      val methodCls = cp.getFieldrefClass(index)
      val fcls = cp.getClassInfo(methodCls)
      val declcls = clazz.getClassPool.get(fcls).getField(fname, ftype).getDeclaringClass.getName

      // if there are dj fields, then don't modify their writes I guess
      if (fname.startsWith(config.fieldPrefix))
        return pos


      val isInit = minfo.getName() == "<init>"
      val isStaticInit = minfo.getName == "<clinit>"

      // we only optionally enable/disable the reads and writes of fields on an instance
      // of a class
      if(clazz.getName == fcls) {
        val mode = rewriter.classMode.getMode(fcls)
        if(!mode.rewriteFieldAccess && (c == PUTFIELD || c == GETFIELD))
          return pos
        // check the mode that this class is in
      }

      // TODO: figure out what to do with arrays



      if (c == PUTFIELD  && !ftype.startsWith("[") && !isInit) {
        // there should exist a method like ${config.fieldPrefix}write_field_${name}(${ftype})

        // putfield should already have a form of objectref, value on the stack
        // and use two bytes for the location identifier
        // invoke virtual should take the objectref and then the arguments, which should only be one
        // will possibly have to pop the returned values

        // TODO: check if this is a class that was rewritten and thus has these indirection methods

        val methodType = s"(L${Descriptor.toJvmName(declcls)};${ftype})V"
        val methodName = s"${config.fieldPrefix}write_field_${fname}"
        val methodRef = cp.addMethodrefInfo(methodCls, methodName, methodType)
        it.writeByte(INVOKESTATIC, pos)
        it.write16bit(methodRef, pos + 1)
      } else if (c == GETFIELD && !ftype.startsWith("[") && !isInit) {
        val methodType = s"(L${Descriptor.toJvmName(declcls)};)${ftype}"
        val methodName = s"${config.fieldPrefix}read_field_${fname}"
        val methodRef = cp.addMethodrefInfo(methodCls, methodName, methodType)
        it.writeByte(INVOKESTATIC, pos)
        it.write16bit(methodRef, pos + 1)

        // for the static field, we allways rewrite them
      } else if(c == PUTSTATIC && !ftype.startsWith("[") && !isStaticInit) {
        val methodType = s"(${ftype})V"
        val methodName = s"${config.fieldPrefix}write_static_field_${fname}"
        val methodRef = cp.addMethodrefInfo(methodCls, methodName, methodType)
        it.writeByte(INVOKESTATIC, pos)
        it.write16bit(methodRef, pos + 1)
      }
      // TODO: deal with static fields



    }
    pos
  }


  def isPrimitiveType(ftype: String) = {
    ftype.length == 1 && "ZCBSIJFDV".contains(ftype)
  }

}
