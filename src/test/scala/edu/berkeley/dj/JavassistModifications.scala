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

    assert(new String(b.toBytecode()).contains("java/util/List"))
    //assert(new String(b.toBytecode))
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

  test("change super name") {
    CtClass.debugDump = "/tmp"
    val cp = new ClassPool()
    cp.appendClassPath(new ClassClassPath(this.getClass))
    val base = cp.makeClass("test.somepackage.newclass")
    val baseOld = cp.makeClass("test.somepackage.oldclass")
    val c1 = cp.makeClass("test.somepackage.someclass1")
    c1.setSuperclass(baseOld)
    c1.replaceClassName("test.somepackage.oldclass", "test.somepackage.newclass")
    // we are now manually doing this setSuperclass
    c1.setSuperclass(base)
    //c1.toBytecode()
    assert(base.getName == c1.getSuperclass.getName)
  }

  test("method with array building") {
    val b = BasicClass
    val mth_code =
      """
         static void test_123() {
           new java.lang.Class[] { int.class, java.lang.Class.class };
           new int [0] ;
         }
      """
    b.addMethod(CtMethod.make(mth_code, b))
    b.toBytecode
  }


}


