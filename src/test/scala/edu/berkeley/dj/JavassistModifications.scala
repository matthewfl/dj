package edu.berkeley.dj

import javassist._

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

    b.toBytecode()
    //assert(b.toBytecode)
  }


  test("access method on super") {
    CtClass.debugDump = "/tmp"
    val cp = new ClassPool()
    cp.appendClassPath(new ClassClassPath(this.getClass))
    val c1 = cp.makeClass("test.somepackage.someclass1")
    c1.addField(CtField.make("int __test;", c1))
    c1.addField(CtField.make("java.util.List l1;", c1))
    val c2 = cp.makeClass("test.somepackage.someclass2")
    c2.setSuperclass(c1)
    c2.addMethod(CtMethod.make(
      """public void gg (java.util.List vv) {
        if((__test & 0x02) == 0) {
          System.out.println("something");
        } else {
         this.l1 = vv;
        }
      }""", c2))
    val loader = new Loader(null, cp)
    loader.loadClass("test.somepackage.someclass1")
    val inst = loader.loadClass("test.somepackage.someclass2")
    for(m <- inst.getMethods)
      println(m)
  }

}
