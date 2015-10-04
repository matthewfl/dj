package edu.berkeley.dj.rt.convert
/*
import javassist.CtClass
import javassist.bytecode.{ConstPool, CodeIterator}
import javassist.convert.Transformer
import javassist.bytecode.Opcode._

import edu.berkeley.dj.rt.Config

/**
 * Created by matthewfl
 */
class ArraysTypeRefs(next: Transformer, val config: Config) extends Transformer(next) {

  val mregx = """(\[+)([ZCBSIJFD]|L.*?;)""".r

  def rewriteType(typ: String) = mregx.replaceAllIn(typ, mt => {
    val arr_depth = mt.group(1).length
    val typ = if(mt.group(2).length == 1) {
      mt.group(2) match {
        case "Z" => "Boolean"
        case "C" => "Char"
        case "B" => "Byte"
        case "I" => "Integer"
        case "J" => "Long"
        case "F" => "Float"
        case "D" => "Double"
        case _ => throw new NotImplementedError()
      }
    } else {
      val s = mt.group(2)
      s.substring(1, s.length - 1)//.replace("/", ".")
    }
    "L"+(config.arrayprefix + typ + "_" + arr_depth).replace(".", "/") + ";"
  })

  override def transform(clazz: CtClass, pos: Int, it: CodeIterator, cp: ConstPool): Int = {
    val c : Int = it.byteAt(pos)

    c match {
      case INVOKEDYNAMIC | INVOKEINTERFACE | INVOKESPECIAL | INVOKESTATIC | INVOKEVIRTUAL => {}

      case GETFIELD | PUTFIELD | GETSTATIC | PUTSTATIC => {
        val indx = it.u16bitAt(pos + 1)
        val ftype = cp.getFieldrefType(indx)
        if(ftype.contains("[")) {
          //it.
        }
      }

      case INSTANCEOF => {

      }
    }



  }

}

*/