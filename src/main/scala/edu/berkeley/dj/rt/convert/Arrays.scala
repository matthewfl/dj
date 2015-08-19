package edu.berkeley.dj.rt.convert

import javassist.CtClass
import javassist.bytecode.Opcode._
import javassist.bytecode.analysis.{Type, Analyzer, Frame}
import javassist.bytecode.{MethodInfo, CodeIterator, ConstPool}
import javassist.convert.Transformer

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

  private var analysis: Array[Frame] = null
  private var addedSpaces: Int = 0
  private var minfo: MethodInfo = null

  override def initialize(cp: ConstPool, cls: CtClass, minfo: MethodInfo): Unit = {
    val ana = new Analyzer
    analysis = ana.analyze(cls, minfo)
    addedSpaces = 0
    this.minfo = minfo
    //println("gg")
  }

  private def placeArrayFunc(method: String, pos: Int, it: CodeIterator, cp: ConstPool) = {

  }



  override def transform(clazz: CtClass, _pos: Int, it: CodeIterator, cp: ConstPool) : Int = {
    var pos = _pos
    val c : Int = it.byteAt(pos)

    if(clazz.getName.startsWith(config.internalPrefix))
      return pos

    val frame = analysis(pos - addedSpaces)

    def makeMthod(intername: String, mthname: String, mthsig: String, count: Int) = {
      // assume that it is on some item that only has one byte
      // invoke a method on an interface that will get or set a value in an array

      val clsref = cp.addClassInfo(intername)
      val mthref = cp.addInterfaceMethodrefInfo(clsref, mthname, mthsig)
      it.writeByte(NOP, pos)
      addedSpaces += it.insertGapAt(pos, 4, false).length
      it.writeByte(INVOKEINTERFACE, pos)
      it.write16bit(mthref, pos + 1)
      it.writeByte(count, pos + 3)
    }

    def makeStaticMethod(clsname: String, mthname: String, mthsig: String) = {
      val clsref = cp.addClassInfo(clsname)
      val mthref = cp.addMethodrefInfo(clsref, mthname, mthsig)
      it.writeByte(NOP, pos)
      addedSpaces += it.insertGapAt(pos, 2, false).length
      it.writeByte(INVOKESTATIC, pos)
      it.write16bit(mthref, pos + 1)
    }

    def augName(n: String) = n.replaceAll("[^A-Za-z0-9]", "_")

    if(c == ARRAYLENGTH) {
      val clsref = cp.addClassInfo(config.arrayprefix + "Base")
      val mthref = cp.addMethodrefInfo(clsref, "length", s"(L${(config.arrayprefix + "Base").replace(".","/")};)I")
      it.writeByte(NOP, pos)
      addedSpaces += it.insertGapAt(pos, 2, false).length

      it.writeByte(INVOKESTATIC, pos)
      it.write16bit(mthref, pos + 1)
    } else if(c == NEWARRAY) {
      // this is for a primitive type
      val typ = it.byteAt(pos + 1)
      // http://cs.au.dk/~mis/dOvs/jvmspec/ref-newarray.html
      val tname = typ match {
        case 4 => "Boolean"
        case 5 => "Char"
        case 6 => "Float"
        case 7 => "Double"
        case 8 => "Byte"
        case 9 => "Short"
        case 10 => "Integer"
        case 11 => "Long"
        case _ => throw new NotImplementedError()
      }
      val clsref = cp.addClassInfo(config.arrayprefix + tname + "_impl_1")
      val mthref = cp.addMethodrefInfo(clsref, "newInstance_1", s"(I)L${(config.arrayprefix + tname + "_1").replace(".","/")};")

      it.writeByte(NOP, pos)
      it.writeByte(NOP, pos + 1)
      addedSpaces += it.insertGapAt(pos, 1, false).length
      it.writeByte(INVOKESTATIC, pos)
      it.write16bit(mthref, pos + 1)

    } else if(c == ANEWARRAY) {
      val tindx = it.u16bitAt(pos + 1)
      val typ = cp.getClassInfo(tindx)
      //assert(typ(0) == 'L')
      val tname = if(typ.endsWith(";") && typ.startsWith("L")) {
        typ.substring(1, typ.length - 1)
      } else {
        typ
      }.replace('/', '.')

      val clsref = cp.addClassInfo(config.arrayprefix + tname + "_impl_1")
      val mthref = cp.addMethodrefInfo(clsref, "newInstance_1", s"(I)L${(config.arrayprefix + tname + "_1").replace('.','/')};")

      it.writeByte(INVOKESTATIC, pos)
      it.write16bit(mthref, pos + 1)

    } else if(c == MULTIANEWARRAY) {
      // TODO:
      ???

    } else if(c == BASTORE) { // byte and boolean
      val ct = frame.getStack(frame.getTopIndex- 2)
      if(ct.getComponent == Type.BOOLEAN) {
        // this is a boolean array
        makeMthod(config.arrayprefix + "Boolean_1", "set_boolean", "(IZ)V", 3)
      } else {
        assert(ct.getComponent == Type.BYTE)
        makeMthod(config.arrayprefix + "Byte_1", "set_byte", "(IB)V", 3)
        // assume that this is a byte
      }
    } else if(c == BALOAD) {
      val ct = frame.getStack(frame.getTopIndex- 1)
      if(ct.getComponent == Type.BOOLEAN) {
        // this is a boolean array
        makeMthod(config.arrayprefix + "Boolean_1", "get_boolean", "(I)Z", 2)
      } else {
        assert(ct.getComponent == Type.BYTE)
        makeMthod(config.arrayprefix + "Byte_1", "get_byte", "(I)B", 2)
      }
    } else if(c == CALOAD) { // char
      makeMthod(config.arrayprefix + "Char_1", "get_char", "(I)C", 2)
    } else if(c == CASTORE) {
      makeMthod(config.arrayprefix + "Char_1", "set_char", "(IC)V", 3)
    } else if(c == DALOAD) { // double
      makeMthod(config.arrayprefix + "Double_1", "get_double", "(I)D", 2)
    } else if(c == DASTORE) {
      makeMthod(config.arrayprefix + "Double_1", "set_double","(ID)V", 3)
    } else if(c == FALOAD) { // float
      makeMthod(config.arrayprefix + "Float_1", "get_float", "(I)F", 2)
    } else if(c == FASTORE) {
      makeMthod(config.arrayprefix + "Float_1", "set_float", "(IF)V", 3)
    } else if(c == IALOAD) { // int
      //makeStaticMethod(config.arrayprefix + "Integer_impl_1", "helper_get", "(Ledu/berkeley/dj/internal/arrayclazz/Integer_1;I)I")
      makeMthod(config.arrayprefix + "Integer_1", "get_int", "(I)I", 2)
    } else if(c == IASTORE) {
      //makeStaticMethod(config.arrayprefix + "Integer_impl_1", "helper_set", "(Ledu/berkeley/dj/internal/arrayclazz/Integer_1;II)V")
      makeMthod(config.arrayprefix + "Integer_1", "set_int", "(II)V", 3)
    } else if(c == LALOAD) { // long
      makeMthod(config.arrayprefix + "Long_1", "get_long", "(I)J", 2)
    } else if(c == LASTORE) {
      makeMthod(config.arrayprefix + "Long_1", "set_long", "(IJ)V", 3)
    } else if(c == SALOAD) { // short
      makeMthod(config.arrayprefix + "Short_1", "get_short", "(I)S", 2)
    } else if(c == SASTORE) {
      makeMthod(config.arrayprefix + "Short_1", "set_short", "(IS)V", 3)
    } else if(c == AALOAD) { // object
      val ct = frame.getStack(frame.getTopIndex - 1)
      val comp = ct.getComponent
      val arrdim = ct.getDimensions
      val name = comp.getCtClass.getName
      makeMthod(config.arrayprefix + name + "_" + arrdim, "get_"+augName(name), s"(I)L${name.replace('.','/')};", 2)
      //???
    } else if(c == AASTORE) {
      val ct = frame.getStack(frame.getTopIndex - 2)
      val comp = ct.getComponent
      val arrdim = ct.getDimensions
      val name = comp.getCtClass.getName
      makeMthod(config.arrayprefix + name + "_" + arrdim, "set_"+augName(name), s"(IL${name.replace('.','/')};)V", 3)
      //???
    }



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
