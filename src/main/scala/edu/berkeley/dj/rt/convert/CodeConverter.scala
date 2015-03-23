package edu.berkeley.dj.rt.convert

import javassist.convert.Transformer

/**
 * Created by matthew
 */
class CodeConverter extends javassist.CodeConverter{

  def prevTransforms = transformers

  def addTransform(tran : Transformer) = {
    if(tran.getNext != prevTransforms)
      throw new RuntimeException("chain of transformers broken")

    transformers = tran
  }

}
