package edu.berkeley.dj

import javassist.{CtClass, ClassClassPath, CtMethod, ClassPool}

import org.scalatest.FunSuite


/**
 * Created by matthewfl
 */
class JavassistModifications extends FunSuite {

  def BasicClass = {
    val p = new ClassPool()
    p.appendClassPath(new ClassClassPath(this.getClass))
    p.makeClass("test.somepackage.someclass")
  }


  test("method with type signature") {
    CtClass.debugDump = "/tmp";
    val b = BasicClass
    val mth =
      """
         public ``@Ljava/util/List<Ljava/lang/String;>;`` random() {
           return null;
         }
      """

    b.addMethod(CtMethod.make(mth, b))

    b.toBytecode
  }

}
